package usi2011.util;

import org.apache.commons.lang.math.NumberUtils;

/**
 * We are working mostly with numbers between 0 and 20, well, let's skip all the magic of Integer.parseInt for a simplified version when we can.
 * <p>
 * When we are not in the golden path, well, we leave java.lang do its magic
 */
public final class FastIntegerParser {

    public static final boolean isNumber(String value) {
        if (value == null) {
            return false;
        }
        final int length = value.length();
        switch (length) {
        case 1:
            return isDigit(value.charAt(0));
        case 2:
            if (isDigit(value.charAt(0)) && isDigit(value.charAt(1))) {
                return true;
            }
            return NumberUtils.isNumber(value);
        default:
            return NumberUtils.isNumber(value);
        }
    }

    public static final int parseInt(char c) {
        char decimal = c;
        if (!isDigit(decimal)) {
            throw new IllegalArgumentException(c + " is not a number");
        }
        return charToInt(decimal);
    }

    public static final int parseInt(String value) {
        if (value == null) {
            throw new NullPointerException();
        }
        final int length = value.length();
        switch (length) {
        case 0:
            throw new IllegalArgumentException("empty value");
        case 1:
            return parseInt(value.charAt(0));
        case 2:
            char tens = value.charAt(0);
            char decimals = value.charAt(1);
            if (isDigit(tens) && isDigit(decimals)) {
                return charToInt(tens) * 10 + charToInt(decimals);
            } else {
                // may be a negative sign, let Integer do its job
                return Integer.parseInt(value);
            }
        default:
            return Integer.parseInt(value);
        }
    }

    public static final long parseLong(String value) {
        return parseInt(value);
    }

    public static final boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    public static final int charToInt(char c) {
        return c - '0';
    }
}
