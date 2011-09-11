package usi2011.repository;

import static com.google.common.collect.Iterables.get;
import static org.fest.assertions.Assertions.assertThat;
import static usi2011.Main.context;
import static usi2011.domain.UserScoreHistory.UNANSWERED;
import static usi2011.util.Specifications.FIRST_QUESTION;
import static usi2011.util.Specifications.MAX_NUMBER_OF_QUESTIONS;

import java.io.IOException;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.BeansException;

import usi2011.domain.AuditUser;
import usi2011.domain.AuditUserAnswer;
import usi2011.domain.Question;
import usi2011.domain.Question.QuestionBuilder;
import usi2011.domain.Ranking.UserScore;
import usi2011.domain.User;
import usi2011.domain.UserScoreHistory;

import com.google.common.collect.Iterables;

@Ignore
public class ScoreRepositoryTest {
    private static ScoreRepository repository = null;
    private static final int[] goodAnswers = new int[] { UNANSWERED, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4 }; // MAX_NUMBER_OF_ANSWERS answers...

    @BeforeClass
    public static void init() throws BeansException, IOException {
        repository = context.getBean(ScoreRepository.class);
        repository.reset();
    }

    @Test
    public void reset() {
        repository.reset();
        assertThat(repository.getWeakerThanUser(new UserScore("1", "ln", "fn", "email@domain.com"), 1)).isEmpty();
        assertThat(repository.getStrongerThanUser(new UserScore("1", "ln", "fn", "email@domain.com"), 1)).isEmpty();
        assertThat(repository.getTop(1)).isEmpty();
    }

    @Test
    public void getScore() {
        User user = new User("email@domain.com", "dummy", "dummy", "dummy");
        Question[] questions = new Question[21];
        for (int i = FIRST_QUESTION; i <= MAX_NUMBER_OF_QUESTIONS; i++) {
            questions[i] = question(i);
        }
        UserScoreHistory userHistory = new UserScoreHistory("email@domain.com", "ln", "fn", goodAnswers);

        // got it right
        userHistory.answer(questions[1], goodAnswers[1]); // GOOD
        repository.updateScore(userHistory, questions[1]);

        assertThat(userHistory.getScore(1)).isEqualTo(1);
        assertThat(repository.getScore(user, questions, goodAnswers)).isEqualTo(1);

        // got it right a second time
        userHistory.answer(questions[2], goodAnswers[2]); // GOOD
        repository.updateScore(userHistory, questions[2]);

        assertThat(userHistory.getScore(2)).isEqualTo(3);
        assertThat(repository.getScore(user, questions, goodAnswers)).isEqualTo(3);
    }

