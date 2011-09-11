package usi2011.domain;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang.builder.ToStringBuilder.reflectionToString;
import static org.apache.commons.lang.builder.ToStringStyle.MULTI_LINE_STYLE;
import static usi2011.domain.UserScoreHistory.UNANSWERED;
import static usi2011.util.Specifications.AUDIT_GOOD_ANSWERS;
import static usi2011.util.Specifications.AUDIT_USER_ANSWERS;
import static usi2011.util.Specifications.FIRST_QUESTION;
import static usi2011.util.Specifications.MAX_NUMBER_OF_QUESTIONS;

public final class AuditUser {
    private final int[] userAnswers;
    private final int[] goodAnswers;
    private final String json;

    public AuditUser(int[] userAnswers, int[] goodAnswers) {
        checkNotNull(userAnswers);
        checkNotNull(goodAnswers);
        checkArgument(userAnswers.length == MAX_NUMBER_OF_QUESTIONS + 1);
        checkArgument(goodAnswers.length == MAX_NUMBER_OF_QUESTIONS + 1);
        this.userAnswers = userAnswers;
        this.goodAnswers = goodAnswers;
        this.json = buildJson();
    }

    private String buildJson() {
        StringBuilder builder = new StringBuilder(256);
        builder.append('{');
        buildArray(builder, AUDIT_USER_ANSWERS, userAnswers);
        builder.append(',');
        buildArray(builder, AUDIT_GOOD_ANSWERS, goodAnswers);
        builder.append("}");
        return builder.toString();
    }

    private StringBuilder buildArray(StringBuilder builder, String name, int[] answers) {
        builder.append('"').append(name).append("\":");
        if (!hasAnswered(answers)) {
            builder.append("null");
            return builder;
        }
        builder.append('[');
        for (int i = FIRST_QUESTION; i <= MAX_NUMBER_OF_QUESTIONS; i++) {
            builder.append(i != FIRST_QUESTION ? ",\"" : '"').append(answers[i]).append("\"");
        }
        builder.append(']');
        return builder;
    }

    private boolean hasAnswered(int[] answers) {
        for (int i = FIRST_QUESTION; i <= MAX_NUMBER_OF_QUESTIONS; i++) {
            if (answers[i] != UNANSWERED) {
                return true;
            }
        }
        return false;

    }

    public String getJson() {
        return json;
    }

    public int[] getUserAnswers() {
        return userAnswers;
    }

    public int[] getGoodAnswers() {
        return goodAnswers;
    }

    @Override
    public String toString() {
        return reflectionToString(this, MULTI_LINE_STYLE);
    }
}