package usi2011.http;

import static org.apache.commons.lang.exception.ExceptionUtils.getStackTrace;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.slf4j.LoggerFactory.getLogger;
import static usi2011.util.LogUtil.isDebugEnabled;
import static usi2011.util.LogUtil.isWarnEnabled;

import java.util.Date;

import javax.annotation.PostConstruct;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import usi2011.http.support.HttpResponseService;

@Component
public class HttpBenchRequestHandler extends SimpleChannelUpstreamHandler {
    private static final Logger logger = getLogger(StateMachineHttpRequestHandler.class);
    @Autowired
    private HttpResponseService httpResponseService;
    @Value("${http.enable.bench:false}")
    private boolean httpBenchEnabled;

    @PostConstruct
    public void init() {
        if (httpBenchEnabled) {
            logger.error("!!!!!!!!!!!!!!!!!!");
            logger.error("");
            logger.error("BENCH MODE ENABLED");
            logger.error("");
            logger.error("!!!!!!!!!!!!!!!!!!");
        }
    }

    @SuppressWarnings("deprecation")
    private byte[] getMessage() {
        final String s = "HTTP/1.0 200 OK\r\n" + //
                "Date:Tue, 21 Jun 2011 17:21:35 GMT\r\n" + //
                "Content-Type: text/html\r\n" + //
                "\r\n" + new Date() + "tototo\r\n";
        final int length = s.length();

        // to prevent convertion as we use only ASCII
        byte[] ret = new byte[length + 1];
        s.getBytes(0, length, ret, 0);

        return ret;
    }

    private ChannelBuffer getChannelBuffer() {
        return ChannelBuffers.wrappedBuffer(getMessage());
    }

    @Override
    public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e) throws Exception {
        final Channel channel = e.getChannel();
        final HttpRequest request = (HttpRequest) e.getMessage();

        responseService(e, request);
        if (false) {
            directWriteToChannel(channel);
        }
    }

    private void responseService(final MessageEvent e, final HttpRequest request) {
        httpResponseService.writeResponseWithCookie(e, request, HttpResponseStatus.CREATED, "something", "my cookie");
    }

    private void directWriteToChannel(final Channel channel) {
        Channels.write(channel, getChannelBuffer());// .addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final ExceptionEvent exception) {
        if (isDebugEnabled) {
            exception.getCause().printStackTrace();
        }
        exception.getCause().printStackTrace();
        if (isWarnEnabled) {
            logger.warn("Got exception {}, from remote address {}", exception.getCause().getMessage(), ctx.getChannel().getRemoteAddress());
        }
        if (ctx.getChannel().isBound() && ctx.getChannel().isWritable()) {
            String content = exception.getCause().getMessage();
            if (isDebugEnabled) {
                exception.getCause().printStackTrace();
                content += "\n" + getStackTrace(exception.getCause());
            }
            httpResponseService.write(ctx.getChannel(), httpResponseService.response(HttpVersion.HTTP_1_0, BAD_REQUEST), false, content, "text/plain");
        }
    }
}
