package usi2011.http;

import static org.fest.assertions.Assertions.assertThat;
import static usi2011.http.StateMachineHttpRequestHandler.getNumericSuffix;

import org.junit.Test;

public class StateMachineHttpRequestHandlerTest {
    @Test
    public void t() {
        assertThat(getNumericSuffix("/api/audit", "/api/audit")).isEqualTo("");
        assertThat(getNumericSuffix("/api/audit/?", "/api/audit")).isEqualTo("");
        assertThat(getNumericSuffix("/api/audit/1", "/api/audit")).isEqualTo("1");
        assertThat(getNumericSuffix("/api/audit/1?user_mail=smith.white@hotmail.com&authentication_key=jaxio", "/api/audit")).isEqualTo("1");
    }
}
