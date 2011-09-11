package usi2011.repository.cassandra;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static java.util.Collections.reverse;
import static me.prettyprint.hector.api.factory.HFactory.createMutator;
import static me.prettyprint.hector.api.factory.HFactory.createSliceQuery;
import static me.prettyprint.hector.api.factory.HFactory.createStringColumn;
import static org.slf4j.LoggerFactory.getLogger;
import static usi2011.domain.Ranking.UserScore.fromColumnNameString;
import static usi2011.repository.cassandra.CassandraRepository.cfName;
import static usi2011.repository.cassandra.HectorUtil.createIntegerColumn;
import static usi2011.util.LogUtil.isInfoEnabled;
import static usi2011.util.Profiles.HECTOR;
import static usi2011.util.Specifications.FIRST_QUESTION;
import static usi2011.util.Specifications.MAX_NUMBER_OF_QUESTIONS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.prettyprint.cassandra.serializers.IntegerSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.mutation.Mutator;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import usi2011.domain.AuditUser;
import usi2011.domain.AuditUserAnswer;
import usi2011.domain.GameStatus;
import usi2011.domain.Question;
import usi2011.domain.Ranking.UserScore;
import usi2011.domain.User;
import usi2011.domain.UserScoreHistory;
import usi2011.repository.GameStatusRepository;
import usi2011.repository.ScoreRepository;
import usi2011.repository.ScoreRepository.AnswersByMailMetadata;

@Component
@ManagedResource
@Profile(HECTOR)
public class CassandraScoreRepository implements ScoreRepository {
    private static final Logger logger = getLogger(CassandraScoreRepository.class);
    private static final StringSerializer stringSerializer = StringSerializer.get();
    private static final IntegerSerializer integerSerializer = IntegerSerializer.get();
    private static final String CF_ANSWER_BY_MAIL = cfName(AnswersByMailMetadata.class);
    private static final String CF_FINAL_SCORES = cfName(FinalScoresMetadata.class);
    /**
     * see {@link #buildAnswerByMailColumnName(AnswersByMailMetadata, int) 
     */
    private static final String FINAL_SCORES_KEY = "values";
    @Autowired
    private CassandraRepository cassandra;

    private boolean enableMemoryCaching = true;
    Map<String, Integer> rankingIndex;
    List<String> rankingData;

    @Autowired
    private GameStatusRepository gameStatusRepository;
    
    
    @Override
    @ManagedOperation
    public void reset() {
        if (isInfoEnabled) {
            logger.info("Reset score");
        }
        cassandra.truncate(AnswersByMailMetadata.class);
        cassandra.truncate(cassandra.rankingKeyspace(), CF_FINAL_SCORES);
    }

    @Override
    public void updateScores(Iterable<Score> scores) {
        final Mutator<String> mutator = createMutator(cassandra.keyspace(), stringSerializer);
        for (Score score : scores) {
            batchUpdateScore(mutator, score.userScoreHistory, score.question);
        }
        mutator.execute();
    }

    @Override
    public void updateScore(UserScoreHistory userScoreHistory, Question question) {
        final Mutator<String> mutator = createMutator(cassandra.keyspace(), stringSerializer);
        batchUpdateScore(mutator, userScoreHistory, question);
        mutator.execute();
    }

    private void batchUpdateScore(Mutator<String> mutator, UserScoreHistory userScoreHistory, Question question) {
        checkNotNull(userScoreHistory, "userScoreHistory cannot be null");
        checkNotNull(question, "question cannot be null");

        final int questionId = question.getQuestionId();
        final String email = userScoreHistory.getEmail();
        final int userAnswer = userScoreHistory.getAnswerAsInt(questionId);
        mutator.addInsertion(email, CF_ANSWER_BY_MAIL, createIntegerColumn(questionId, userAnswer));
    }

    @Override
    public AuditUserAnswer getAudit(final String email, final Question question) {
        final ColumnSlice<Integer, Integer> result = createSliceQuery(cassandra.keyspace(), stringSerializer, integerSerializer, integerSerializer) //
                .setColumnFamily(CF_ANSWER_BY_MAIL) //
                .setKey(email) //
                .setColumnNames(question.getQuestionId()) //
                .execute() //
                .get();

        checkArgument(!result.getColumns().isEmpty(), "%s is not found", question.getQuestionId());
        int userAnswer = result.getColumnByName(question.getQuestionId()).getValue();
        return new AuditUserAnswer(userAnswer, question.getGoodChoice(), question.getLabelJsonEscaped());
    }

    @Override
    public AuditUser getAudit(final String email, final int[] goodAnswers) {
        final int[] userAnswers = getAnswersByEmail(email);
        return new AuditUser(userAnswers, goodAnswers);
    }

