package usi2011.domain;

import static org.apache.commons.lang.builder.ToStringBuilder.reflectionToString;
import static org.apache.commons.lang.builder.ToStringStyle.MULTI_LINE_STYLE;
import static usi2011.util.Specifications.ANSWER_ARE_YOU_RIGHT;
import static usi2011.util.Specifications.ANSWER_GOOD_ANSWER;
import static usi2011.util.Specifications.ANSWER_SCORE;

public final class Answer {
    // prevent concatenation during runtime
    private static final String JSON_PART_1 = "{\"" + ANSWER_ARE_YOU_RIGHT + "\":\"";
    private static final String JSON_PART_2 = "\",\"" + ANSWER_GOOD_ANSWER + "\":\"";
    private static final String JSON_PART_3 = "\",\"" + ANSWER_SCORE + "\":\"";
    private static final String JSON_PART_4 = "\"}";

    private final String goodAnswerJsonEscaped;
    private final boolean areYouRight;
    private final long score;

    public Answer(String goodAnswerJsonEscaped, boolean areYouRight, long score) {
        this.goodAnswerJsonEscaped = goodAnswerJsonEscaped;
        this.areYouRight = areYouRight;
        this.score = score;
    }

    public String toJsonString() {
        StringBuilder builder = new StringBuilder(256);
        builder.append(JSON_PART_1).append(areYouRight);
        builder.append(JSON_PART_2).append(goodAnswerJsonEscaped);
        builder.append(JSON_PART_3).append(score);
        builder.append(JSON_PART_4);
        return builder.toString();
    }

    public long getScore() {
        return score;
    }

    public boolean isCorrect() {
        return areYouRight;
    }

    @Override
    public String toString() {
        return reflectionToString(this, MULTI_LINE_STYLE);
    }
    
    public final static String toJSONString(String goodAnswerJsonEscaped, boolean areYouRight, long score) {
        StringBuilder builder = new StringBuilder(256);
        builder.append(JSON_PART_1).append(areYouRight);
        builder.append(JSON_PART_2).append(goodAnswerJsonEscaped);
        builder.append(JSON_PART_3).append(score);
        builder.append(JSON_PART_4);
        return builder.toString();        
    }
}