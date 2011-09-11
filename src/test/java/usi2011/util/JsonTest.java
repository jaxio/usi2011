package usi2011.util;

import static org.fest.assertions.Assertions.assertThat;
import static usi2011.util.Json.jsonEscape;
import static usi2011.util.Json.jsonUnescape;

import org.junit.Test;

import usi2011.exception.JsonException;

public class JsonTest {
    @Test
    public void simpleJsonPossible() {
        new Json("{}");
    }

    @Test
    public void simpleKeyValue() {
        Json json = new Json("{key:'value'}");
        assertThat(json.getString("key")).isEqualTo("value");
    }

    @Test
    public void simpleKeyValueNeedingQuotes() {
        Json json = new Json("{key:'value\\t\\u0080'}");
        assertThat(json.getString("key")).isEqualTo("value\t\u0080");
    }

    @Test
    public void multipleKeyValues() {
        Json json = new Json("{key1:'value1', key2:'value2'}");
        assertThat(json.getString("key1")).isEqualTo("value1");
        assertThat(json.getString("key2")).isEqualTo("value2");
    }

    @Test
    public void valueOnMultipleLines() {
        Json json = new Json("{key:'line1\\\n\\\nline2'}");
        assertThat(json.getString("key")).isEqualTo("line1\n\nline2");
    }
    
    @Test
    public void escape() {
        assertThat(jsonEscape("")).isEqualTo("");
        assertThat(jsonEscape("a")).isEqualTo("a");
        assertThat(jsonEscape("é")).isEqualTo("é");
        assertThat(jsonEscape("\u0080")).isEqualTo("\\u0080");
        assertThat(jsonEscape("\t")).isEqualTo("\\t");
        assertThat(jsonEscape("\\")).isEqualTo("\\\\");
        assertThat(jsonEscape("I \u2665 jaxio")).isEqualTo("I \u2665 jaxio");
    }

    @Test
    public void unescape() {
        assertThat(jsonUnescape("")).isEqualTo("");
        assertThat(jsonUnescape("a")).isEqualTo("a");
        assertThat(jsonUnescape("é")).isEqualTo("é");
        assertThat(jsonUnescape("\\u0080")).isEqualTo("\u0080");
        assertThat(jsonUnescape("\\t")).isEqualTo("\t");
        assertThat(jsonUnescape("\\\\")).isEqualTo("\\");
        assertThat(jsonUnescape("I \\u2665 jaxio")).isEqualTo("I \u2665 jaxio");
    }

    @Test(expected = JsonException.class)
    public void requestingUnknownKeyThrowsException() {
        new Json("{key:'value'}").getString("invalid key");
    }

    @Test(expected = JsonException.class)
    public void nullStringThrowsException() {
        new Json(null);
    }

    @Test(expected = JsonException.class)
    public void emptyStringThrowsException() {
        new Json("");
    }
}