package usi2011.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import usi2011.domain.AuditUser;
import usi2011.domain.AuditUserAnswer;
import usi2011.domain.Question;
import usi2011.domain.Ranking.UserScore;
import usi2011.domain.User;
import usi2011.domain.UserScoreHistory;

public interface ScoreRepository {
    public static enum AnswersByMailMetadata {
    }

    public static enum FinalScoresMetadata {
    }

    public static final class Score {
        public final UserScoreHistory userScoreHistory;
        public final Question question;
        
        public Score(UserScoreHistory userScoreHistory, Question question) {
            this.userScoreHistory = userScoreHistory;
            this.question = question;
        }
    }

    void reset();

    void updateScore(UserScoreHistory userScoreHistory, Question question);
    
    int getScore(User user, Question[] questions, int[] goodAnswers);

    void updateScores(Iterable<Score> scores);
    
    void clearRankingsLoadedInMemory();    
    void loadAllRankingsFromCassandraToMemory();    

    List<UserScore> getWeakerThanUser(UserScore finalUserScore, int size);

    List<UserScore> getStrongerThanUser(UserScore finalUserScore, int size);

    List<UserScore> getTop(int size);

    void publishRankings(Iterable<UserScore> finalUserScores);

    void publishRanking(UserScore finalUserScore);

    AuditUser getAudit(String email, int[] goodAnswers);

    AuditUserAnswer getAudit(String email, Question question);
}
