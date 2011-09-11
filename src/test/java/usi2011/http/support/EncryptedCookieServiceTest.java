package usi2011.http.support;

import static org.fest.assertions.Assertions.assertThat;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.COOKIE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.NoSuchAlgorithmException;

import org.jboss.netty.handler.codec.http.HttpRequest;
import org.junit.Before;
import org.junit.Test;

import usi2011.exception.SessionKeyException;

public class EncryptedCookieServiceTest {

    CookieService cookieService;
    @Before
    public void setup() throws NoSuchAlgorithmException {
        cookieService = new CookieService(true);
    }

    @Test
    public void consecutiveCryptDoNotCreateSameHash() {
        assertThat(cookieService.set("something")).isNotEqualTo(cookieService.set("something"));
    }

    @Test
    public void uncrypt() {
        String crypted = cookieService.set("something");
        String uncrypted = cookieService.get(crypted);
        System.out.println(crypted);

        assertThat(uncrypted).isEqualTo("something");
    }
    
    @Test
    public void validCookie() {
        String crypted = cookieService.set("to be encoded");
        HttpRequest httpRequest = mock(HttpRequest.class);
        when(httpRequest.getHeader(COOKIE)).thenReturn("$Version=0;\r\nsession_key=\"" + crypted + "\"");
        assertThat(cookieService.get(httpRequest)).isEqualTo("to be encoded");
    }

    @Test(expected = SessionKeyException.class)
    public void temperedValue() {
        HttpRequest httpRequest = mock(HttpRequest.class);
        when(httpRequest.getHeader(COOKIE)) //
                .thenReturn("$Version=0;\r\nsession_key=\"CHANGED MANUALLY/xFfwU7i1/98f7795239d6c4ccc2e8b8822671ca7e/1302193145664\"");
        cookieService.get(httpRequest);
    }

    @Test(expected = SessionKeyException.class)
    public void temperedSaltValue() {
        HttpRequest httpRequest = mock(HttpRequest.class);
        when(httpRequest.getHeader(COOKIE)) //
                .thenReturn("$Version=0;\r\nsession_key=\"to be encoded/xxxxxxxx/0b87938bbe50d5b5075a1d29739e4be0/1302193386508\"");
        cookieService.get(httpRequest);
    }

    @Test(expected = SessionKeyException.class)
    public void temperedCryptValue() {
        HttpRequest httpRequest = mock(HttpRequest.class);
        when(httpRequest.getHeader(COOKIE)) //
                .thenReturn("$Version=0;\r\nsession_key=\"to be encoded/ZPmmeXSS/xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx/1302193386508\"");
        cookieService.get(httpRequest);
    }

    @Test(expected = SessionKeyException.class)
    public void temperedTimestampValue() {
        HttpRequest httpRequest = mock(HttpRequest.class);
        when(httpRequest.getHeader(COOKIE)) //
                .thenReturn("$Version=0;\r\nsession_key=\"to be encoded/ZPmmeXSS/0b87938bbe50d5b5075a1d29739e4be0/xxxxxxxxxxxxx\"");
        cookieService.get(httpRequest);
    }

    @Test(expected = NullPointerException.class)
    public void nullValueThrowsException() {
        cookieService.set(null);
    }

    @Test(expected = NullPointerException.class)
    public void uncryptNullStringThrowsException() {
        cookieService.get((String) null);
    }

    @Test(expected = NullPointerException.class)
    public void uncryptNullHttpRequestThrowsException() {
        cookieService.get((HttpRequest) null);
    }

    @Test(expected = SessionKeyException.class)
    public void noCookie() {
        HttpRequest httpRequest = mock(HttpRequest.class);
        when(httpRequest.getHeader(COOKIE)).thenReturn(null);
        cookieService.get(httpRequest);
    }

    @Test(expected = SessionKeyException.class)
    public void cookieTooShort() {
        HttpRequest httpRequest = mock(HttpRequest.class);
        when(httpRequest.getHeader(COOKIE)).thenReturn("a");
        cookieService.get(httpRequest);
    }
}