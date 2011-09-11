package usi2011.http.encoder;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.HttpMessage;
import org.jboss.netty.handler.codec.http.HttpMessageEncoder;

import usi2011.http.decoder.FasterHttpResponse;

public final class FasterHttpResponseEncoder extends HttpMessageEncoder {

    /**
     * Creates a new instance.
     */
    public FasterHttpResponseEncoder() {
        super();
    }

    @Override
    protected Object encode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
        if (msg instanceof FasterHttpResponse) {
            return encode(ctx, channel, (FasterHttpResponse) msg);
        } else {
            return super.encode(ctx, channel, msg);
        }
    }

    // to prevent convertion as we use only ASCII
    @SuppressWarnings("deprecation")
    private static byte[] noConvertionToBytes(StringBuilder sb) {
        final int length = sb.length();
        byte[] ret = new byte[length];
        sb.toString().getBytes(0, length, ret, 0);
        return ret;
    }

    protected Object encode(ChannelHandlerContext ctx, Channel channel, FasterHttpResponse response) throws Exception {
        StringBuilder toWrite = new StringBuilder(256);
        toWrite.append(initialLine(response));
        toWrite.append(response.headersToWrite());
        toWrite.append("\r\n");
        if (response.getContentAsString() != null) {
            toWrite.append(response.getContentAsString());
            return ChannelBuffers.wrappedBuffer(noConvertionToBytes(toWrite));
        } else {
            return ChannelBuffers.wrappedBuffer(noConvertionToBytes(toWrite), response.getContent().array());
        }
    }

    private Object initialLine(FasterHttpResponse response) {
        switch (response.getStatus().getCode()) {
        case 200:
            return HttpResponseEncoderUtil.OK;
        case 201:
            return HttpResponseEncoderUtil.CREATED;
        default:
        }
        return HttpResponseEncoderUtil.HTTP_1_1 + " " //
                + String.valueOf(response.getStatus().getCode()) + " " //
                + String.valueOf(response.getStatus().getReasonPhrase()) //
                + "\r\n";
    }

    @Override
    protected void encodeInitialLine(ChannelBuffer buf, HttpMessage message) throws Exception {
        throw new IllegalStateException("Should not be called");
    }
}
