package usi2011.repository;

import static org.fest.assertions.Assertions.assertThat;
import static usi2011.Main.context;
import static usi2011.util.Specifications.FIRST_QUESTION;
import static usi2011.util.Specifications.MAX_NUMBER_OF_ANSWERS;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.BeansException;
import org.springframework.core.io.ClassPathResource;

import usi2011.domain.Question;
import usi2011.domain.Session;

@Ignore
public class QuestionRepositoryTest {

    static QuestionRepository repository = null;

    @BeforeClass
    public static void init() throws BeansException, IOException {
        repository = context.getBean(QuestionRepository.class);
    }

    @Test
    public void reset() {
        repository.reset();
        assertThat(repository.getQuestion(FIRST_QUESTION)).isNull();
        assertThat(repository.getQuestion(MAX_NUMBER_OF_ANSWERS)).isNull();
    }

    @Test
    public void saveSession() {
        repository.save(session());

        // check first question
        Question firstQuestion = repository.getQuestion(FIRST_QUESTION);
        assertThat(firstQuestion).isNotNull();
        assertThat(firstQuestion.getQuestionId()).isEqualTo(1);

        // last one
        Question maxQuestion = repository.getQuestion(MAX_NUMBER_OF_ANSWERS);
        assertThat(maxQuestion).isNotNull();
        assertThat(maxQuestion.getQuestionId()).isEqualTo(MAX_NUMBER_OF_ANSWERS);

        repository.reset();

        assertThat(repository.getQuestion(FIRST_QUESTION)).isNull();
        assertThat(repository.getQuestion(MAX_NUMBER_OF_ANSWERS)).isNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void outOfBoundariesQuestionsThrowsException() {
        // out of boundaries
        assertThat(repository.getQuestion(FIRST_QUESTION - 1)).isNull();
        assertThat(repository.getQuestion(MAX_NUMBER_OF_ANSWERS + 1)).isNull();
    }

    private Session session() {
        return new Session(new ClassPathResource("/session/gamesession-20-questions.xml"));
    }
}
