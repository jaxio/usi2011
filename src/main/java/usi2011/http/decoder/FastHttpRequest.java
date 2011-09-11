package usi2011.http.decoder;

import static com.google.common.collect.Lists.newArrayList;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.COOKIE;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.TRANSFER_ENCODING;
import static org.slf4j.LoggerFactory.getLogger;
import static usi2011.util.LogUtil.isDebugEnabled;

import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.http.DefaultHttpMessage;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpHeaders.Names;
import org.jboss.netty.handler.codec.http.HttpHeaders.Values;
import org.jboss.netty.handler.codec.http.HttpMessage;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.slf4j.Logger;

import usi2011.util.FastIntegerParser;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * handle only the headers we need that is
 * <ul>
 * <li>HttpHeaders.Names.CONNECTION</li>
 * <li>HttpHeaders.Names.CONTENT_LENGTH</li>
 * <li>HttpHeaders.Names.COOKIE</li>
 * </ul>
 * Use a multi map instead of the slow {@link HttpHeaders} that checks for validity etc
 * 
 * @see DefaultHttpRequest
 * @see DefaultHttpMessage
 */
public class FastHttpRequest implements HttpMessage, HttpRequest {
    private static final Logger logger = getLogger(FastHttpRequest.class);
    private static final String[] INCOMING_REQUEST_HEADERS_ALLOWED = { COOKIE, CONNECTION, CONTENT_LENGTH, TRANSFER_ENCODING};
    private static final int MAX_INCOMING_REQUEST_HEADERS_ALLOWED = INCOMING_REQUEST_HEADERS_ALLOWED.length;

    private static final int MOST_DISCRIMINANT_CHAR_POSITION = 5;
    private static final char COOKIE_MOST_DISCRIMINANT_CHAR = COOKIE.charAt(MOST_DISCRIMINANT_CHAR_POSITION);
    private static final char CONNECTION_MOST_DISCRIMINANT_CHAR = CONNECTION.charAt(MOST_DISCRIMINANT_CHAR_POSITION);
    private static final char CONTENT_LENGTH_MOST_DISCRIMINANT_CHAR = CONTENT_LENGTH.charAt(MOST_DISCRIMINANT_CHAR_POSITION);
    private static final char TRANSFER_ENCODING_MOST_DISCRIMINANT_CHAR = TRANSFER_ENCODING.charAt(MOST_DISCRIMINANT_CHAR_POSITION);
    private static final char CONTENT_TYPE_MOST_DISCRIMINANT_CHAR = CONTENT_TYPE.charAt(MOST_DISCRIMINANT_CHAR_POSITION);
    private static final long CONTENT_LENGTH_UNITIALIZED = -1l;
    private static final long IS_KEEP_ALIVE_UNITIALIZED = -1l;
    private static final long IS_KEEP_ALIVE_TRUE = 1l;
    private static final long IS_KEEP_ALIVE_FALSE = 0l;

    private final Multimap<String, String> headers = HashMultimap.create();
    private HttpMethod method;
    private String uri;
    private HttpVersion version;
    private boolean chunked;
    private ChannelBuffer content = ChannelBuffers.EMPTY_BUFFER;
    private long contentLength = CONTENT_LENGTH_UNITIALIZED;
    private long isKeepAlive = IS_KEEP_ALIVE_UNITIALIZED;

    public FastHttpRequest(HttpVersion httpVersion, HttpMethod method, String uri) {
        this.version = httpVersion;
        this.method = method;
        this.uri = uri;
    }

    /**
     * Used when building a http response message
     */
    public FastHttpRequest(HttpVersion httpVersion) {
        this.version = httpVersion;
        this.method = null;
        this.uri = null;
    }

    public boolean hasAllIncomingRequestHeader() {
        return getHeaderNames().size() == MAX_INCOMING_REQUEST_HEADERS_ALLOWED;
    }

    public boolean acceptIncomingRequestHeader(String headerName) {
        try {
            if (hasAllIncomingRequestHeader()) {
                return false;
            }
            // fail fast comparison using the most discriminant char
            final char mostDiscriminantChar = headerName.charAt(MOST_DISCRIMINANT_CHAR_POSITION);
            if (mostDiscriminantChar != COOKIE_MOST_DISCRIMINANT_CHAR //
                    && mostDiscriminantChar != CONNECTION_MOST_DISCRIMINANT_CHAR //
                    && mostDiscriminantChar != CONTENT_LENGTH_MOST_DISCRIMINANT_CHAR //
                    && mostDiscriminantChar != TRANSFER_ENCODING_MOST_DISCRIMINANT_CHAR //
                    && mostDiscriminantChar != CONTENT_TYPE_MOST_DISCRIMINANT_CHAR) {
                return false;
            }
            return COOKIE.equals(headerName) //
                    || CONNECTION.equals(headerName) //
                    || CONTENT_LENGTH.equals(headerName) //
                    || TRANSFER_ENCODING.equals(headerName);
        } catch (Exception ignore) {
            // possible if header name length is less that is MOST_DISCRIMINANT_CHAR_POSITION
            return false;
        }
    }

