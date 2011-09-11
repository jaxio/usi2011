package usi2011.http.decoder;

import java.util.List;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.TooLongFrameException;
import org.jboss.netty.handler.codec.http.DefaultHttpChunk;
import org.jboss.netty.handler.codec.http.DefaultHttpChunkTrailer;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpChunkTrailer;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMessage;
import org.jboss.netty.handler.codec.http.HttpMessageDecoder;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.replay.ReplayingDecoder;

import usi2011.util.FastIntegerParser;
import usi2011.util.SplitUtil;

/**
 * Most of the code is copied from {@link HttpMessageDecoder} to set our own http request and handle the headers we need. We are way less lenient that netty
 * code.
 * <p>
 * The indication in the code is present to show where we changed stuff. It is mostly in {@link #readHeaders(ChannelBuffer)}
 * 
 * @see HttpRequestDecoder
 * @see HttpMessageDecoder
 */
public class FasterHttpRequestDecoder extends ReplayingDecoder<FasterHttpRequestDecoder.State> {
    private final int maxInitialLineLength;
    private final int maxHeaderSize;
    private final int maxChunkSize;
    private HttpMessage message;
    private ChannelBuffer content;
    private long chunkSize;
    private int headerSize;

    /**
     * The internal state of {@link HttpMessageDecoder}. <em>Internal use only</em>.
     * 
     * @author <a href="http://www.jboss.org/netty/">The Netty Project</a>
     * @author <a href="http://gleamynode.net/">Trustin Lee</a>
     * @version $Rev: 2370 $, $Date: 2010-10-19 14:40:44 +0900 (Tue, 19 Oct 2010) $
     * 
     * @apiviz.exclude
     */
    protected static enum State {
        SKIP_CONTROL_CHARS,
        READ_INITIAL,
        READ_HEADER,
        READ_VARIABLE_LENGTH_CONTENT,
        READ_VARIABLE_LENGTH_CONTENT_AS_CHUNKS,
        READ_FIXED_LENGTH_CONTENT,
        READ_FIXED_LENGTH_CONTENT_AS_CHUNKS,
        READ_CHUNK_SIZE,
        READ_CHUNKED_CONTENT,
        READ_CHUNKED_CONTENT_AS_CHUNKS,
        READ_CHUNK_DELIMITER,
        READ_CHUNK_FOOTER;
    }

