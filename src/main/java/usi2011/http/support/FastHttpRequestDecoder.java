package usi2011.http.support;

import org.jboss.netty.handler.codec.http.HttpMessage;
import org.jboss.netty.handler.codec.http.HttpMessageDecoder;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpVersion;

/**
 * Copied from {@link HttpMessageDecoder} to set our own http request and handle only the cookie header
 */
public class FastHttpRequestDecoder extends HttpMessageDecoder {
    public FastHttpRequestDecoder(int maxInitialLineLength, int maxHeaderSize, int maxChunkSize) {
        super(maxInitialLineLength, maxHeaderSize, maxChunkSize);
    }

    @Override
    protected HttpMessage createMessage(String[] initialLine) throws Exception {
        return new FastHttpRequest(HttpVersion.valueOf(initialLine[2]), HttpMethod.valueOf(initialLine[0]), initialLine[1]);
    }

    @Override
    protected boolean isDecodingRequest() {
        return true;
    }
}