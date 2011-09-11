package usi2011.repository;

import usi2011.domain.GameStatus;

public interface GameStatusRepository {
    public static enum GameStatusMetadata {
        NB_USER_LOGGED_FOR_MACHINE, GAME_CREATION_TIME, WHEN_FIRST_LOGGED_IN_MILLISECONDS, QUESTION_1_TIMEFRAME_START
    }
    public static enum GameStatusSuperColMetadata {
        NB_USER_LOGGED_FOR_MACHINE, GAME_CREATION_TIME, WHEN_FIRST_LOGGED_IN_MILLISECONDS, QUESTION_1_TIMEFRAME_START, NB_TWEET_SENT_FOR_MACHINE
    }    
    
    /**
     * SuperColumn Family to store local ranking request counter
     */
    public static enum RankingStatusMetadata {        
    }

    void reset();

    void loggedUsers(int nbLoggedUsers);
    
    void gameCreated(long gameCreationTime);
    
    void firstLogin(long firstLoginInMilliseconds);

    void question1TimeframeSTart(long question1TimeframeSTart);

    void tweetSent();
    
    GameStatus getGameStatus();

    /**
     * Mainly for test (so we can simulate multiple machine uid)
     * @param machineUid
     * @param gameCreationTimeMs
     * @param localCount
     */
    public void publishLocalRankingRequestCount(String machineUid, long gameCreationTimeMs, int localCount);

    public void publishLocalRankingRequestCount(long gameCreationTimeMs, int localCount);
    
    public int getGlobalRankingRequestCount(long gameCreationTimeMs);
}
