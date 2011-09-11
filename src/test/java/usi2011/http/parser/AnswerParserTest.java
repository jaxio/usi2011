package usi2011.http.parser;

import static org.fest.assertions.Assertions.assertThat;
import static usi2011.util.Specifications.ANSWER_ANSWER;
import static usi2011.util.Specifications.MAX_NUMBER_OF_ANSWERS;

import org.junit.Test;

import usi2011.exception.AnswerException;
import usi2011.exception.JsonException;
import usi2011.util.Json;

public class AnswerParserTest {
    @Test
    public void valid() {
        AnswerParser answerParser = new AnswerParser(new Json().put(ANSWER_ANSWER, 1));
        assertThat(answerParser.getUserAnswer()).isEqualTo(1);
    }

    @Test
    public void withString() {
        // golden path 1
        assertThat(new AnswerParser("{\"answer\":\"1\"}").getUserAnswer()).isEqualTo(1);
        assertThat(new AnswerParser("{\"answer\": \"1\"}").getUserAnswer()).isEqualTo(1);
        assertThat(new AnswerParser("{\"answer\":\"1\" }").getUserAnswer()).isEqualTo(1);
        assertThat(new AnswerParser("{\"answer\": \"1\" }").getUserAnswer()).isEqualTo(1);

        // golden path 2
        assertThat(new AnswerParser("{\"answer\":1}").getUserAnswer()).isEqualTo(1);
        assertThat(new AnswerParser("{\"answer\": 1}").getUserAnswer()).isEqualTo(1);
        assertThat(new AnswerParser("{\"answer\":1 }").getUserAnswer()).isEqualTo(1);
        assertThat(new AnswerParser("{\"answer\": 1 }").getUserAnswer()).isEqualTo(1);

        // not in golden
        assertThat(new AnswerParser(" {\"answer\":1}").getUserAnswer()).isEqualTo(1);
        assertThat(new AnswerParser("{ \"answer\":1}").getUserAnswer()).isEqualTo(1);
        assertThat(new AnswerParser("{\"answer\" :1}").getUserAnswer()).isEqualTo(1);
        assertThat(new AnswerParser(" {   \"answer\":1}").getUserAnswer()).isEqualTo(1);
    }

    @Test(expected = JsonException.class)
    public void invalidJson() {
        new AnswerParser("not json");
    }

    @Test(expected = AnswerException.class)
    public void missingAnswerThrowsException() {
        new AnswerParser(new Json().put("unknown", "not checked"));
    }

    @Test(expected = AnswerException.class)
    public void nonNumericValueThrowsException() {
        new AnswerParser(new Json().put(ANSWER_ANSWER, "invalid"));
    }

    @Test
    public void answersBoundaries() {
        new AnswerParser(new Json().put(ANSWER_ANSWER, 1));
        new AnswerParser(new Json().put(ANSWER_ANSWER, MAX_NUMBER_OF_ANSWERS));
    }

    @Test(expected = AnswerException.class)
    public void answerTooLowThrowsException() {
        new AnswerParser(new Json().put(ANSWER_ANSWER, 0));
    }

    @Test(expected = AnswerException.class)
    public void answerTooHighThrowsException() {
        new AnswerParser(new Json().put(ANSWER_ANSWER, MAX_NUMBER_OF_ANSWERS + 1));
    }
}