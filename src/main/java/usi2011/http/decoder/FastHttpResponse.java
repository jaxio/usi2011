package usi2011.http.decoder;

import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;

/**
 * Uses all the work dones in {@link FastHttpRequest} instead of relying in way too lenient code from netty
 * @see DefaultHttpResponse 
 */
public class FastHttpResponse extends FastHttpRequest implements HttpResponse {
    private HttpResponseStatus status;

    public FastHttpResponse(HttpVersion httpVersion, HttpResponseStatus status) {
        super(httpVersion);
        this.status = status;
    }

    @Override
    public HttpResponseStatus getStatus() {
        return status;
    }

    @Override
    public void setStatus(HttpResponseStatus status) {
        if (status == null) {
            throw new NullPointerException("status");
        }
        this.status = status;
    }
}