package usi2011.http.support;

import static java.net.URLConnection.guessContentTypeFromName;
import static org.apache.commons.io.IOUtils.toByteArray;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.slf4j.LoggerFactory.getLogger;
import static usi2011.util.LogUtil.isDebugEnabled;
import static usi2011.util.LogUtil.isWarnEnabled;

import java.io.File;

import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

/**
 * Serve static resources
 */
@Component
@ManagedResource
public class HttpServeFile {
    private static final Logger logger = getLogger(HttpServeFile.class);
    @Autowired
    private HttpResponseService httpResponseService;
    @Value("${http.server.htdocs:htdocs/}")
    private String htDocBaseDir;

    private long filesServed = 0;
    private long errorServed = 0;

    public boolean serve(final MessageEvent e, final HttpRequest request) {
        final String filename = getFilename(request.getUri());
        final Resource resource = resource(filename);

        if (!resource.exists()) {
            return false;
        }
        try {
            httpResponseService.write(e.getChannel(), //
                    OK, //
                    HttpResponseService.isKeepAlive(request), //
                    toByteArray(resource.getInputStream()), //
                    contentType(filename));
            if (isDebugEnabled) {
                logger.debug("Served static resource {}", filename);
            }
            filesServed++;
            return true;
        } catch (Exception ex) {
            errorServed++;
            if (isDebugEnabled) {
                ex.printStackTrace();
            }
            if (isWarnEnabled) {
                logger.warn("could not load " + filename, e);
            }
            throw new IllegalArgumentException(ex);
        }
    }

    private String getFilename(final String uri) {
        final String filename = htDocBaseDir + uri.replace('/', File.separatorChar);
        if (filename.endsWith("/")) {
            return filename + "/index.html";
        }
        return filename;
    }

    private String contentType(final String filename) {
        final String contentType = guessContentTypeFromName(filename);
        return contentType == null ? "application/octet-stream" : contentType;
    }

    private Resource resource(final String filename) {
        // yes security should be somewhere ... right here
        if (filename.startsWith("/")) {
            return new FileSystemResource(filename);
        } else {
            return new ClassPathResource(filename);
        }
    }

    @ManagedAttribute
    public long getNbErrorsServed() {
        return errorServed;
    }

    @ManagedAttribute
    public void setNbErrorsServerd(long errorServed) {
        this.errorServed = errorServed;
    }

    @ManagedAttribute
    public long getNbFilesServed() {
        return filesServed;
    }

    @ManagedAttribute
    public void setNbFilesServed(long filesServed) {
        this.filesServed = filesServed;
    }
}