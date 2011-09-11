package usi2011.http.support;

import static org.fest.assertions.Assertions.assertThat;
import static usi2011.util.Specifications.SESSION_KEY;

import org.jboss.netty.handler.codec.http.Cookie;
import org.jboss.netty.handler.codec.http.CookieEncoder;
import org.jboss.netty.handler.codec.http.DefaultCookie;
import org.junit.Test;

public class HttpResponseServiceTest {

    @Test
    public void valid() {
        String cookieValue = "dummy";
        assertThat(cookie(cookieValue)).isEqualTo(SESSION_KEY + "=" + cookieValue + ";Path=/");
    }

    @Test
    public void validNeedsEncoding() {
        String cookieValue = "smith.smith@hotmail.com:smith:smith:000000000000000000000";
        assertThat(cookie(cookieValue)).isEqualTo(SESSION_KEY + "=\"" + cookieValue + "\";Path=/");
    }

    public String cookie(String cookieValue) {
        final Cookie cookie = new DefaultCookie(SESSION_KEY, cookieValue);
        cookie.setDiscard(false);
        cookie.setPath("/");
        final CookieEncoder encoder = new CookieEncoder(true);
        encoder.addCookie(cookie);
        return encoder.encode();

    }

}
