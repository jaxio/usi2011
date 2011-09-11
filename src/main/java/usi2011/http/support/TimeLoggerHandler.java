package usi2011.http.support;

import static org.slf4j.LoggerFactory.getLogger;
import static usi2011.util.LogUtil.isInfoEnabled;
import static usi2011.util.LogUtil.isWarnEnabled;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.WriteCompletionEvent;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.slf4j.Logger;

public final class TimeLoggerHandler extends SimpleChannelUpstreamHandler {
    private static final Logger logger = getLogger(TimeLoggerHandler.class);
    private long warnThresholdMs = 200l;
    private static Map<ChannelHandlerContext, Stat> map = new ConcurrentHashMap<ChannelHandlerContext, Stat>();

    @Override
    public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e) throws Exception {
        Long t0 = System.currentTimeMillis();
        // HttpRequest req = (HttpRequest) e.getMessage();
        // we do not use ctx.setAttachment as I suspect some other might too
        map.put(ctx, new Stat(((HttpRequest) e.getMessage()).getUri(), t0));
        super.messageReceived(ctx, e);
    }

    @Override
    public void writeComplete(ChannelHandlerContext ctx, WriteCompletionEvent e) throws Exception {
        super.writeComplete(ctx, e);
        final Stat stat = map.remove(ctx);
        final long durationMs = System.currentTimeMillis() - stat.t0;

        if (!stat.uri.startsWith("/api/question") && durationMs > warnThresholdMs) {
            if (isWarnEnabled) {
                logger.warn("duration: {}ms, uri: {}, bytes: {}", new Object[] { durationMs, stat.uri, e.getWrittenAmount() });
            }
        } else {
            if (isInfoEnabled) {
                logger.info("duration: {}ms, uri: {}, bytes: {}", new Object[] { durationMs, stat.uri, e.getWrittenAmount() });
            }
        }
    }

    public static class Stat {
        String uri;
        Long t0;

        public Stat(String uri, Long t0) {
            this.uri = uri;
            this.t0 = t0;
        }
    }
}