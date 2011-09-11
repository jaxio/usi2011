package usi2011.http.support;

import static org.apache.commons.lang.exception.ExceptionUtils.getStackTrace;
import static org.jboss.netty.buffer.ChannelBuffers.wrappedBuffer;
import static org.jboss.netty.channel.ChannelFutureListener.CLOSE;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.SET_COOKIE;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.jboss.netty.util.CharsetUtil.UTF_8;
import static org.slf4j.LoggerFactory.getLogger;
import static usi2011.http.HttpServerPipelineFactory.DEFAULT_DECODER;
import static usi2011.http.HttpServerPipelineFactory.FASTER_DECODER;
import static usi2011.http.HttpServerPipelineFactory.FAST_DECODER;
import static usi2011.util.LogUtil.isDebugEnabled;
import static usi2011.util.LogUtil.isInfoEnabled;
import static usi2011.util.Specifications.SESSION_KEY;

import javax.annotation.PostConstruct;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.DefaultChannelFuture;
import org.jboss.netty.channel.DownstreamMessageEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders.Names;
import org.jboss.netty.handler.codec.http.HttpHeaders.Values;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import usi2011.http.decoder.FastHttpResponse;
import usi2011.http.decoder.FasterHttpResponse;

@Component
public final class HttpResponseService {
    private static final Logger logger = getLogger(HttpResponseService.class);
    @Autowired
    private HttpResponseStatsService httpResponseStatsService;
    @Value("${http.decoder:default}")
    private String httpDecoder;
    private boolean useDefaultHttpResponse = false;
    private boolean useFastHttpResponse = false;
    private boolean useFasterHttpResponse = false;

    @PostConstruct
    public void init() {
        if (FAST_DECODER.equalsIgnoreCase(httpDecoder)) {
            logger.warn("Using {} response", FAST_DECODER);
            useFastHttpResponse = true;
        } else if (FASTER_DECODER.equalsIgnoreCase(httpDecoder)) {
            logger.warn("Using {} response", FASTER_DECODER);
            useFasterHttpResponse = true;
        } else {
            logger.warn("Using {} response", DEFAULT_DECODER);
            useDefaultHttpResponse = true;
        }
    }

    public HttpResponse response(final HttpVersion version, final HttpResponseStatus status) {
        if (isInfoEnabled) {
            httpResponseStatsService.newResponse(status);
        }
        if (useFasterHttpResponse) {
            return new FasterHttpResponse(version, status);
        } else if (useFastHttpResponse) {
            return new FastHttpResponse(version, status);
        } else if (useDefaultHttpResponse){
            return new DefaultHttpResponse(version, status);
        } else {
            throw new IllegalStateException("Not right decoder");
        }
    }

    public void writeResponse(final MessageEvent e, final HttpRequest request, final HttpResponseStatus status, final Exception exception) {
        logger.error(exception.getMessage(), exception);
        if (isDebugEnabled) {
            writeResponse(e, request, status, exception.getClass().getSimpleName() + '\n' + exception.getMessage() + '\n' + getStackTrace(exception));
        } else {
            writeResponse(e, request, status, exception.getClass().getSimpleName() + '\n' + exception.getMessage());
        }
    }

    public void writeResponse(final MessageEvent e, final HttpRequest request, final HttpResponseStatus status, final String content) {
        writeContent(e, request, response(request.getProtocolVersion(), status), content);
    }

    public void writeResponseWithCookie(final MessageEvent e, final HttpRequest request, final HttpResponseStatus status, final String content,
            final String cookieValue) {
        final HttpResponse response = response(request.getProtocolVersion(), status);
        response.setHeader(SET_COOKIE, SESSION_KEY + "=\"" + cookieValue + "\";Path=/");
        writeContent(e, request, response, content);
    }

    private void writeContent(final MessageEvent e, final HttpRequest request, final HttpResponse response, final String content) {
        if (isInfoEnabled) {
            if (response.getStatus() != OK && response.getStatus() != CREATED) {
                logger.info("[{}:{}] {} {}", new Object[] { request.getMethod().getName(), request.getUri(), response.getStatus().getCode(),
                        response.getStatus().getReasonPhrase() });
            }
        }
        write(e.getChannel(), response, isKeepAlive(request), content, "text/html");
    }

    public static boolean isKeepAlive(final HttpRequest request) {
        return !Values.CLOSE.equals(request.getHeader(Names.CONNECTION));
    }

    public void write(final Channel channel, final HttpResponseStatus status, final boolean keepAlive, final String content, final String contentType) {
        write(channel, status, keepAlive, content.getBytes(), contentType);
    }
    public void write(final Channel channel, final HttpResponseStatus status, final boolean keepAlive, final byte[] content, final String contentType) {
        final HttpResponse response = response(HttpVersion.HTTP_1_0, status);
        if (content != null) {
            response.setHeader(CONTENT_TYPE, contentType);
            response.setContent(wrappedBuffer(content));
        }
        if (keepAlive) {
            response.setHeader(CONTENT_LENGTH, "" + (content != null ? content.length : 0));
        } else {
            response.setHeader(CONNECTION, Values.CLOSE);
        }
        if (useFasterHttpResponse) {
            directWrite(channel, response, keepAlive);
        } else {
            write(channel, response, keepAlive);
        }
    }

    public void write(final Channel channel, final HttpResponse response, final boolean keepAlive, final String content, final String contentType) {
        if (content != null) {
            response.setHeader(CONTENT_TYPE, contentType);
            if (response instanceof FasterHttpResponse) {
                ((FasterHttpResponse)response).setContentAsString(content);
            } else {
                response.setContent(wrappedBuffer(content.getBytes(UTF_8)));
            }
        }
        if (keepAlive) {
            response.setHeader(CONTENT_LENGTH, "" + (content != null ? content.length() : 0));
        } else {
            response.setHeader(CONNECTION, Values.CLOSE);
        }
        if (useFasterHttpResponse) {
            directWrite(channel, response, keepAlive);
        } else {
            write(channel, response, keepAlive);
        }
    }

    public final void write(final Channel channel, final HttpResponse response, final boolean keepAlive) {
        if (!keepAlive) {
            channel.write(response).addListener(CLOSE);
        } else {
            channel.write(response);
        }
    }

    public final void directWrite(final Channel channel, final HttpResponse response, final boolean keepAlive) {
        ChannelFuture future = new DefaultChannelFuture(channel, false);
        channel.getPipeline().sendDownstream( //
                new DownstreamMessageEvent(channel,future, response, null));
        if (!keepAlive) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }
}