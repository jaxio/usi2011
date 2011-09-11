package usi2011.util;

import static usi2011.util.Specifications.USER_INFO_MAX_LENGTH;
import static usi2011.util.Specifications.USER_INFO_MIN_LENGTH;

import java.util.regex.Pattern;

import org.apache.commons.validator.EmailValidator;

/**
 * This class add simple email validation to prevent {@link #emailValidator} to fire at least a perl regexp for errors that can be catched very early
 */
public final class FastEmailValidator {
    private final static EmailValidator emailValidator = EmailValidator.getInstance();
    private final static Pattern emailPattern = Pattern.compile("\\w*[.-_]?\\w*@\\w*.\\w{3}");

    public static boolean isEmail(final String email) {
        if (email == null || email.length() < USER_INFO_MIN_LENGTH || email.length() > USER_INFO_MAX_LENGTH) {
            return false;
        }
        return emailPattern.matcher(email).matches() || emailValidator.isValid(email);
    }
}