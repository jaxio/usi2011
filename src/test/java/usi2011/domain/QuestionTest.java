package usi2011.domain;

import static org.fest.assertions.Assertions.assertThat;
import static usi2011.util.Specifications.MAX_NUMBER_OF_ANSWERS;

import org.junit.Test;

import usi2011.domain.Question.QuestionBuilder;

public class QuestionTest {
    @Test
    public void build() {
        Question question = questionBuilder().build();

        assertThat(question.getQuestionId()).isEqualTo(1);
        assertThat(question.getLabel()).isEqualTo("label");
        assertThat(question.getLabelJsonEscaped()).isEqualTo("label");
        assertThat(question.getGoodChoice()).isEqualTo(3);
        assertThat(question.getChoice1()).isEqualTo("choice1");
        assertThat(question.getChoice2()).isEqualTo("choice2");
        assertThat(question.getChoice3()).isEqualTo("good answer");
        assertThat(question.getChoice4()).isEqualTo("choice4");
        assertThat(question.getGoodAnswer()).isEqualTo("good answer");
        assertThat(question.isRightAnswer(3)).isTrue();
        assertThat(question.isRightAnswer(1)).isFalse();
        assertThat(question.asJsonStringForScore(0)).isNotEmpty();
    }

    @Test
    public void buildNeedsEscape() {
        Question question = new Question.QuestionBuilder() //
                .setQuestionId(1) //
                .setLabel("label with euro sign {\u20AC}") //
                .setGoodChoice(1) //
                .setChoice1("good answer has a tab [\t]") //
                .setChoice2("choice2") //
                .setChoice3("choice3") //
                .setChoice4("choice4") //
                .build();

        assertThat(question.getQuestionId()).isEqualTo(1);
        assertThat(question.getLabel()).isEqualTo("label with euro sign {\u20AC}");
        assertThat(question.getLabelJsonEscaped()).isEqualTo("label with euro sign {\\u20AC}");
        assertThat(question.getGoodChoice()).isEqualTo(1);
        assertThat(question.getChoice1()).isEqualTo("good answer has a tab [\t]");
        assertThat(question.getChoice2()).isEqualTo("choice2");
        assertThat(question.getChoice3()).isEqualTo("choice3");
        assertThat(question.getChoice4()).isEqualTo("choice4");
        assertThat(question.getGoodAnswer()).isEqualTo("good answer has a tab [\t]");
        assertThat(question.getGoodAnswerJsonEscaped()).isEqualTo("good answer has a tab [\\t]");
        assertThat(question.isRightAnswer(1)).isTrue();
        assertThat(question.isRightAnswer(3)).isFalse();
        assertThat(question.asJsonStringForScore(0)).isNotEmpty();
    }

    @Test
    public void orderIsOk() {
        Question question = questionBuilder().build();
        String actual = question.asJsonStringForScore(10);
        String expected = "{\"question\":\"label\",\"answer_1\":\"choice1\",\"answer_2\":\"choice2\",\"answer_3\":\"good answer\",\"answer_4\":\"choice4\",\"score\":\"10\"}";
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void goodChoiceBoundaries() {
        questionBuilder().setGoodChoice(1).build();
        questionBuilder().setGoodChoice(MAX_NUMBER_OF_ANSWERS).build();
    }

    @Test(expected = IllegalStateException.class)
    public void tooLowGoodChoiceThrowsException() {
        questionBuilder().setGoodChoice(0).build();
    }

    @Test(expected = IllegalStateException.class)
    public void tooHighGoodChoiceThrowsException() {
        questionBuilder().setGoodChoice(MAX_NUMBER_OF_ANSWERS + 1).build();
    }

    @Test(expected = NullPointerException.class)
    public void missingQuestionIdThrowsException() {
        questionBuilder().setQuestionId(null).build();
    }

    @Test(expected = NullPointerException.class)
    public void missingLabelThrowsException() {
        questionBuilder().setLabel(null).build();
    }

    @Test(expected = NullPointerException.class)
    public void missingChoice1ThrowsException() {
        questionBuilder().setChoice1(null).build();
    }

    @Test(expected = NullPointerException.class)
    public void missingChoice2ThrowsException() {
        questionBuilder().setChoice2(null).build();
    }

    @Test(expected = NullPointerException.class)
    public void missingChoice3ThrowsException() {
        questionBuilder().setChoice3(null).build();
    }

    @Test(expected = NullPointerException.class)
    public void missingChoice4ThrowsException() {
        questionBuilder().setChoice4(null).build();
    }

    @Test(expected = NullPointerException.class)
    public void missingGoodAnswerThrowsException() {
        questionBuilder().setGoodChoice(null).build();
    }

    private QuestionBuilder questionBuilder() {
        return new Question.QuestionBuilder() //
                .setQuestionId(1) //
                .setLabel("label") //
                .setGoodChoice(3) //
                .setChoice1("choice1") //
                .setChoice2("choice2") //
                .setChoice3("good answer") //
                .setChoice4("choice4");
    }
}