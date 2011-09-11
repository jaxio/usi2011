package usi2011.http.support;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Long.parseLong;
import static java.lang.System.currentTimeMillis;
import static org.apache.commons.codec.binary.Hex.encodeHexString;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang.StringUtils.isNumeric;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.COOKIE;
import static usi2011.util.LogUtil.isInfoEnabled;
import static usi2011.util.Specifications.SESSION_KEY;
import static usi2011.util.SplitUtil.split;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

import org.jboss.netty.handler.codec.http.Cookie;
import org.jboss.netty.handler.codec.http.CookieDecoder;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import usi2011.exception.SessionKeyException;
import usi2011.util.SplitUtil;

/**
 * When adding a value in the cookie, crypt it with a salt and a timestamp so we are sure it will not be tricked
 */
@Service
public final class CookieService {
    private static final String KEY_WITH_EQUAL = SESSION_KEY + "=";
    private static final int KEY_WITH_EQUAL_LENGTH = KEY_WITH_EQUAL.length();
    private static final char CRYPT_SEPARATOR = '/';
    private static final int MD5_HEX_SIZE = 32;
    private static final String NOT_FOUND = null;
    private static final int NB_OF_SEPARATORS = 3;
    private static final CookieDecoder cookieDecoder = new CookieDecoder();
    private final MessageDigest md5;

    @Value("${cookie.salt.size:8}")
    private int saltSize = 8;
    @Value("${cookie.secret.key:usi}")
    private String secretKey = "usi";
    @Value("${cookie.encrypt:false}")
    private boolean encrypt;

    public CookieService() throws NoSuchAlgorithmException {
        md5 = MessageDigest.getInstance("MD5");
    }

    public CookieService(boolean encrypt) throws NoSuchAlgorithmException {
        this();
        this.encrypt = encrypt;
    }

    public boolean isLoggedIn(final HttpRequest request) {
        final String crypted = getValue(request, SESSION_KEY);
        if (crypted == NOT_FOUND) {
            return false;
        }
        try {
            get(crypted);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * If the data was not found or is invalid the {@link SessionKeyException} is thrown
     */
    public String get(final HttpRequest request) {
        final String value = getValue(request, SESSION_KEY);
        if (value == NOT_FOUND) {
            throw new SessionKeyException("No cookie found in " + SESSION_KEY);
        }
        return get(value);
    }

    /**
     * Returns the encodede value from the cookie
     * 
     * @param fromCookie
     * @return
     */
    String get(final String fromCookie) {
        checkNotNull(fromCookie);
        if (!encrypt) {
            return fromCookie;
        }

        final String[] values = split(fromCookie, CRYPT_SEPARATOR, NB_OF_SEPARATORS + 1);
        final String cookieValue = values[0];
        final String cookieSalt = values[1];
        final String cookieCrypted = values[2];
        final String cookieTimestamp = values[3];
        if (isInfoEnabled) {
            assertValid(fromCookie, cookieValue, cookieSalt, cookieCrypted, cookieTimestamp);
        }

        final String crypted = createValue(cookieValue, cookieSalt, cookieTimestamp);
        if (!crypted.equals(cookieSalt + CRYPT_SEPARATOR + cookieCrypted + CRYPT_SEPARATOR + cookieTimestamp)) {
            throw new SessionKeyException(SESSION_KEY + " contains an invalid value (" + fromCookie + ")");
        }
        return cookieValue;
    }

    private void assertValid(final String fromCookie, final String cookieValue, final String cookieSalt, final String cookieCrypted,
            final String cookieTimestamp) {
        if (cookieValue == null) {
            throw new SessionKeyException(fromCookie + " is missing elements the value");
        } else if (cookieSalt == null) {
            throw new SessionKeyException(fromCookie + " is missing elements the salt");
        } else if (cookieCrypted == null) {
            throw new SessionKeyException(fromCookie + " is missing elements the hash");
        } else if (cookieTimestamp == null) {
            throw new SessionKeyException(fromCookie + " is missing elements the timestamp");
        } else if (!isNumeric(cookieTimestamp)) {
            throw new SessionKeyException(fromCookie + " has an invalid (" + cookieTimestamp + ")");
        } else {
            long timestampOfCreation = parseLong(cookieTimestamp);
            if (currentTimeMillis() - timestampOfCreation < 0) {
                throw new SessionKeyException(fromCookie + " timestamp is in the future (" + cookieTimestamp + ")");
            }
        }
    }

    public String set(final Object object) {
        checkNotNull(object);
        final String value = object.toString();
        if (!encrypt) {
            return value;
        }
        final String salt = randomAlphanumeric(saltSize);
        final String timestamp = "" + currentTimeMillis();
        return value + CRYPT_SEPARATOR + createValue(value, salt, timestamp);
    }

    private String createValue(final String value, final String salt, final String timestamp) {
        final String toCrypt = value + secretKey + salt + timestamp;
        final String crypted = encodeHexString(md5.digest(toCrypt.getBytes()));
        return salt + CRYPT_SEPARATOR + crypted + CRYPT_SEPARATOR + timestamp;
    }

    private String getValue(final HttpRequest request, final String key) {
        final String cookieHeader = getCookie(request);

        if (cookieHeader != NOT_FOUND) {
            final String values[] = SplitUtil.split(cookieHeader, ';');
            // not in golden path, rely on netty CookieEncoder
            for (final String value : values) {
                if (KEY_WITH_EQUAL.equals(value)) {
                    return value.substring(KEY_WITH_EQUAL_LENGTH);
                }
            }
            // not in golden path, rely on netty CookieEncoder
            final Set<Cookie> cookies = cookieDecoder.decode(cookieHeader);
            for (Cookie c : cookies) {
                if (c.getName().equalsIgnoreCase(key)) {
                    return c.getValue();
                }
            }
        }
        // cookie not found...
        return NOT_FOUND;
    }

    private String getCookie(final HttpRequest request) {
        final String cookie = request.getHeader(COOKIE);
        if (isInfoEnabled) {
            assertCookie(cookie);
        }
        return cookie;
    }

    private void assertCookie(final String cookie) {
        if (cookie != null && encrypt && cookie.length() <= (MD5_HEX_SIZE + saltSize + NB_OF_SEPARATORS)) {
            throw new SessionKeyException("Cookie is invalid, its value is too short : " + cookie);
        }
    }
}