    /**
     * Main difference is we support only String in headers
     */
    @Override
    public void addHeader(final String name, final Object value) {
        if (isDebugEnabled) {
            if (!(value instanceof String)) {
                logger.warn("You can add only String values to header, you set a {} for {}", value.getClass().getSimpleName(), name);
            }
        }
        headers.put(name, (String) value);
    }

    @Override
    public void setHeader(final String name, final Object value) {
        addHeader(name, value);
    }

    @Override
    public void setHeader(final String name, final Iterable<?> values) {
        for (Object value : values) {
            addHeader(name, value);
        }
    }

    @Override
    public void removeHeader(final String name) {
        headers.removeAll(name);
    }

    @Override
    public String getHeader(String name) {
        final List<String> values = getHeaders(name);
        return !values.isEmpty() ? values.get(0) : null;
    }

    @Override
    public List<String> getHeaders(String name) {
        return newArrayList(headers.get(name));
    }

    @Override
    public List<Entry<String, String>> getHeaders() {
        return newArrayList(headers.entries());
    }

    @Override
    public boolean containsHeader(String name) {
        return headers.containsKey(name);
    }

    @Override
    public Set<String> getHeaderNames() {
        return headers.keySet();
    }

    @Override
    public HttpMethod getMethod() {
        return method;
    }

    @Override
    public void setMethod(HttpMethod method) {
        this.method = method;
    }

    @Override
    public String getUri() {
        return uri;
    }

    @Override
    public void setUri(String uri) {
        if (uri == null) {
            throw new NullPointerException("uri");
        }
        this.uri = uri;
    }

    @Override
    public HttpVersion getProtocolVersion() {
        return version;
    }

    @Override
    public void setProtocolVersion(HttpVersion version) {
        if (version == null) {
            throw new NullPointerException("version");
        }
        this.version = version;
    }

    @Override
    public ChannelBuffer getContent() {
        return content;
    }

    @Override
    public void setContent(ChannelBuffer content) {
        if (content == null) {
            content = ChannelBuffers.EMPTY_BUFFER;
        }
        if (content.readable() && isChunked()) {
            throw new IllegalArgumentException("non-empty content disallowed if this.chunked == true");
        }
        this.content = content;
    }

    @Override
    public void clearHeaders() {
        headers.clear();
    }

    @Override
    public long getContentLength() {
        return getContentLength(0l);
    }

    @Override
    public long getContentLength(long defaultValue) {
        if (contentLength != CONTENT_LENGTH_UNITIALIZED) {
            return contentLength;
        }
        final String contentLengthValue = getHeader(Names.CONTENT_LENGTH);
        if (contentLengthValue != null) {
            contentLength = FastIntegerParser.parseLong(contentLengthValue);
        }
        contentLength = defaultValue;
        return contentLength;
    }

    @Override
    public boolean isChunked() {
        if (chunked) {
            return true;
        } else {
            return isTransferEncodingChunked(this);
        }
    }

    /**
     * Copied from org.jboss.netty.handler.codec.http.HttpCodecUtil
     */
    static boolean isTransferEncodingChunked(HttpMessage m) {
        List<String> chunked = m.getHeaders(HttpHeaders.Names.TRANSFER_ENCODING);
        if (chunked.isEmpty()) {
            return false;
        }

        for (String v : chunked) {
            if (v.equalsIgnoreCase(HttpHeaders.Values.CHUNKED)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void setChunked(boolean chunked) {
        this.chunked = chunked;
        if (chunked) {
            setContent(ChannelBuffers.EMPTY_BUFFER);
        }
    }

    @Override
    public boolean isKeepAlive() {
        if (IS_KEEP_ALIVE_UNITIALIZED != isKeepAlive) {
            return isKeepAlive == IS_KEEP_ALIVE_TRUE;
        }
        final String connection = getHeader(Names.CONNECTION);
        if (Values.CLOSE.equalsIgnoreCase(connection)) {
            isKeepAlive = IS_KEEP_ALIVE_FALSE;
            return false;
        }

        if (getProtocolVersion().isKeepAliveDefault()) {
            if (!Values.CLOSE.equalsIgnoreCase(connection)) {
                isKeepAlive = IS_KEEP_ALIVE_TRUE;
                return true;
            } else {
                isKeepAlive = IS_KEEP_ALIVE_FALSE;
                return false;
            }
        } else {
            if (Values.KEEP_ALIVE.equalsIgnoreCase(connection)) {
                isKeepAlive = IS_KEEP_ALIVE_TRUE;
                return true;
            } else {
                isKeepAlive = IS_KEEP_ALIVE_FALSE;
                return false;
            }
        }
    }
}