    @Override
    public int getScore(User user, Question[] questions, int[] goodAnswers) {
        // recompute the score (since we do not store it)
        UserScoreHistory ush = new UserScoreHistory(user.getEmail(), user.getLastName(), user.getFirstName(), goodAnswers);
        int[] userAnswers = getAnswersByEmail(user.getEmail());
        for (int i = FIRST_QUESTION; i <= MAX_NUMBER_OF_QUESTIONS; i++) {
            ush.answer(questions[i], userAnswers[i]);
        }

        return ush.getScore();
    }

    private int[] getAnswersByEmail(String email) {
        final ColumnSlice<Integer, Integer> result = createSliceQuery(cassandra.keyspace(), stringSerializer, integerSerializer, integerSerializer) //
                .setColumnFamily(CF_ANSWER_BY_MAIL) //
                .setKey(email) //
                .setRange(1, MAX_NUMBER_OF_QUESTIONS + 1, false, MAX_NUMBER_OF_QUESTIONS) //
                .execute() //
                .get();

        checkArgument(!result.getColumns().isEmpty(), "Could not find user %s for audit", email);

        final Map<Integer, Integer> userAnswers = newHashMap();
        for (HColumn<Integer, Integer> column : result.getColumns()) {
            final int questionId = column.getName();
            final int userAnswer = column.getValue();
            userAnswers.put(questionId, userAnswer);
        }
        final int[] ret = new int[MAX_NUMBER_OF_QUESTIONS + 1];
        for (int i = FIRST_QUESTION; i <= MAX_NUMBER_OF_QUESTIONS; i++) {
            Integer answer = userAnswers.get(i);
            if (answer != null) {
                ret[i] = answer;
            }
        }
        return ret;
    }

    // ---------------------------------------------
    // RANKING (separate keyspace)
    // ---------------------------------------------

    @Override
    public void publishRanking(UserScore userScore) {
        publishRankings(newArrayList(userScore));
    }

    @Override
    public void publishRankings(Iterable<UserScore> userScores) {
        final Mutator<String> mutator = createMutator(cassandra.rankingKeyspace(), stringSerializer);
        for (UserScore userScore : userScores) {
            batchPublishRanking(mutator, userScore);
        }
        mutator.execute();
    }

    private void batchPublishRanking(Mutator<String> mutator, UserScore userScore) {
        checkNotNull(userScore, "userScore cannot be null");
        mutator.addInsertion(FINAL_SCORES_KEY, CF_FINAL_SCORES, createStringColumn(userScore.toColumnNameString(), ""));
    }


    @Override
    public List<UserScore> getTop(final int size) {
        if (enableMemoryCaching) {
            return getTopFromMemory(size);
        } else {
            return getTopFromCassandra(size);            
        }
    }

    @Override
    public List<UserScore> getStrongerThanUser(UserScore us, int size) {
        if (enableMemoryCaching) {
            return getStrongerThanUserFromMemory(us, size);
        } else {
            return getStrongerThanUserFromCassandra(us, size);            
        } 
    }

    @Override
    public List<UserScore> getWeakerThanUser(UserScore us, int size) {
        if (enableMemoryCaching) {
            return getWeakerThanUserFromMemory(us, size);
        } else {
            return getWeakerThanUserFromCassandra(us, size);            
        }        
    }
    
    
    public List<UserScore> getTopFromCassandra(final int size) {
        checkArgument(size > 0, "invalid size");
        final ColumnSlice<String, String> result = createSliceQuery(cassandra.rankingKeyspace(), stringSerializer, stringSerializer, stringSerializer) //
                .setColumnFamily(CF_FINAL_SCORES) //
                .setKey(FINAL_SCORES_KEY) //
                .setRange(null, null, false, size) // reverse is false for a good reason: please see Ranking.transformScore
                .execute() //
                .get();
        final List<UserScore> ret = newArrayList();
        for (HColumn<String, String> col : result.getColumns()) {
            ret.add(fromColumnNameString(col.getName()));
        }
        return ret;
    }

    public List<UserScore> getStrongerThanUserFromCassandra(UserScore us, int size) {
        checkArgument(size > 0, "invalid size");
        final ColumnSlice<String, String> result = createSliceQuery(cassandra.rankingKeyspace(), stringSerializer, stringSerializer, stringSerializer)
                .setColumnFamily(CF_FINAL_SCORES) //
                .setKey(FINAL_SCORES_KEY) //
                .setRange(us.toColumnNameString() // start
                        , null // finish is not included (beware, see Ranking.transformScore)
                        , true // reverse is true for a good reason: please see Ranking.transformScore
                        , size + 1) //
                .execute() //
                .get();

        final List<UserScore> ret = newArrayList();
        for (HColumn<String, String> col : result.getColumns()) {
            final UserScore currentUserScore = fromColumnNameString(col.getName());
            if (!currentUserScore.getEmail().equals(us.getEmail())) {
                ret.add(currentUserScore);
            }
        }
        reverse(ret); // tricky
        return ret;
    }

