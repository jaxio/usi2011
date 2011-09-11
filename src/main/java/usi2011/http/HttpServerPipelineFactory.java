package usi2011.http;

import static java.lang.Runtime.getRuntime;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.jboss.netty.channel.Channels.pipeline;
import static org.slf4j.LoggerFactory.getLogger;
import static usi2011.util.LogUtil.isInfoEnabled;
import static usi2011.util.LogUtil.isWarnEnabled;

import java.util.concurrent.ExecutorService;

import javax.annotation.PostConstruct;

import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.StaticChannelPipeline;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpContentCompressor;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import usi2011.http.decoder.FastHttpRequestDecoder;
import usi2011.http.decoder.FasterHttpRequestDecoder;
import usi2011.http.encoder.FasterHttpResponseEncoder;
import usi2011.http.support.TimeLoggerHandler;

@Component
@ManagedResource
public class HttpServerPipelineFactory implements ChannelPipelineFactory {
    private static final Logger logger = getLogger(HttpServerPipelineFactory.class);

    public static final String DEFAULT_DECODER = "default";
    public static final String FAST_DECODER = "fast";
    public static final String FASTER_DECODER = "faster";

    @Autowired
    private StateMachineHttpRequestHandler stateMachineHttpRequestHandler;
    @Autowired
    private HttpBenchRequestHandler httpBenchRequestHandler;

    @Value("${http.enable.compression:false}")
    private boolean enableCompression;
    @Value("${http.enable.chunks:false}")
    private boolean enableHttpChunks;
    @Value("${http.enable.logger:false}")
    private boolean enableLogger;
    @Value("${http.enable.time.logger:false}")
    private boolean enableTimeLogger;
    @Value("${http.maximum.request.length:512}")
    private int maxInitialLineLength = 512;
    @Value("${http.maximum.header.size:2048}")
    private int maxHeaderSize = 512;
    @Value("${http.maximum.chunk.size:8192}")
    private int maxChunkSize = 8192;
    @Value("${http.decoder:default}")
    private String httpDecoder;
    @Value("${http.enable.bench:false}")
    private boolean httpBenchEnabled;

    private boolean useDefaultDecoder;
    private boolean useFastDecoder;
    private boolean useFasterDecoder;

    private long nbChannelsOpened = 0;

    @Value("${httpServerPipelineFactory.executionHandler.enable}")
    private boolean enableExecutionHandler;
    @Value("${httpServerPipelineFactory.executionService.nThreads}")
    private int nThreadsExecutionService;

    private ExecutorService executorService;
    private ExecutionHandler executionHandler;

    @PostConstruct
    public void init() {
        if (FAST_DECODER.equalsIgnoreCase(httpDecoder)) {
            logger.warn("Using {} decoder", FAST_DECODER);
            useFastDecoder = true;
        } else if (FASTER_DECODER.equalsIgnoreCase(httpDecoder)) {
            logger.warn("Using {} decoder", FASTER_DECODER);
            useFasterDecoder = true;
        } else {
            logger.warn("Using {} decoder", DEFAULT_DECODER);
            useDefaultDecoder = true;
        }
        if (enableExecutionHandler) {
            if (nThreadsExecutionService <= 0) {
                nThreadsExecutionService = getRuntime().availableProcessors() * 2; // total nb of core actually x 2
            }

            if (isWarnEnabled) {
                logger.warn("THREAD INFO: httpServerPipelineFactory.executionService.nThreads=" + nThreadsExecutionService);
            }
            executorService = new OrderedMemoryAwareThreadPoolExecutor(nThreadsExecutionService, 1048576, 1048576, 120, SECONDS);
            executionHandler = new ExecutionHandler(executorService);
        } else {
            if (isWarnEnabled) {
                logger.warn("THREAD INFO: no execution handler... requests are processed entirely by nio worker");
            }
        }
    }

    private ChannelHandler getDecoder() {
        if (useFasterDecoder) {
            return new FasterHttpRequestDecoder(maxInitialLineLength, maxHeaderSize, maxChunkSize);
        } else if (useFastDecoder) {
           return new FastHttpRequestDecoder(maxInitialLineLength, maxHeaderSize, maxChunkSize);
        } else if (useDefaultDecoder) {
           return new HttpRequestDecoder(maxInitialLineLength, maxHeaderSize, maxChunkSize);
        } else {
            throw new IllegalStateException("Need a decoder to be setup");
        }
    }

    private ChannelHandler getEncoder() {
        if (useFasterDecoder) {
            return new FasterHttpResponseEncoder();
        } else if (useDefaultDecoder || useFastDecoder) {
           return new HttpResponseEncoder();
        } else {
            throw new IllegalStateException("Need a decoder to be setup");
        }
    }
    
    public ChannelPipeline getPipeline() {
        nbChannelsOpened++;
        if (httpBenchEnabled) {
            return new StaticChannelPipeline(getDecoder(), executionHandler, httpBenchRequestHandler, getEncoder());
        } else {
            final ChannelPipeline pipeline = pipeline();
            pipeline.addLast("decoder", getDecoder());
            pipeline.addLast("encoder", getEncoder());
            if (enableHttpChunks) {
                pipeline.addLast("aggregator", new HttpChunkAggregator(1048576));
            }
            if (enableCompression) {
                pipeline.addLast("deflater", new HttpContentCompressor());
            }
            if (enableLogger) {
                pipeline.addLast("logger", new LoggingHandler());
            }
            if (enableTimeLogger) {
                if (isWarnEnabled || isInfoEnabled) {
                    // no need to add the handler if the correct level of log is not present
                    // as its only purpose is to log
                    pipeline.addLast("timeLogger", new TimeLoggerHandler());
                }
            }
            if (enableExecutionHandler) {
                pipeline.addLast("executor", executionHandler);
            }
            pipeline.addLast("business", stateMachineHttpRequestHandler);
            return pipeline;
        }
    }

    @ManagedAttribute
    public boolean isEnableCompression() {
        return enableCompression;
    }

    @ManagedAttribute
    public void setEnableCompression(boolean enableCompression) {
        this.enableCompression = enableCompression;
    }

    @ManagedAttribute
    public boolean isEnableHttpChunks() {
        return enableHttpChunks;
    }

    @ManagedAttribute
    public void setEnableHttpChunks(boolean enableHttpChunks) {
        this.enableHttpChunks = enableHttpChunks;
    }

    @ManagedAttribute
    public boolean isEnableLogger() {
        return enableLogger;
    }

    @ManagedAttribute
    public void setEnableLogger(boolean enableLogger) {
        this.enableLogger = enableLogger;
    }

    @ManagedAttribute
    public long getNbChannelsOpened() {
        return nbChannelsOpened;
    }

    @ManagedAttribute
    public void setNbChannelsOpened(long nbChannelsOpened) {
        this.nbChannelsOpened = nbChannelsOpened;
    }
}