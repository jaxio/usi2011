package usi2011.http;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.slf4j.LoggerFactory.getLogger;
import static usi2011.util.LogUtil.isInfoEnabled;
import static usi2011.util.LogUtil.isWarnEnabled;

import java.net.InetSocketAddress;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.stereotype.Service;

import usi2011.util.NamedThreadFactory;

@Service
public class HttpServer {
    private static final Logger logger = getLogger(HttpServer.class);
    @Autowired
    private HttpServerPipelineFactory pipelineFactory;
    @Value("${http.server.port:9090}")
    private int defaultPort;
    @Value("${http.server.nb.ports.to.scan:100}")
    private int nbPortsToScan;
    private int listeningOnPort;

    @Value("${httpServer.nioBoss.nThreads}")
    private int nbNioBoss;

    @Value("${httpServer.nioWorker.nThreads}")
    private int nbNioWorkers;

    public void build() {
        if (nbNioBoss <= 0) {
            nbNioBoss = Runtime.getRuntime().availableProcessors() * 2;
        }
        
        if (nbNioWorkers <= 0) {
            nbNioWorkers = Runtime.getRuntime().availableProcessors() * 2;
        }        

        if (isWarnEnabled) {
            logger.warn("THREAD INFO: httpServer.nioBoss.nThreads={}, httpServer.nioWorker.nThreads={}", nbNioBoss, nbNioWorkers);
        }
        
        final ServerBootstrap bootstrap = new ServerBootstrap( //
                new NioServerSocketChannelFactory( //
                        newFixedThreadPool(nbNioBoss, new NamedThreadFactory("netty-nio-accept-connection")), //
                        newFixedThreadPool(nbNioWorkers, new NamedThreadFactory("netty-nio-read-write")), //
                        nbNioWorkers));
        bootstrap.setOption("reuseAddress", true);
        bootstrap.setOption("child.keepAlive", true);
        bootstrap.setOption("child.tcpNoDelay", true);
        bootstrap.setOption("child.soLinger", true);
        bootstrap.setOption("child.reuseAddress", true);
        bootstrap.setPipelineFactory(pipelineFactory);

        for (int port = defaultPort; port < defaultPort + nbPortsToScan; port++) {
            if (bind(bootstrap, port)) {
                return;
            }
        }
        logger.error("Trying {} ports, they were all used, abort", nbPortsToScan);
    }

    private boolean bind(ServerBootstrap bootstrap, int port) {
        try {
            bootstrap.bind(new InetSocketAddress(port));
            if (isWarnEnabled) {
                logger.warn("Http Server listening on port {}", port);
            }
            listeningOnPort = port;
            return true;
        } catch (Exception e) {
            if (isInfoEnabled) {
                logger.info("Port {} is already taken, trying next one", port);
            }
            return false;
        }
    }

    @ManagedAttribute
    public int getListeningOnPort() {
        return listeningOnPort;
    }
}