    public List<UserScore> getWeakerThanUserFromCassandra(UserScore us, int size) {
        checkArgument(size > 0, "invalid size");
        final ColumnSlice<String, String> result = createSliceQuery(cassandra.rankingKeyspace(), stringSerializer, stringSerializer, stringSerializer)
                .setColumnFamily(CF_FINAL_SCORES) //
                .setKey(FINAL_SCORES_KEY) //
                .setRange(us.toColumnNameString() // start (beware, look at Ranking.transformScore)
                        , null // finish
                        , false // reverse is false for a good reason: please see Ranking.transformScore
                        , size + 1) //
                .execute() //
                .get();

        final List<UserScore> ret = newArrayList();
        for (HColumn<String, String> col : result.getColumns()) {
            final UserScore currentUserScore = UserScore.fromColumnNameString(col.getName());
            if (!currentUserScore.getEmail().equals(us.getEmail())) {
                ret.add(currentUserScore);
            }
        }
        return ret;
    }
    
    public void clearRankingsLoadedInMemory() {
        // DO NOT DO THIS IN RESET... think distributed!!!
        rankingIndex = null;
        rankingData = null;        
    }

    @Override
    public void loadAllRankingsFromCassandraToMemory() {
        if (!enableMemoryCaching) {
            return;
        }
        
        if (rankingData == null || rankingIndex == null) {
            long t0 = System.currentTimeMillis();
            logger.warn("Ranking: preparing query");
            final ColumnSlice<String, String> result = createSliceQuery(cassandra.rankingKeyspace(), stringSerializer, stringSerializer, stringSerializer)
                    .setColumnFamily(CF_FINAL_SCORES) //
                    .setKey(FINAL_SCORES_KEY) //
                    .setRange(null // start
                            , null // finish
                            , false // reverse is false for a good reason: please see Ranking.transformScore
                            , 1000 * 1000 * 10) //
                    .execute() //
                    .get();

            logger.warn("Ranking: query executed ok");
            rankingData = new ArrayList<String>(1000000);// todo: make it conf or use nbUserLoggedIn
            rankingIndex = new HashMap<String, Integer>(100000); // todo: make it conf or use nbUserLoggedIn
            logger.warn("Ranking: memory ready. Start iterating result");

            int counter = 0;

            for (HColumn<String, String> col : result.getColumns()) {
                String colName = col.getName();
                rankingData.add(colName);
                rankingIndex.put(colName, counter++);
            }
            logger.warn("Ranking: iteration done, memory filled");

            long duration = System.currentTimeMillis() - t0;
            GameStatus gs = gameStatusRepository.getGameStatus();
            logger.warn("Get all {} rankings in {} ms. Nb users logged was {}. We miss {} entries/users <=> {}%", 
                    new Object[] { rankingData.size(), 
                        duration, 
                        gs.getNbOfUsersLogged(), 
                        gs.getNbOfUsersLogged() - rankingData.size(), 
                        (100 * (gs.getNbOfUsersLogged() - rankingData.size())) / (gs.getNbOfUsersLogged() > 0 ? gs.getNbOfUsersLogged() : 1)
            });            
        } else {
            logger.warn("Oh oh, you call it more than once in a raw without cleaning memory in between...");
        }
    }
    

    public List<UserScore> getTopFromMemory(final int size) {
        checkArgument(size > 0, "invalid size");
        final List<UserScore> ret = newArrayList();        
        int rsize = rankingData.size();
        for (int i = 0; i < size && i < rsize; i ++) {
            ret.add(fromColumnNameString(rankingData.get(i)));            
        }
        return ret;
    }

    public List<UserScore> getStrongerThanUserFromMemory(UserScore us, int size) {
        checkArgument(size > 0, "invalid size");

        Integer index = rankingIndex.get(us.toColumnNameString());
        if (index == null) {
            throw new IllegalStateException("Ranking index missing for \""+ us.toColumnNameString() +"\". rankingData size=" + rankingData.size());
        }
       
        final List<UserScore> ret = newArrayList();
        for (int i = index - 1; i > index - size - 1 && i >= 0; i--) {
            final UserScore currentUserScore = UserScore.fromColumnNameString(rankingData.get(i));
            ret.add(currentUserScore);
        }
        Collections.reverse(ret); // tricky
        return ret;
    }

    public List<UserScore> getWeakerThanUserFromMemory(UserScore us, int size) {
        checkArgument(size > 0, "invalid size");

        int rsize = rankingData.size();
        Integer index = rankingIndex.get(us.toColumnNameString());
        if (index == null) {
            throw new IllegalStateException("Ranking index missing for \""+ us.toColumnNameString() +"\". rankingData size=" + rsize);
        }

        final List<UserScore> ret = newArrayList();
        for (int i = index + 1; i < index + size + 1 && i < rsize; i ++) {
            final UserScore currentUserScore = UserScore.fromColumnNameString(rankingData.get(i));
            ret.add(currentUserScore);
        }
        return ret;
    }
}
