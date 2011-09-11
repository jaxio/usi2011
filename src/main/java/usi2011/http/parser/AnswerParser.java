package usi2011.http.parser;

import static org.apache.commons.lang.builder.ToStringBuilder.reflectionToString;
import static org.apache.commons.lang.builder.ToStringStyle.MULTI_LINE_STYLE;
import static org.slf4j.LoggerFactory.getLogger;
import static usi2011.util.Specifications.ANSWER_ANSWER;
import static usi2011.util.Specifications.MAX_NUMBER_OF_ANSWERS;

import java.util.Arrays;

import org.slf4j.Logger;

import usi2011.exception.AnswerException;
import usi2011.util.JSONBackedObject;
import usi2011.util.Json;

public final class AnswerParser extends JSONBackedObject {
    private static final Logger logger = getLogger(AnswerParser.class);
    private final int userAnswer;
    private final static String WELL_KNOWN_JSON = "{\"" + ANSWER_ANSWER + "\":";
    private final static byte[] WELL_KNOWN_JSON_ANSWER_1_UNQUOTED = (WELL_KNOWN_JSON + "1}").getBytes();
    private final static byte[] WELL_KNOWN_JSON_ANSWER_2_UNQUOTED = (WELL_KNOWN_JSON + "2}").getBytes();
    private final static byte[] WELL_KNOWN_JSON_ANSWER_3_UNQUOTED = (WELL_KNOWN_JSON + "3}").getBytes();
    private final static byte[] WELL_KNOWN_JSON_ANSWER_4_UNQUOTED = (WELL_KNOWN_JSON + "4}").getBytes();
    private final static byte[] WELL_KNOWN_JSON_ANSWER_1_QUOTED = (WELL_KNOWN_JSON + "\"1\"}").getBytes();
    private final static byte[] WELL_KNOWN_JSON_ANSWER_2_QUOTED = (WELL_KNOWN_JSON + "\"2\"}").getBytes();
    private final static byte[] WELL_KNOWN_JSON_ANSWER_3_QUOTED = (WELL_KNOWN_JSON + "\"3\"}").getBytes();
    private final static byte[] WELL_KNOWN_JSON_ANSWER_4_QUOTED = (WELL_KNOWN_JSON + "\"4\"}").getBytes();

    public AnswerParser(String json) {
        this(json.getBytes());
    }

    /**
     * We handle the following special case to avoid relying on json <code>{"answer":\"X\"}</code> Whenever we are not in this case, we fallback to the real
     * json library
     */
    public AnswerParser(byte[] jsonAsBytes) {
        final int length = jsonAsBytes.length;
        if (length == WELL_KNOWN_JSON_ANSWER_1_UNQUOTED.length) {
            if (Arrays.equals(WELL_KNOWN_JSON_ANSWER_1_UNQUOTED, jsonAsBytes)) {
                this.userAnswer = 1;
            } else if (Arrays.equals(WELL_KNOWN_JSON_ANSWER_2_UNQUOTED, jsonAsBytes)) {
                this.userAnswer = 2;
            } else if (Arrays.equals(WELL_KNOWN_JSON_ANSWER_3_UNQUOTED, jsonAsBytes)) {
                this.userAnswer = 3;
            } else if (Arrays.equals(WELL_KNOWN_JSON_ANSWER_4_UNQUOTED, jsonAsBytes)) {
                this.userAnswer = 4;
            } else {
                logger.warn("Not in unquoted golden path!");
                // ok well, we are not in the golden path, let json do its magic
                this.userAnswer = getInt(new Json(new String(jsonAsBytes)), ANSWER_ANSWER);
            }
        } else if (length == WELL_KNOWN_JSON_ANSWER_1_QUOTED.length) {
            if (Arrays.equals(WELL_KNOWN_JSON_ANSWER_1_QUOTED, jsonAsBytes)) {
                this.userAnswer = 1;
            } else if (Arrays.equals(WELL_KNOWN_JSON_ANSWER_2_QUOTED, jsonAsBytes)) {
                this.userAnswer = 2;
            } else if (Arrays.equals(WELL_KNOWN_JSON_ANSWER_3_QUOTED, jsonAsBytes)) {
                this.userAnswer = 3;
            } else if (Arrays.equals(WELL_KNOWN_JSON_ANSWER_4_QUOTED, jsonAsBytes)) {
                this.userAnswer = 4;
            } else {
                logger.warn("Not in quoted golden path !");
                // ok well, we are not in the golden path, let json do its magic
                this.userAnswer = getInt(new Json(new String(jsonAsBytes)), ANSWER_ANSWER);
            }
        } else {
            logger.warn("Absolutely not in golden path !");
            // ok well, we are not in the golden path, let json do its magic
            this.userAnswer = getInt(new Json(new String(jsonAsBytes)), ANSWER_ANSWER);
        }
        checkUserAnswer(userAnswer);
    }

    public AnswerParser(Json json) {
        this.userAnswer = getInt(json, ANSWER_ANSWER);
        checkUserAnswer(userAnswer);
    }

    private void checkUserAnswer(int userAnswer) {
        if (userAnswer < 1) {
            throw buildException("is too low, it should be between 1 and " + MAX_NUMBER_OF_ANSWERS);
        } else if (userAnswer > MAX_NUMBER_OF_ANSWERS) {
            throw buildException("is too high, it should be between 1 and " + MAX_NUMBER_OF_ANSWERS);
        }
    }

    public int getUserAnswer() {
        return userAnswer;
    }

    @Override
    public String toString() {
        return reflectionToString(this, MULTI_LINE_STYLE);
    }

    @Override
    final public RuntimeException buildException(String message) {
        return new AnswerException(message);
    }
}