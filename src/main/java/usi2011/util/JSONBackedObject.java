package usi2011.util;

import static org.apache.commons.lang.StringUtils.trimToNull;
import static usi2011.util.FastEmailValidator.isEmail;
import static usi2011.util.Specifications.AUTHENTICATION_KEY;
import static usi2011.util.Specifications.AUTHENTICATION_SECRET_KEY;
import static usi2011.util.Specifications.USER_INFO_MAX_LENGTH;
import static usi2011.util.Specifications.USER_INFO_MIN_LENGTH;
import usi2011.exception.AuthenticationKeyException;

/**
 * Helper class to work with json commands
 */
public abstract class JSONBackedObject {

    abstract protected RuntimeException buildException(String message);

    protected String getString(final Json json, final String key) {
        if (!json.has(key)) {
            throw buildException("Input " + key + " is missing");
        }
        return json.getString(key);
    }

    protected String getAndAssertString(final Json json, final String key) {
        return assertLength(key, getString(json, key));
    }

    protected String assertLength(final String key, final String value) {
        String trimed = trimToNull(value);
        if (trimed == null) {
            throw buildException(key + " is empty");
        } else if (trimed.length() < USER_INFO_MIN_LENGTH) {
            throw buildException(key + " is too short");
        } else if (trimed.length() > USER_INFO_MAX_LENGTH) {
            throw buildException(key + " is too long");
        }
        return trimed;
    }

    protected int getInt(final Json json, final String key) {
        if (!json.has(key)) {
            throw buildException("Numeric input " + key + " is missing");
        }
        try {
            return json.getInt(key);
        } catch (Exception e) {
            throw buildException(json.getString(key) + " is not a valid int");
        }
    }

    protected String getEmail(final Json json, final String key) {
        return assertEmail(getAndAssertString(json, key));
    }

    protected String assertEmail(final String email) {
        if (!isEmail(email)) {
            throw buildException(email + " is not a valid email");
        }
        return email;
    }

    protected void assertAuthenticationKey(final Json json) {
        if (!json.has(AUTHENTICATION_KEY)) {
            throw new AuthenticationKeyException("Authentication key is missing");
        }
        final String authenticationKey = json.getString(AUTHENTICATION_KEY);
        if (!AUTHENTICATION_SECRET_KEY.equals(authenticationKey)) {
            throw new AuthenticationKeyException("Wrong authentication key");
        }
    }
}