    /**
     * Creates a new instance with the specified parameters.
     */
    public FasterHttpRequestDecoder(int maxInitialLineLength, int maxHeaderSize, int maxChunkSize) {
        super(State.SKIP_CONTROL_CHARS, true);
        this.maxInitialLineLength = maxInitialLineLength;
        this.maxHeaderSize = maxHeaderSize;
        this.maxChunkSize = maxChunkSize;
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer, State state) throws Exception {
        switch (state) {
        case SKIP_CONTROL_CHARS: {
            try {
                skipControlCharacters(buffer);
                checkpoint(State.READ_INITIAL);
            } finally {
                checkpoint();
            }
        }
        case READ_INITIAL: {
            final String[] initialLine = splitInitialLine(readLine(buffer, maxInitialLineLength));
            if (initialLine.length < 3) {
                // Invalid initial line - ignore.
                checkpoint(State.SKIP_CONTROL_CHARS);
                return null;
            }
            message = createMessage(initialLine);
            checkpoint(State.READ_HEADER);
        }
        case READ_HEADER: {
            State nextState = readHeaders(buffer);
            checkpoint(nextState);
            if (nextState == State.SKIP_CONTROL_CHARS) {
                return message;
            } else {
                long contentLength = getContentLength(message, -1);
                if (contentLength == 0 || contentLength == -1 && isDecodingRequest()) {
                    content = ChannelBuffers.EMPTY_BUFFER;
                    return reset();
                }

                switch (nextState) {
                case READ_FIXED_LENGTH_CONTENT:
                    if (contentLength > maxChunkSize || HttpHeaders.is100ContinueExpected(message)) {
                        // Generate HttpMessage first. HttpChunks will follow.
                        checkpoint(State.READ_FIXED_LENGTH_CONTENT_AS_CHUNKS);
                        message.setChunked(true);
                        // chunkSize will be decreased as the READ_FIXED_LENGTH_CONTENT_AS_CHUNKS
                        // state reads data chunk by chunk.
                        chunkSize = getContentLength(message, -1);
                        return message;
                    }
                    break;
                case READ_VARIABLE_LENGTH_CONTENT:
                    if (buffer.readableBytes() > maxChunkSize || HttpHeaders.is100ContinueExpected(message)) {
                        // Generate HttpMessage first. HttpChunks will follow.
                        checkpoint(State.READ_VARIABLE_LENGTH_CONTENT_AS_CHUNKS);
                        message.setChunked(true);
                        return message;
                    }
                    break;
                default:
                    throw new IllegalStateException("Unexpected state: " + nextState);
                }
            }
            // We return null here, this forces decode to be called again where we will decode the content
            return null;
        }
        case READ_VARIABLE_LENGTH_CONTENT: {
            if (content == null) {
                content = ChannelBuffers.dynamicBuffer(channel.getConfig().getBufferFactory());
            }
            // this will cause a replay error until the channel is closed where this will read what's left in the buffer
            content.writeBytes(buffer.readBytes(buffer.readableBytes()));
            return reset();
        }
        case READ_VARIABLE_LENGTH_CONTENT_AS_CHUNKS: {
            // Keep reading data as a chunk until the end of connection is reached.
            int chunkSize = Math.min(maxChunkSize, buffer.readableBytes());
            HttpChunk chunk = new DefaultHttpChunk(buffer.readBytes(chunkSize));

            if (!buffer.readable()) {
                // Reached to the end of the connection.
                reset();
                if (!chunk.isLast()) {
                    // Append the last chunk.
                    return new Object[] { chunk, HttpChunk.LAST_CHUNK };
                }
            }
            return chunk;
        }
        case READ_FIXED_LENGTH_CONTENT: {
            // we have a content-length so we just read the correct number of bytes
            readFixedLengthContent(buffer);
            return reset();
        }
        case READ_FIXED_LENGTH_CONTENT_AS_CHUNKS: {
            long chunkSize = this.chunkSize;
            HttpChunk chunk;
            if (chunkSize > maxChunkSize) {
                chunk = new DefaultHttpChunk(buffer.readBytes(maxChunkSize));
                chunkSize -= maxChunkSize;
            } else {
                assert chunkSize <= Integer.MAX_VALUE;
                chunk = new DefaultHttpChunk(buffer.readBytes((int) chunkSize));
                chunkSize = 0;
            }
            this.chunkSize = chunkSize;

            if (chunkSize == 0) {
                // Read all content.
                reset();
                if (!chunk.isLast()) {
                    // Append the last chunk.
                    return new Object[] { chunk, HttpChunk.LAST_CHUNK };
                }
            }
            return chunk;
        }
            /**
             * everything else after this point takes care of reading chunked content. basically, read chunk size, read chunk, read and ignore the CRLF and
             * repeat until 0
             */
        case READ_CHUNK_SIZE: {
            String line = readLine(buffer, maxInitialLineLength);
            int chunkSize = getChunkSize(line);
            this.chunkSize = chunkSize;
            if (chunkSize == 0) {
                checkpoint(State.READ_CHUNK_FOOTER);
                return null;
            } else if (chunkSize > maxChunkSize) {
                // A chunk is too large. Split them into multiple chunks again.
                checkpoint(State.READ_CHUNKED_CONTENT_AS_CHUNKS);
            } else {
                checkpoint(State.READ_CHUNKED_CONTENT);
            }
        }
        case READ_CHUNKED_CONTENT: {
            assert chunkSize <= Integer.MAX_VALUE;
            HttpChunk chunk = new DefaultHttpChunk(buffer.readBytes((int) chunkSize));
            checkpoint(State.READ_CHUNK_DELIMITER);
            return chunk;
        }
        case READ_CHUNKED_CONTENT_AS_CHUNKS: {
            long chunkSize = this.chunkSize;
            HttpChunk chunk;
            if (chunkSize > maxChunkSize) {
                chunk = new DefaultHttpChunk(buffer.readBytes(maxChunkSize));
                chunkSize -= maxChunkSize;
            } else {
                assert chunkSize <= Integer.MAX_VALUE;
                chunk = new DefaultHttpChunk(buffer.readBytes((int) chunkSize));
                chunkSize = 0;
            }
            this.chunkSize = chunkSize;

            if (chunkSize == 0) {
                // Read all content.
                checkpoint(State.READ_CHUNK_DELIMITER);
            }

            if (!chunk.isLast()) {
                return chunk;
            }
        }
        case READ_CHUNK_DELIMITER: {
            for (;;) {
                byte next = buffer.readByte();
                if (next == HttpCodecUtil.CR) {
                    if (buffer.readByte() == HttpCodecUtil.LF) {
                        checkpoint(State.READ_CHUNK_SIZE);
                        return null;
                    }
                } else if (next == HttpCodecUtil.LF) {
                    checkpoint(State.READ_CHUNK_SIZE);
                    return null;
                }
            }
        }
        case READ_CHUNK_FOOTER: {
            HttpChunkTrailer trailer = readTrailingHeaders(buffer);
            if (maxChunkSize == 0) {
                // Chunked encoding disabled.
                return reset();
            } else {
                reset();
                // The last chunk, which is empty
                return trailer;
            }
        }
        default: {
            throw new Error("Shouldn't reach here.");
        }

        }
    }

