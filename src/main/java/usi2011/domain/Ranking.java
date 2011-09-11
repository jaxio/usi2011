package usi2011.domain;

import static com.google.common.collect.Lists.newArrayList;
import static usi2011.util.Specifications.SCORE_AFTER_SCORE;
import static usi2011.util.Specifications.SCORE_BEFORE_SCORE;
import static usi2011.util.Specifications.SCORE_EMAIL;
import static usi2011.util.Specifications.SCORE_FIRST_NAME;
import static usi2011.util.Specifications.SCORE_LAST_NAME;
import static usi2011.util.Specifications.SCORE_SCORE;
import static usi2011.util.Specifications.SCORE_SCORES;
import static usi2011.util.Specifications.SCORE_TOP_SCORE;
import static usi2011.util.SplitUtil.split;

import java.util.List;

public final class Ranking {
    private final String score;
    private final String json;

    public Ranking(final String score, final String topJson, final List<UserScore> before, final List<UserScore> after) {
        this.score = score;
        this.json = buildJson(topJson, before, after);
    }

    public Ranking() {
        this.score = "0";
        this.json = buildJson(null, null, null);
    }

    private String buildJson(final String topJson, final List<UserScore> before, final List<UserScore> after) {
        final StringBuilder builder = new StringBuilder(1024);
        builder.append("{\"").append(SCORE_SCORE).append("\":\"").append(score);
        builder.append("\",\"").append(SCORE_TOP_SCORE).append("\":{");
        builder.append(topJson);
        builder.append("},\"").append(SCORE_BEFORE_SCORE).append("\":{");
        buildArray(builder, before);
        builder.append("},\"").append(SCORE_AFTER_SCORE).append("\":{");
        buildArray(builder, after);
        builder.append("}}");
        return builder.toString();
    }

    // precompute empty array
    private static final String EMPTY_ARRAY = "\"" + SCORE_EMAIL + "\":null,\"" + SCORE_SCORES + "\":null,\"" + SCORE_FIRST_NAME + "\":null,\""
            + SCORE_LAST_NAME + "\":null";

    // precompute part of the json to avoir append at runtime
    private static final String ARRAY_EMAIL = "\"" + SCORE_EMAIL + "\":[\"";
    private static final String ARRAY_SCORES = "\"],\"" + SCORE_SCORES + "\":[\"";
    private static final String ARRAY_FIRST_NAME = "\"],\"" + SCORE_FIRST_NAME + "\":[\"";
    private static final String ARRAY_LAST_NAME = "\"],\"" + SCORE_LAST_NAME + "\":[\"";
    private static final String ARRAY_FIRST_ELEMENT = "";
    private static final String ARRAY_ELEMENT = "\",\"";
    private static final String ARRAY_LAST_ELEMENT = "\"";

    public static final String element(final int i, final int length) {
        if (i == 0) {
            return ARRAY_FIRST_ELEMENT;
        } else if (i == length + 1) {
            return ARRAY_LAST_ELEMENT;
        } else {
            return ARRAY_ELEMENT;
        }
    }

    protected static void buildArray(final StringBuilder builder, final List<UserScore> data) {
        if (data.isEmpty()) {
            builder.append(EMPTY_ARRAY);
            return;
        }
        final int length = data.size();
        builder.append(ARRAY_EMAIL);
        for (int i = 0; i < length; i++) {
            builder.append(element(i, length)).append(data.get(i).getEmail());
        }
        builder.append(ARRAY_SCORES);
        for (int i = 0; i < length; i++) {
            builder.append(element(i, length)).append(data.get(i).getScoreStr());
        }
        builder.append(ARRAY_FIRST_NAME);
        for (int i = 0; i < length; i++) {
            builder.append(element(i, length)).append(data.get(i).getFirstName());
        }
        builder.append(ARRAY_LAST_NAME);
        for (int i = 0; i < length; i++) {
            builder.append(element(i, length)).append(data.get(i).getLastName());
        }
        builder.append("\"]");
    }

    public String getJson() {
        return json;
    }

    public static final class UserScore {
        private final String email;
        private final String firstName;
        private final String lastName;
        private String scoreStr;

        public UserScore(String scoreStr, String lastName, String firstName, String email) {
            this.scoreStr = scoreStr;
            this.lastName = lastName;
            this.firstName = firstName;
            this.email = email;
        }

        public UserScore(int score, String lastName, String firstName, String email) {
            this("" + score, lastName, firstName, email);
        }

