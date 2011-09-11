package usi2011.http.decoder;

import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.SET_COOKIE;

import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;

/**
 * Uses all the work dones in {@link FastHttpRequest} instead of relying in way too lenient code from netty
 * 
 * @see DefaultHttpResponse
 */
public class FasterHttpResponse extends FasterHttpRequest implements HttpResponse {
    private HttpResponseStatus status;

    private String contentAsString;

    public FasterHttpResponse(HttpVersion httpVersion, HttpResponseStatus status) {
        super(httpVersion);
        this.status = status;
    }

    @Override
    public HttpResponseStatus getStatus() {
        return status;
    }

    @Override
    public void setStatus(HttpResponseStatus status) {
        this.status = status;
    }

    public String headersToWrite() {
        StringBuilder ret = new StringBuilder(128);
        if (contentLength != null) {
            ret.append(CONTENT_LENGTH).append(": ").append(contentLength).append("\r\n");
        }
        if (setCookie != null) {
            ret.append(SET_COOKIE).append(": ").append(setCookie).append("\r\n");
        }
        if (connection != null) {
            ret.append(CONNECTION).append(": ").append(connection).append("\r\n");
        }
        if (contentType != null) {
            ret.append(CONTENT_TYPE).append(": ").append(contentType).append("\r\n");
        }
        return ret.toString();
    }

    public String getContentAsString() {
        return contentAsString;
    }

    public void setContentAsString(String contentAsString) {
        this.contentAsString = contentAsString;
    }
}