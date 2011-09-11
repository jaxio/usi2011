package usi2011.util;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.JSONValue;

import usi2011.exception.JsonException;

/**
 * Wrapper around JSON to wrap its checked exception into unchecked exception
 */
public final class Json {
    final private JSONObject json;

    public Json() {
        json = new JSONObject();
    }

    public Json(Object o) {
        json = new JSONObject(o);
    }

    public Json(String s) {
        assertJsonValidity(s);
        try {
            json = new JSONObject(s);
        } catch (JSONException e) {
            throw new JsonException(e);
        }
    }

    private void assertJsonValidity(String json) {
        if (json == null || json.indexOf('{') == -1 || json.indexOf('}') == -1) {
            throw new JsonException();
        }
    }

    public Json put(String key, String value) {
        try {
            json.put(key, value);
            return this;
        } catch (JSONException e) {
            throw new JsonException(e);
        }
    }

    public Json put(String key, boolean value) {
        try {
            json.put(key, value);
            return this;
        } catch (JSONException e) {
            throw new JsonException(e);
        }
    }

    public Json put(String key, int value) {
        try {
            json.put(key, value);
            return this;
        } catch (JSONException e) {
            throw new JsonException(e);
        }
    }

    public Json put(String key, long value) {
        try {
            json.put(key, value);
            return this;
        } catch (JSONException e) {
            throw new JsonException(e);
        }
    }

    public Json put(String key, int[] values) {
        for (int value : values) {
            put(key, value);
        }
        return this;
    }

    public String getString(String key) {
        try {
            return json.getString(key);
        } catch (JSONException e) {
            throw new JsonException(e);
        }
    }

    public int getInt(String key) {
        try {
            return json.getInt(key);
        } catch (JSONException e) {
            throw new JsonException(e);
        }
    }

    public boolean has(String key) {
        return json.has(key);
    }

    public String asJson() {
        return json.toString();
    }

    public String toString() {
        return asJson();
    }

    public static final String jsonEscape(final String value) {
        return JSONValue.escape(value);
    }

    @SuppressWarnings("unchecked")
    public static final <T> T jsonUnescape(final String value) {
        return (T) (value.indexOf('\\') == -1 ? value : JSONValue.parse('"' + value + '"'));
    }
}