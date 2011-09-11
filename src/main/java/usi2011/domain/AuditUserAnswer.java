package usi2011.domain;

import static org.apache.commons.lang.builder.ToStringBuilder.reflectionToString;
import static org.apache.commons.lang.builder.ToStringStyle.MULTI_LINE_STYLE;
import static usi2011.util.Specifications.AUDIT_GOOD_ANSWER;
import static usi2011.util.Specifications.AUDIT_QUESTION;
import static usi2011.util.Specifications.AUDIT_USER_ANSWER;

public final class AuditUserAnswer {
    private final int userAnswer;
    private final int goodAnswer;
    private final String labelJsonEscaped;

    public AuditUserAnswer(int userAnswer, int goodAnswer, String labelJsonEscaped) {
        this.userAnswer = userAnswer;
        this.goodAnswer = goodAnswer;
        this.labelJsonEscaped = labelJsonEscaped;
    }

    public int getUserAnswer() {
        return userAnswer;
    }

    public int getGoodAnswer() {
        return goodAnswer;
    }

    public String getLabelJsonEscaped() {
        return labelJsonEscaped;
    }

    public String toJsonString() {
        StringBuilder builder = new StringBuilder(256);
        builder.append("{\"").append(AUDIT_USER_ANSWER).append("\":").append(userAnswer);
        builder.append(",\"").append(AUDIT_GOOD_ANSWER).append("\":").append(goodAnswer);
        builder.append(",\"").append(AUDIT_QUESTION).append("\":\"").append(labelJsonEscaped);
        builder.append("\"}");
        return builder.toString();
    }

    @Override
    public String toString() {
        return reflectionToString(this, MULTI_LINE_STYLE);
    }
}