    protected boolean isContentAlwaysEmpty(HttpMessage msg) {
        if (msg instanceof HttpResponse) {
            HttpResponse res = (HttpResponse) msg;
            int code = res.getStatus().getCode();
            if (code < 200) {
                return true;
            }
            switch (code) {
            case 204:
            case 205:
            case 304:
                return true;
            }
        }
        return false;
    }

    private Object reset() {
        HttpMessage message = this.message;
        ChannelBuffer content = this.content;

        if (content != null) {
            message.setContent(content);
            this.content = null;
        }
        this.message = null;

        checkpoint(State.SKIP_CONTROL_CHARS);
        return message;
    }

    private void skipControlCharacters(ChannelBuffer buffer) {
        for (;;) {
            char c = (char) buffer.readUnsignedByte();
            if (!Character.isISOControl(c) && !Character.isWhitespace(c)) {
                buffer.readerIndex(buffer.readerIndex() - 1);
                break;
            }
        }
    }

    private void readFixedLengthContent(ChannelBuffer buffer) {
        long length = getContentLength(message);
        assert length <= Integer.MAX_VALUE;

        if (content == null) {
            content = buffer.readBytes((int) length);
        } else {
            content.writeBytes(buffer.readBytes((int) length));
        }
    }

    private State readHeaders(ChannelBuffer buffer) throws TooLongFrameException {
        headerSize = 0;
        String line = readHeader(buffer);
        int match = 0;
        String name = null;
        String value = null;
        boolean nameToInsert = false;
        boolean hasContentLength = false;
        if (line.length() != 0) {
            do {
                char firstChar = line.charAt(0);
                if (name != null && (firstChar == ' ' || firstChar == '\t')) {
                    value = value + ' ' + line.trim();
                } else {
                    if (name != null && nameToInsert) {
                        match ++;
                        message.addHeader(name, value);
                    }
                    if (match < 3 && firstChar == 'C') {
                        if (line.startsWith("Connection: ")) {
                            name = "Connection";
                            value = line.substring(12);
                            nameToInsert = true;
                        } else if (line.startsWith("Cookie: ")) {
                            name = "Cookie";
                            value = line.substring(8);
                            nameToInsert = true;
                        } else if (line.startsWith("Content-Length: ")) {
                            name = "Content-Length";
                            value = line.substring(16);
                            nameToInsert = true;
                            hasContentLength = true;
                        } else {
                            nameToInsert = false;
                        }
                    }
                }
                
                line = readHeader(buffer);
            } while (line.length() != 0);
            
            // Add the last header.
            if (name != null && nameToInsert) {
                match ++;
                message.addHeader(name, value);
            }
        }

        if (hasContentLength) {
            return State.READ_FIXED_LENGTH_CONTENT;
        } else {
            return State.READ_VARIABLE_LENGTH_CONTENT;
        }
    }

