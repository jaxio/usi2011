package usi2011.http.parser;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang.builder.ToStringBuilder.reflectionToString;
import static org.apache.commons.lang.builder.ToStringStyle.MULTI_LINE_STYLE;
import static org.slf4j.LoggerFactory.getLogger;
import static usi2011.util.Json.jsonUnescape;
import static usi2011.util.Specifications.USER_EMAIL;
import static usi2011.util.Specifications.USER_INFO_MAX_LENGTH;
import static usi2011.util.Specifications.USER_INFO_MIN_LENGTH;
import static usi2011.util.Specifications.USER_PASSWORD;
import static usi2011.util.SplitUtil.split;

import org.slf4j.Logger;

import usi2011.exception.LoginException;
import usi2011.util.JSONBackedObject;
import usi2011.util.Json;

public final class LoginParser extends JSONBackedObject {
    private static final Logger logger = getLogger(LoginParser.class);
    private static final char GOLDEN_PATH_SPLIT_REGEX = '"';
    private static final int GOLDEN_PATH_SPLIT_SIZE = "{\"mail\":\"email@domain.com\",\"password\":\"password\"}".split("\"").length;
    private static final char SPLIT_0_CURLY_OPEN = '{';
    private static final char SPLIT_2_COLUMN = ':';
    private static final char SPLIT_4_COMA = ',';
    private static final char SPLIT_6_COLUMN = ':';
    private static final char SPLIT_8_CURLY_CLOSE = '}';

    private final String email;
    private final String password;

    public LoginParser(String json) {
//        checkNotNull(json);
//        checkArgument(!json.isEmpty());
//        final String[] values = split(json, GOLDEN_PATH_SPLIT_REGEX, GOLDEN_PATH_SPLIT_SIZE);
//        if (isGoldenPath(values)) {
//            email = jsonUnescape(values[3]);
//            password = jsonUnescape(values[7]);
//            if (password.length() < USER_INFO_MIN_LENGTH) {
//                throw new LoginException("password is too short");
//            } else if (password.length() > USER_INFO_MAX_LENGTH) {
//                throw new LoginException("password is too long");
//            }
//        } else {
//            logger.warn("Not in golden path");
//            final Json jsonObject = new Json(json);
//            email = getEmail(jsonObject, USER_EMAIL);
//            password = getAndAssertString(jsonObject, USER_PASSWORD);
//        }
        
        //methode HARDCORE => not approved by Florent :p
            StringBuilder buf = new StringBuilder(json);
            int cursor = 0;
            //skip 3 double quotes
            for (int i = 0; i < 3; i++) {
                while(buf.charAt(cursor) != '"')
                    cursor++;
                cursor++;
            }
           email = findNextValue(buf, cursor);
            //skip 4 double quotes
            for (int i = 0; i < 4; i++) {
                while(buf.charAt(cursor) != '"')
                    cursor++;
                cursor++;
            }
            password = findNextValue(buf, cursor);
        }


    private String findNextValue(StringBuilder buf, int cursor) {
        int begin = cursor;
        while(buf.charAt(cursor) != '"')
            cursor++;
        return buf.substring(begin, cursor);
        
    }

    public LoginParser(Json json) {
        checkNotNull(json);
        email = getEmail(json, USER_EMAIL);
        password = getAndAssertString(json, USER_PASSWORD);
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    static boolean isGoldenPath(final String[] split) {
        // need the correct length
        if (split.length != GOLDEN_PATH_SPLIT_SIZE) {
            return false;
        }
        // need to match the expected sizes
        if (split[0].length() != 1 || split[2].length() != 1 || split[4].length() != 1 || split[8].length() != 1) {
            return false;
        }
        // need to match the expected values
        if (split[0].charAt(0) != SPLIT_0_CURLY_OPEN || split[2].charAt(0) != SPLIT_2_COLUMN || split[4].charAt(0) != SPLIT_4_COMA
                || split[6].charAt(0) != SPLIT_6_COLUMN || split[8].charAt(0) != SPLIT_8_CURLY_CLOSE) {
            return false;
        }
        // need to match json in the right order and the rigth var names
        if (!split[1].equals(USER_EMAIL) || !split[5].equals(USER_PASSWORD)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return reflectionToString(this, MULTI_LINE_STYLE);
    }

    @Override
    public RuntimeException buildException(String message) {
        return new LoginException(message);
    }
}