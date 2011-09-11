package usi2011.http.support;

import static org.apache.commons.lang.exception.ExceptionUtils.getStackTrace;
import static org.jboss.netty.buffer.ChannelBuffers.copiedBuffer;
import static org.jboss.netty.channel.ChannelFutureListener.CLOSE;
import static org.jboss.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.SET_COOKIE;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static org.jboss.netty.util.CharsetUtil.UTF_8;
import static org.slf4j.LoggerFactory.getLogger;
import static usi2011.util.Specifications.SESSION_KEY;

import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.Cookie;
import org.jboss.netty.handler.codec.http.CookieEncoder;
import org.jboss.netty.handler.codec.http.DefaultCookie;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;

public final class HttpResponseHelper {
    private static final Logger logger = getLogger(HttpResponseHelper.class);

    public static HttpResponseStats stats = new HttpResponseStats();

    private HttpResponseHelper() {
        // response helper cannot be instanciated
    }

    public static HttpResponse response(final HttpResponseStatus status) {
        if (logger.isInfoEnabled()) {
            stats.newResponse(status);
        }
        return new DefaultHttpResponse(HTTP_1_1, status);
    }

    public static void writeResponse(final MessageEvent e, final HttpRequest request, final HttpResponseStatus status, final Exception exception) {
        if (logger.isDebugEnabled()) {
            exception.printStackTrace();
            writeResponse(e, request, status, exception.getClass().getSimpleName() + '\n' + exception.getMessage() + '\n' + getStackTrace(exception));
        } else {
            writeResponse(e, request, status, exception.getClass().getSimpleName() + '\n' + exception.getMessage());
        }
    }

    public static void writeResponse(final MessageEvent e, final HttpRequest request, final HttpResponseStatus status, final String content) {
        writeContent(e, request, response(status), content);
    }

    public static void writeResponseWithCookie(final MessageEvent e, final HttpRequest request, final HttpResponseStatus status, final String content,
            final String cookieValue) {
        final HttpResponse response = response(status);
        writeCookie(response, cookieValue);
        writeContent(e, request, response, content);
    }

    private static void writeCookie(final HttpResponse response, final String cookieValue) {
        final Cookie cookie = new DefaultCookie(SESSION_KEY, cookieValue);
        cookie.setDiscard(false);
        cookie.setPath("/");
        final CookieEncoder encoder = new CookieEncoder(true);
        encoder.addCookie(cookie);
        response.setHeader(SET_COOKIE, encoder.encode());
    }

    private static void writeContent(final MessageEvent e, final HttpRequest request, final HttpResponse response, final String content) {
        final boolean keepAlive = isKeepAlive(request);
        final int bufferLength = content == null ? 0 : content.length();
        if (content != null && bufferLength != 0) {
            response.setHeader(CONTENT_TYPE, "text/plain; charset=UTF-8");
            response.setContent(copiedBuffer(content, UTF_8));
        }
        if (keepAlive) {
            response.setHeader(CONTENT_LENGTH, bufferLength);
        }
        final ChannelFuture future = e.getChannel().write(response);
        if (!keepAlive) {
            future.addListener(CLOSE);
        }
        if (logger.isInfoEnabled()) {
            if (response.getStatus() != OK || response.getStatus() != CREATED) {
                logger.info("[{}:{}] {} {}", new Object[] { request.getMethod().getName(), request.getUri(), response.getStatus().getCode(), response.getStatus().getReasonPhrase() });
            }
        }
    }
}