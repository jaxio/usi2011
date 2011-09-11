package usi2011.http.support;

import static usi2011.util.Specifications.URI_RANKING;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.HttpRequest;

/**
 * Just decide to either invoke directly our StateMachineHttpRequestHandler or to bubble up the event to the next buddy in the pipeline.
 */
public class SelectiveAsyncRequestHandler extends SimpleChannelUpstreamHandler {
    SimpleChannelUpstreamHandler stateMachineHttpRequestHandler;

    public SelectiveAsyncRequestHandler(SimpleChannelUpstreamHandler stateMachineHttpRequestHandler) {
        this.stateMachineHttpRequestHandler = stateMachineHttpRequestHandler;
    }

    @Override
    public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e) throws Exception {
        final HttpRequest request = (HttpRequest) e.getMessage();
        if (request.getUri().startsWith(URI_RANKING)) {
            stateMachineHttpRequestHandler.messageReceived(ctx, e); // do it in this thread as it should be pretty fast
        } else {
            super.messageReceived(ctx, e); // next handler (ie executor handler) will receive it and delegate it to an execution service.
        }
    }
}