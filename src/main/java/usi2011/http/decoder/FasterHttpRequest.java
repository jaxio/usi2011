package usi2011.http.decoder;

import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.COOKIE;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.SET_COOKIE;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.http.DefaultHttpMessage;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpHeaders.Values;
import org.jboss.netty.handler.codec.http.HttpMessage;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.slf4j.Logger;

import com.google.common.collect.Sets;

/**
 * handle only the headers we need that is
 * <ul>
 * <li>HttpHeaders.Names.CONNECTION</li>
 * <li>HttpHeaders.Names.CONTENT_LENGTH</li>
 * <li>HttpHeaders.Names.COOKIE</li>
 * </ul>
 * Use a multi map instead of the slow {@link HttpHeaders} that checks for validity etc
 * 
 * @see DefaultHttpRequest
 * @see DefaultHttpMessage
 */
public class FasterHttpRequest implements HttpMessage, HttpRequest {
    private static final Logger logger = getLogger(FasterHttpRequest.class);
    private HttpMethod method;
    private HttpVersion version;
    protected String uri;
    protected String cookie;
    protected String contentType;
    protected String contentLength;
    protected String keepAliveLength;

    protected String setCookie;
    protected String connection;

    private ChannelBuffer content = ChannelBuffers.EMPTY_BUFFER;
    public static final int COOKIE_LENGTH = COOKIE.length();
    public static final int CONTENT_TYPE_LENGTH = CONTENT_TYPE.length();
    public static final int CONTENT_LENGTH_LENGTH = CONTENT_LENGTH.length();

    // beware, set_cookie and connection they have the same length beway
    public static final int SET_COOKIE_OR_CONNECTION_LENGTH = SET_COOKIE.length();

    private static final Set<String> headerSet = Sets.newHashSet(COOKIE, CONTENT_LENGTH);

    public FasterHttpRequest(HttpVersion httpVersion, HttpMethod method, String uri) {
        this.method = method;
        this.uri = uri;
    }

    public FasterHttpRequest(String version, String method, String uri) {
        if ("HTTP/1.1".equals(version)) {
            this.version = HttpVersion.HTTP_1_1;
        } else {
            this.version = HttpVersion.HTTP_1_0;
            this.connection = Values.CLOSE;
        }
        int length = method.length();
        if (length == 3 && "GET".equals(method)) {
            this.method = HttpMethod.GET;
        } else if (length == 4 && "POST".equals(method)) {
            this.method = HttpMethod.POST;
        }
        this.uri = uri;
    }

    /**
     * Used when building a http response message
     */
    public FasterHttpRequest(HttpVersion httpVersion) {
        this.method = null;
        this.uri = null;
    }

    /**
     * Main difference is we support only String in headers
     */
    @Override
    public void addHeader(final String name, final Object value) {
        int length = name == null ? 0 : name.length();
        if (length == COOKIE_LENGTH) {
            cookie = (String) value;
        } else if (length == CONTENT_LENGTH_LENGTH) {
            contentLength = (String) value;
        } else if (length == CONTENT_TYPE_LENGTH) {
            contentType = (String) value;
        } else if (length == SET_COOKIE_OR_CONNECTION_LENGTH) {
            if (name.charAt(0) == 'C') {
                connection = (String) value;
            } else {
                setCookie = (String) value;
            }
        } else {
            logger.warn("Header [{}]:[{}] is not supported", name, value);
        }
    }

    @Override
    public void setHeader(final String name, final Object value) {
        addHeader(name, value);
    }

    @Override
    public void setHeader(final String name, final Iterable<?> values) {
        for (Object value : values) {
            addHeader(name, value);
        }
    }

    @Override
    public void removeHeader(final String name) {
    }

