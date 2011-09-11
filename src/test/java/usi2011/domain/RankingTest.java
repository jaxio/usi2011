package usi2011.domain;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;

import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.Lists;

import usi2011.domain.Ranking.RankingBuilder;
import usi2011.domain.Ranking.UserScore;

@Ignore
public class RankingTest {

    @Test
    public void empty() {
        String expected = "{\"score\":\"0\"," //
                + "\"top_scores\":{\"mail\":null,\"scores\":null,\"firstname\":null,\"lastname\":null}," //
                + "\"before\":{\"mail\":null,\"scores\":null,\"firstname\":null,\"lastname\":null}," //
                + "\"after\":{\"mail\":null,\"scores\":null,\"firstname\":null,\"lastname\":null}}";

        assertThat(new RankingBuilder().build().getJson()).isEqualTo(expected);
    }

    @Test
    public void checkOrder() {
        String expected = "{\"score\":\"5\","//
                + "\"top_scores\":{\"mail\":[\"m1\",\"m2\"],\"scores\":[\"200\",\"100\"],\"firstname\":[\"f1\",\"f2\"],\"lastname\":[\"l1\",\"l2\"]}," //
                + "\"before\":{\"mail\":[\"m11\",\"m12\"],\"scores\":[\"20\",\"10\"],\"firstname\":[\"f11\",\"f12\"],\"lastname\":[\"l11\",\"l12\"]}," //
                + "\"after\":{\"mail\":[\"m21\",\"m22\"],\"scores\":[\"2\",\"1\"],\"firstname\":[\"f21\",\"f22\"],\"lastname\":[\"l21\",\"l22\"]}}";

        RankingBuilder builder = new RankingBuilder();
        builder.setScore("5");
        builder.addTop(newArrayList(new UserScore("200", "l1", "f1", "m1"), new UserScore("100", "l2", "f2", "m2")));

        builder.addBefore(new UserScore("20", "l11", "f11", "m11"));
        builder.addBefore(new UserScore("10", "l12", "f12", "m12"));

        builder.addAfter(new UserScore("2", "l21", "f21", "m21"));
        builder.addAfter(new UserScore("1", "l22", "f22", "m22"));

        assertThat(builder.build().getJson()).isEqualTo(expected);
    }

    @Test
    public void init() {
        RankingBuilder builder = new RankingBuilder();
        builder.setScore("500");
        for (int score = 1000; score > 990; score--) {
            builder.addTop(newArrayList(new UserScore("" + score, "lastName-" + score, "firstName-" + score, "user-with-score" + score + "@d.com")));
        }
        for (int score = 510; score > 500; score--) {
            builder.addBefore(new UserScore("" + score, "lastName-" + score, "firstName-" + score, "user-with-score" + score + "@d.com"));
        }
        for (int score = 499; score > 489; score--) {
            builder.addAfter(new UserScore("" + score, "lastName-" + score, "firstName-" + score, "user-with-score" + score + "@d.com"));
        }
        System.out.println(builder.build().getJson());
    }

    @Test
    public void transformScore() {
        String r = Ranking.transformScore("32");
        assertThat(r).isEqualTo("967");
        assertThat(Ranking.restoreScore(r)).isEqualTo("32");

        r = Ranking.transformScore("172");
        assertThat(r).isEqualTo("827");
        assertThat(Ranking.restoreScore(r)).isEqualTo("172");

        r = Ranking.transformScore("3");
        assertThat(r).isEqualTo("996");
        assertThat(Ranking.restoreScore(r)).isEqualTo("3");
    }
}