        public String getEmail() {
            return email;
        }

        public String getFirstName() {
            return firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public String getScoreStr() {
            return scoreStr;
        }

        public final String toColumnNameString() {
            // the order is important for comparison as when 2 scores are equals, we sort
            // alphabetically (ASC) the lastname/firstname...
            // But beware! if you impose ASC order for the lastname/firstname... you can only do cassandra query
            // that will return score in ASC order... This is a show stopper for the TOP N scores as we do not know who is the 100tieth ranked user!
            // Hopefully, we have a magic trick... we perform a transformation on the score to get the sorting all right...

            return transformScore(scoreStr) + ":" + getLastName() + ":" + getFirstName() + ":" + getEmail();
        }

        static public UserScore fromColumnNameString(String s) {
            final String[] values = split(s, ':', 4);
            final String scoreStr = restoreScore(values[0]);
            final String lastName = values[1];
            final String firstName = values[2];
            final String email = values[3];
            return new UserScore(scoreStr, lastName, firstName, email);
        }
    }

    private static char ZERO_PLUS_9 = '0' + '9';

    /**
     * pad and 'inverse' the score.
     * 
     * <pre>
     * Example: "8" ==> "991" 
     * Example: "0" ==> "999"
     * Example: "999" ==> "000" 
     * Example: "34" ==> "965"
     * </pre>
     */
    public static String transformScore(String score) {
        final char[] result = new char[3];
        final int scoreLength = score.length();

        if (scoreLength == 3) {
            result[0] = (char) (ZERO_PLUS_9 - score.charAt(0));
            result[1] = (char) (ZERO_PLUS_9 - score.charAt(1));
            result[2] = (char) (ZERO_PLUS_9 - score.charAt(2));
            return new String(result);
        }

        if (scoreLength == 2) {
            result[0] = '9';
            result[1] = (char) (ZERO_PLUS_9 - score.charAt(0));
            result[2] = (char) (ZERO_PLUS_9 - score.charAt(1));
            return new String(result);
        }

        if (scoreLength == 1) {
            result[0] = '9';
            result[1] = '9';
            result[2] = (char) (ZERO_PLUS_9 - score.charAt(0));
            return new String(result);
        }

        throw new IllegalStateException("expecting a score between 0 & 999");
    }

    /**
     * Restore the padded inversed score to a regular score as string (not padded)
     * 
     * <pre>
     * Example: "991" ==> "8" 
     * Example: "999" ==> "0" 
     * Example: "000" ==> "999"
     * Example: "965" ==> "34"
     * </code>
     */
    public static String restoreScore(String score) {
        if (score.charAt(0) == '9') {
            if (score.charAt(1) == '9') {
                return new String(new char[] { (char) (ZERO_PLUS_9 - score.charAt(2)) });
            }

            final char[] result = new char[2];
            result[0] = (char) (ZERO_PLUS_9 - score.charAt(1));
            result[1] = (char) (ZERO_PLUS_9 - score.charAt(2));
            return new String(result);
        }

        final char[] result = new char[3];
        result[0] = (char) (ZERO_PLUS_9 - score.charAt(0));
        result[1] = (char) (ZERO_PLUS_9 - score.charAt(1));
        result[2] = (char) (ZERO_PLUS_9 - score.charAt(2));
        return new String(result);
    }

    public static final class RankingBuilder {
        private static String lastTopScoreStr = "";
        private static List<UserScore> lastTopScore = null;

        private String score = "0";
        private final List<UserScore> top = newArrayList();
        private final List<UserScore> before = newArrayList();
        private final List<UserScore> after = newArrayList();

        public RankingBuilder setScore(String score) {
            this.score = score;
            return this;
        }

        public RankingBuilder addTop(List<UserScore> top) {
            // possible race condition, we do not need to synchronize, at most there will be a few extra buildArray
            if (lastTopScore != top) {
                StringBuilder cache = new StringBuilder(2000);
                Ranking.buildArray(cache, top);
                lastTopScore = top;
                lastTopScoreStr = cache.toString();
            }
            return this;
        }

        public RankingBuilder addBefore(UserScore score) {
            before.add(score);
            return this;
        }

        public RankingBuilder addAfter(UserScore score) {
            after.add(score);
            return this;
        }

        public Ranking build() {
            return new Ranking(score, lastTopScoreStr, before, after);
        }
    }
}