    @Override
    public String getHeader(String name) {
        int length = name == null ? 0 : name.length();
        if (length == COOKIE_LENGTH) {
            return cookie;
        } else if (length == CONTENT_LENGTH_LENGTH) {
            return contentLength;
        } else if (length == CONTENT_TYPE_LENGTH) {
            return contentType;
        } else if (length == SET_COOKIE_OR_CONNECTION_LENGTH) {
            if (name.charAt(0) == 'C') {
                return connection;
            } else {
                return setCookie;
            }
        } else {
            logger.warn("Header [{}] should not be requested", name);
            return null;
        }
    }

    @Override
    public List<String> getHeaders(String name) {
        int length = name == null ? 0 : name.length();
        List<String> ret = new ArrayList<String>(1);
        if (length == COOKIE_LENGTH) {
            ret.add(cookie);
        } else if (length == CONTENT_LENGTH_LENGTH) {
            ret.add(contentLength);
        } else if (length == CONTENT_TYPE_LENGTH) {
            ret.add(contentType);
        } else if (length == SET_COOKIE_OR_CONNECTION_LENGTH) {
            if (name.charAt(0) == 'C') {
                ret.add(connection);
            } else {
                ret.add(setCookie);
            }
        } else {
            // TODO: LOG
        }
        return ret;
    }

    static class REntry implements Entry<String, String> {
        private final String name;
        private final String value;

        public REntry(String name, String value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public String getKey() {
            return name;
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public String setValue(String value) {
            return null;
        }
    }

    @Override
    public List<Entry<String, String>> getHeaders() {
        List<Entry<String, String>> ret = new ArrayList<Entry<String, String>>(2);
        if (cookie != null) {
            ret.add(new REntry(COOKIE, cookie));
        }
        if (contentLength != null) {
            ret.add(new REntry(CONTENT_LENGTH, contentLength));
        }
        if (contentType != null) {
            ret.add(new REntry(CONTENT_TYPE, contentType));
        }
        if (setCookie != null) {
            ret.add(new REntry(SET_COOKIE, setCookie));
        }
        if (connection != null) {
            ret.add(new REntry(CONNECTION, connection));
        }
        return ret;
    }

    @Override
    public boolean containsHeader(String name) {
        int length = name == null ? 0 : name.length();
        if (length == COOKIE_LENGTH) {
            return cookie != null;
        } else if (length == CONTENT_LENGTH_LENGTH) {
            return contentLength != null;
        } else if (length == CONTENT_TYPE_LENGTH) {
            return contentType.length() > 0;
        } else if (length == SET_COOKIE_OR_CONNECTION_LENGTH) {
            if (name.charAt(0) == 'C') {
                return connection != null;
            } else {
                return setCookie != null;
            }
        } else {
            System.out.println("requested " + name);
            return false;
        }
    }

    @Override
    public Set<String> getHeaderNames() {
        return headerSet;
    }

    @Override
    public HttpMethod getMethod() {
        return method;
    }

    @Override
    public void setMethod(HttpMethod method) {
        this.method = method;
    }

    @Override
    public String getUri() {
        return uri;
    }

    @Override
    public void setUri(String uri) {
        this.uri = uri;
    }

    @Override
    public HttpVersion getProtocolVersion() {
        return HttpVersion.HTTP_1_1;
    }

    @Override
    public void setProtocolVersion(HttpVersion version) {
    }

    @Override
    public ChannelBuffer getContent() {
        return content;
    }

    @Override
    public void setContent(ChannelBuffer content) {
        if (content == null) {
            content = ChannelBuffers.EMPTY_BUFFER;
        }
        this.content = content;
    }

    @Override
    public void clearHeaders() {

    }

    @Override
    public long getContentLength() {
        return 0L;
    }

    @Override
    public long getContentLength(long defaultValue) {
        return defaultValue;
    }

    @Override
    public boolean isChunked() {
        return false;
    }

    static boolean isTransferEncodingChunked(HttpMessage m) {
        return false;
    }

    @Override
    public void setChunked(boolean chunked) {
    }

    @Override
    public boolean isKeepAlive() {
        return true;
    }
}
