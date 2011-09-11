package usi2011.http.support;

import static com.google.common.collect.Lists.newArrayList;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.COOKIE;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.TRANSFER_ENCODING;

import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpVersion;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * handle only the headers we need that is
 * <ul>
 * <li>HttpHeaders.Names.CONNECTION</li>
 * <li>HttpHeaders.Names.CONTENT_LENGTH</li>
 * <li>HttpHeaders.Names.COOKIE</li>
 * </ul>
 * Use a multi map instead of the slow {@link HttpHeaders} that checks for validity etc
 */
public final class FastHttpRequest extends DefaultHttpRequest {
    private static final char COOKIE_CHAR_0 = COOKIE.charAt(0);
    private static final char CONNECTION_CHAR_0 = CONNECTION.charAt(0);
    private static final char CONTENT_LENGTH_CHAR_0 = CONTENT_LENGTH.charAt(0);
    private static final char TRANSFER_ENCODING_CHAR_0 = TRANSFER_ENCODING.charAt(0);
    private static final char COOKIE_CHAR_1 = COOKIE.charAt(1);
    private static final char CONNECTION_CHAR_1 = CONNECTION.charAt(1);
    private static final char CONTENT_LENGTH_CHAR_1 = CONTENT_LENGTH.charAt(1);
    private static final char TRANSFER_ENCODING_CHAR_1 = TRANSFER_ENCODING.charAt(0);
    public static final int MAX_HTTP_HEADERS_IN_FAST_MODE = 4;
    

    private final Multimap<String, String> headers = HashMultimap.create();

    public FastHttpRequest(HttpVersion httpVersion, HttpMethod method, String uri) {
        super(httpVersion, method, uri);
    }

    public static boolean acceptHeader(String headerName) {
        final char char1 = headerName.charAt(2);
        if (char1 != COOKIE_CHAR_1 && char1 != CONNECTION_CHAR_1 && char1 != CONTENT_LENGTH_CHAR_1 && char1 != TRANSFER_ENCODING_CHAR_1) {
            return false;
        }
        final char char0 = headerName.charAt(0);
        if (char0 != COOKIE_CHAR_0 && char0 != CONNECTION_CHAR_0 && char0 != CONTENT_LENGTH_CHAR_0 && char1 != TRANSFER_ENCODING_CHAR_0) {
            return false;
        }
        return COOKIE.equals(headerName) || CONNECTION.equals(headerName) || CONTENT_LENGTH.equals(headerName) || TRANSFER_ENCODING.equals(headerName);
    }

    @Override
    public void addHeader(final String name, final Object value) {
        headers.put(name, (String) value);
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
        headers.removeAll(name);
    }

    @Override
    public String getHeader(String name) {
        if (headers.containsKey(name)) {
            return headers.get(name).iterator().next();
        } else {
            return null;
        }
    }

    @Override
    public List<String> getHeaders(String name) {
        return newArrayList(headers.get(name));
    }

    @Override
    public List<Entry<String, String>> getHeaders() {
        return newArrayList(headers.entries());
    }

    @Override
    public boolean containsHeader(String name) {
        return headers.containsKey(name);
    }

    @Override
    public Set<String> getHeaderNames() {
        return headers.keySet();
    }
}