    @Test
    public void audit() {
        Question question = question(1);
        UserScoreHistory userHistory = new UserScoreHistory("email@domain.com", "ln", "fn", goodAnswers);

        // got it right
        userHistory.answer(question, goodAnswers[1]);
        repository.updateScore(userHistory, question);
        AuditUserAnswer auditUser = repository.getAudit("email@domain.com", question(1));

        assertThat(auditUser.getGoodAnswer()).isEqualTo(question.getGoodChoice());
        assertThat(auditUser.getUserAnswer()).isEqualTo(goodAnswers[1]);

        // got it wrong
        question = question(2);
        userHistory.answer(question, goodAnswers[1]); // WRONG
        repository.updateScore(userHistory, question);
        auditUser = repository.getAudit("email@domain.com", question(2));

        assertThat(auditUser.getGoodAnswer()).isEqualTo(question.getGoodChoice());
        assertThat(auditUser.getLabelJsonEscaped()).isEqualTo(question.getLabelJsonEscaped());
        assertThat(auditUser.getUserAnswer()).isEqualTo(goodAnswers[1]);

        // got it right
        question = question(3);
        userHistory.answer(question, goodAnswers[3]); // GOOD
        repository.updateScore(userHistory, question);
        auditUser = repository.getAudit("email@domain.com", question(3));

        assertThat(auditUser.getGoodAnswer()).isEqualTo(question.getGoodChoice());
        assertThat(auditUser.getLabelJsonEscaped()).isEqualTo(question.getLabelJsonEscaped());
        assertThat(auditUser.getUserAnswer()).isEqualTo(goodAnswers[3]);

        AuditUser audit = repository.getAudit("email@domain.com", new int[MAX_NUMBER_OF_QUESTIONS + 1]);
        assertThat(audit).isNotNull();

        assertThat(audit.getUserAnswers()).hasSize(MAX_NUMBER_OF_QUESTIONS + 1);
        assertThat(audit.getUserAnswers()[1]).isEqualTo(goodAnswers[1]);
        assertThat(audit.getUserAnswers()[2]).isEqualTo(goodAnswers[1]);
        assertThat(audit.getUserAnswers()[3]).isEqualTo(goodAnswers[3]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void auditQuestionForUnknownUserThrowsException() {
        repository.reset();
        repository.getAudit("email@domain.com", question(1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void auditUnknownUserThrowsException() {
        repository.reset();
        repository.getAudit("email@domain.com", new int[MAX_NUMBER_OF_QUESTIONS + 1]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void weakerInvalidSizeThrowsException() {
        repository.reset();
        repository.getWeakerThanUser(getDummyUserScore(1), 0);
    }

//    @Test(expected = IllegalArgumentException.class)
//    public void weakerInvalidScoreThrowsException() {
//        repository.reset();
//        repository.getWeakerThanUser(getDummyUserScore(-1), 1);
//    }

    @Test(expected = IllegalArgumentException.class)
    public void strongerInvalidSizeThrowsException() {
        repository.reset();
        repository.getStrongerThanUser(getDummyUserScore(1), 0);
    }

//    @Test(expected = IllegalArgumentException.class)
//    public void strongerInvalidScoreThrowsException() {
//        repository.reset();
//        repository.getStrongerThanUser(getDummyUserScore(-1), 1);
//    }

    @Test(expected = IllegalArgumentException.class)
    public void topInvalidSizeThrowsException() {
        repository.reset();
        repository.getTop(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void auditingUnknownUserThrowsException() {
        repository.reset();
        repository.getAudit("unknown@domain.com", new int[MAX_NUMBER_OF_QUESTIONS + 1]);
    }

    
    @Test(expected = IllegalArgumentException.class)
    public void auditingQuestionForUnknownUserThrowsException() {
        repository.reset();
        repository.getAudit("unknown@domain.com", question(1));
    }

    @Test
    public void top() {
        for (int i = 0; i < 10; i++) {
            repository.publishRanking(getDummyUserScore(i));
        }
        assertThat(repository.getTop(1)).hasSize(1);
        assertThat(repository.getTop(20)).hasSize(10);

        Iterable<UserScore> top10 = repository.getTop(10);
        assertThat(top10).hasSize(10);
        for (int i = 0; i < 10; i++) {
            assertThat(get(top10, i).getEmail()).isEqualTo(getDummyUserScore(9 - i).getEmail());
        }
    }

    @Test
    public void strongerThanUser() {
        repository.reset();
        for (int i = 1; i <= 10; i++) {
            repository.publishRanking(getDummyUserScore(i));
        }
        assertThat(repository.getStrongerThanUser(getDummyUserScore(1), 2)).hasSize(2);
        assertThat(repository.getStrongerThanUser(getDummyUserScore(1), 1000)).hasSize(9);
        assertThat(repository.getTop(20)).hasSize(10);

        // disjoint
        Iterable<UserScore> players = repository.getStrongerThanUser(getDummyUserScore(1), 10);
        assertThat(players).hasSize(9);
        assertThat(Iterables.get(players, 0).getEmail()).isEqualTo(getDummyUserScore(10).getEmail());
        assertThat(Iterables.get(players, 8).getEmail()).isEqualTo(getDummyUserScore(2).getEmail());

        // in middle
        players = repository.getStrongerThanUser(getDummyUserScore(5), 10);
        assertThat(players).hasSize(5);
        assertThat(Iterables.get(players, 0).getEmail()).isEqualTo(getDummyUserScore(10).getEmail());
        assertThat(Iterables.get(players, 4).getEmail()).isEqualTo(getDummyUserScore(6).getEmail());
    }

    @Test
    public void checkTopOrderWhenScoreAreEqual() {
        repository.reset();
        // same score, same last name...
        repository.publishRanking(new UserScore("10", "a", "fn1", "a@b.com"));
        repository.publishRanking(new UserScore("10", "b", "fn2", "b@b.com"));
        repository.publishRanking(new UserScore("10", "c", "fn3", "c@b.com"));
        repository.publishRanking(new UserScore("10", "d", "fn4", "d@b.com"));
        repository.publishRanking(new UserScore("10", "e", "fn5", "e@b.com"));
        repository.publishRanking(new UserScore("10", "f", "fn6", "f@b.com"));
        repository.publishRanking(new UserScore("10", "g", "fn7", "g@b.com"));
        repository.publishRanking(new UserScore("10", "h", "fn8", "h@b.com"));

        List<UserScore> stronger = repository.getTop(4);
        assertThat(stronger).hasSize(4);
        assertThat(stronger.get(0).getLastName()).isEqualTo("a");
        assertThat(stronger.get(1).getLastName()).isEqualTo("b");
        assertThat(stronger.get(2).getLastName()).isEqualTo("c");
        assertThat(stronger.get(3).getLastName()).isEqualTo("d");

        stronger = repository.getTop(1);
        assertThat(stronger).hasSize(1);
        assertThat(stronger.get(0).getLastName()).isEqualTo("a");

        stronger = repository.getTop(1000);
        assertThat(stronger).hasSize(8);
        assertThat(stronger.get(7).getLastName()).isEqualTo("h");
    }

    @Test
    public void checkStrongerOrderWhenScoreAreEqual() {
        repository.reset();
        // same score, same last name...
        repository.publishRanking(new UserScore("10", "a", "fn1", "a@b.com"));
        repository.publishRanking(new UserScore("10", "b", "fn2", "b@b.com"));
        repository.publishRanking(new UserScore("10", "c", "fn3", "c@b.com"));
        repository.publishRanking(new UserScore("10", "d", "fn4", "d@b.com"));
        repository.publishRanking(new UserScore("10", "e", "fn5", "e@b.com"));

        List<UserScore> stronger = repository.getStrongerThanUser(new UserScore("10", "c", "fn3", "c@b.com"), 2);
        assertThat(stronger).hasSize(2);
        assertThat(stronger.get(0).getLastName()).isEqualTo("a");
        assertThat(stronger.get(1).getLastName()).isEqualTo("b");
    }

    @Test
    public void checkWeakerOrderWhenScoreAreEqual() {
        repository.reset();
        // same score, same last name...
        repository.publishRanking(new UserScore("10", "a", "fn1", "a@b.com"));
        repository.publishRanking(new UserScore("10", "b", "fn2", "b@b.com"));
        repository.publishRanking(new UserScore("10", "c", "fn3", "c@b.com"));
        repository.publishRanking(new UserScore("10", "d", "fn4", "d@b.com"));
        repository.publishRanking(new UserScore("10", "e", "fn5", "e@b.com"));

        List<UserScore> weaker = repository.getWeakerThanUser(new UserScore("10", "c", "fn3", "c@b.com"), 2);
        assertThat(weaker).hasSize(2);
        assertThat(weaker.get(0).getLastName()).isEqualTo("d");
        assertThat(weaker.get(1).getLastName()).isEqualTo("e");
    }

    @Test
    public void weakerThanUser() {
        repository.reset();
        for (int i = 1; i <= 10; i++) {
            repository.publishRanking(getDummyUserScore(i));
        }
        assertThat(repository.getWeakerThanUser(getDummyUserScore(10), 2)).hasSize(2);
        assertThat(repository.getWeakerThanUser(getDummyUserScore(10), 1000)).hasSize(9);
        assertThat(repository.getTop(20)).hasSize(10);

        // disjoint
        Iterable<UserScore> players = repository.getWeakerThanUser(getDummyUserScore(10), 10);
        assertThat(players).hasSize(9);
        assertThat(Iterables.get(players, 0).getEmail()).isEqualTo(getDummyUserScore(9).getEmail());
        assertThat(Iterables.get(players, 8).getEmail()).isEqualTo(getDummyUserScore(1).getEmail());

        // middle
        players = repository.getWeakerThanUser(getDummyUserScore(5), 10);
        assertThat(players).hasSize(4);
        assertThat(Iterables.get(players, 0).getEmail()).isEqualTo(getDummyUserScore(4).getEmail());
        assertThat(Iterables.get(players, 3).getEmail()).isEqualTo(getDummyUserScore(1).getEmail());
    }

    private Question question(int questionId) {
        return new QuestionBuilder() //
                .setQuestionId(questionId) //
                .setLabel("label") //
                .setGoodChoice(goodAnswers[questionId]) //
                .setChoice1("choice1") //
                .setChoice2("choice2") //
                .setChoice3("good answer") //
                .setChoice4("choice4") //
                .build();
    }

    private UserScore getDummyUserScore(int score) {
        return new UserScore(""+score, "ln" + score, "fn" + score, "email-with-score-" + score + "@domain.com");
    }
}