    private HttpChunkTrailer readTrailingHeaders(ChannelBuffer buffer) throws TooLongFrameException {
        headerSize = 0;
        String line = readHeader(buffer);
        String lastHeader = null;
        if (line.length() != 0) {
            HttpChunkTrailer trailer = new DefaultHttpChunkTrailer();
            do {
                char firstChar = line.charAt(0);
                if (lastHeader != null && (firstChar == ' ' || firstChar == '\t')) {
                    List<String> current = trailer.getHeaders(lastHeader);
                    if (current.size() != 0) {
                        int lastPos = current.size() - 1;
                        String newString = current.get(lastPos) + line.trim();
                        current.set(lastPos, newString);
                    } else {
                        // Content-Length, Transfer-Encoding, or Trailer
                    }
                } else {
                    String[] header = splitHeader(line);
                    String name = header[0];
                    if (!name.equalsIgnoreCase(HttpHeaders.Names.CONTENT_LENGTH) //
                            && !name.equalsIgnoreCase(HttpHeaders.Names.TRANSFER_ENCODING) //
                            && !name.equalsIgnoreCase(HttpHeaders.Names.TRAILER)) {
                        trailer.addHeader(name, header[1]);
                    }
                    lastHeader = name;
                }

                line = readHeader(buffer);
            } while (line.length() != 0);

            return trailer;
        }

        return HttpChunk.LAST_CHUNK;
    }

    private String readHeader(ChannelBuffer buffer) throws TooLongFrameException {
        StringBuilder sb = new StringBuilder(64);
        int headerSize = this.headerSize;

        loop: for (;;) {
            char nextByte = (char) buffer.readByte();
            headerSize++;

            switch (nextByte) {
            case HttpCodecUtil.CR:
                nextByte = (char) buffer.readByte();
                headerSize++;
                if (nextByte == HttpCodecUtil.LF) {
                    break loop;
                }
                break;
            case HttpCodecUtil.LF:
                break loop;
            }

            // Abort decoding if the header part is too large.
            if (headerSize >= maxHeaderSize) {
                // TODO: Respond with Bad Request and discard the traffic
                // or close the connection.
                // No need to notify the upstream handlers - just log.
                // If decoding a response, just throw an exception.
                throw new TooLongFrameException("HTTP header is larger than " + maxHeaderSize + " bytes.");

            }

            sb.append(nextByte);
        }

        this.headerSize = headerSize;
        return sb.toString();
    }

    private int getChunkSize(String hex) {
        hex = hex.trim();
        for (int i = 0; i < hex.length(); i++) {
            char c = hex.charAt(i);
            if (c == ';' || Character.isWhitespace(c) || Character.isISOControl(c)) {
                hex = hex.substring(0, i);
                break;
            }
        }

        return Integer.parseInt(hex, 16);
    }

    private String readLine(ChannelBuffer buffer, int maxLineLength) throws TooLongFrameException {
        StringBuilder sb = new StringBuilder(64);
        int lineLength = 0;
        while (true) {
            byte nextByte = buffer.readByte();
            if (nextByte == HttpCodecUtil.CR) {
                nextByte = buffer.readByte();
                if (nextByte == HttpCodecUtil.LF) {
                    return sb.toString();
                }
            } else if (nextByte == HttpCodecUtil.LF) {
                return sb.toString();
            } else {
                if (lineLength >= maxLineLength) {
                    // TODO: Respond with Bad Request and discard the traffic
                    // or close the connection.
                    // No need to notify the upstream handlers - just log.
                    // If decoding a response, just throw an exception.
                    throw new TooLongFrameException("An HTTP line is larger than " + maxLineLength + " bytes.");
                }
                lineLength++;
                sb.append((char) nextByte);
            }
        }
    }

    // jaxio new methods

    private HttpMessage createMessage(String[] initialLine) throws Exception {
        return new FasterHttpRequest(initialLine[2], initialLine[0], initialLine[1]);
    }

    private boolean isDecodingRequest() {
        return true;
    }

    /**
     * we do not support requests that does not conform to http specs
     * 
     * <pre>
     * GET      /uri with space     \t HTTP1/1
     * </pre>
     */
    private String[] splitInitialLine(String sb) {
        return SplitUtil.split(sb, ' ', 3);
    }

    /**
     */
    private String[] splitHeader(String sb) {
        final int length = sb.length();
        for (int i = 0; i < length; i++) {
            if (sb.charAt(i) == ':' && (i + 2) <= length && sb.charAt(i + 1) == ' ') {
                return new String[] {sb.substring(0, i), sb.substring(i + 2, length)};
            }
        }
        return new String[] {sb, ""};
    }
    
    private long getContentLength(HttpMessage message) {
        return FastIntegerParser.parseInt(message.getHeader("Content-Length"));
    }
    private long getContentLength(HttpMessage message, int defaultValue) {
        String value = message.getHeader("Content-Length");
        if (FastIntegerParser.isNumber(value)) {
            return FastIntegerParser.parseLong(value);
        }
        return defaultValue;
    }
}