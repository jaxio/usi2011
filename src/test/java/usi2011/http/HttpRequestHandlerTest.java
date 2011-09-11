package usi2011.http;

import static com.jayway.restassured.RestAssured.given;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.OK;
import static usi2011.Main.context;
import static usi2011.util.ResourceUtil.asJsonString;
import static usi2011.util.Specifications.AUTHENTICATION_KEY;
import static usi2011.util.Specifications.AUTHENTICATION_SECRET_KEY;
import static usi2011.util.Specifications.PARAMETERS;
import static usi2011.util.Specifications.USER_EMAIL;
import static usi2011.util.Specifications.USER_FIRSTNAME;
import static usi2011.util.Specifications.USER_LASTNAME;
import static usi2011.util.Specifications.USER_PASSWORD;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import usi2011.Main;
import usi2011.util.Json;

import com.jayway.restassured.specification.RequestSpecification;

@Ignore
public class HttpRequestHandlerTest {
    static RequestSpecification given = given();

    static final String EMAIL = "florent@ramiere.com";
    static final String FIRSTNAME = "florent";
    static final String LASTNAME = "ramiere";
    static final String PASSWORD = "jaxio";

    static int localPort;

    @BeforeClass
    public static void runServer() throws IOException {
        new Main();
        localPort = context.getBean(HttpServer.class).getListeningOnPort();
    }

    private String url(String url) {
        return "http://localhost:" + localPort + url;
    }

    @Test
    public void reset() {
        given.body(new Json() //
                .put(AUTHENTICATION_KEY, AUTHENTICATION_SECRET_KEY) //
                .asJson()) //
                .then() //
                .statusCode(OK.getCode()) //
                .post(url("/api/reset"));
    }

    @Test
    public void game() throws IOException {
        given.body(new Json() //
                .put(AUTHENTICATION_KEY, AUTHENTICATION_SECRET_KEY) //
                .put(PARAMETERS, asJsonString("gamesession-20-questions.xml")) //
                .asJson()) //
                .then() //
                .statusCode(CREATED.getCode()) //
                .post(url("/api/game"));
    }

    @Test
    public void create() {
        given.body(new Json() //
                .put(USER_EMAIL, EMAIL) //
                .put(USER_FIRSTNAME, FIRSTNAME) //
                .put(USER_LASTNAME, LASTNAME) //
                .put(USER_PASSWORD, PASSWORD) //
                .asJson()) //
                .then() //
                .statusCode(CREATED.getCode()) //
                .post(url("/api/user"));
    }

    @Test
    public void login() {
        given.body(new Json() //
                .put(USER_EMAIL, EMAIL) //
                .put(USER_PASSWORD, PASSWORD) //
                .asJson()) //
                .then() //
                .statusCode(OK.getCode()) //
                .post(url("/api/login"));
    }
}
