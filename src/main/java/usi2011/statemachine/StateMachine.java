package usi2011.statemachine;

import usi2011.statemachine.support.StateCallback;

import com.google.common.base.Objects;

public interface StateMachine {

    public enum RequestQuestionAuthorization {
        REQUEST_ALLOWED, KO_OUT_OF_BOUND, KO_GAME_DID_NOT_START, KO_TOO_EARLY, KO_TOO_LATE, KO_GAME_ENDED
    }

    public enum State {
        START, // accept first login request
        LOGIN, // accept login request and question 1 request
        QUESTIONTIMEFRAME, // accept answer N and question N+1 request
        SYNCHROTIME, // accept question N+1 request or just wait but do not accept ranking, even if N+1 = 21
        RANKING, // accept ranking request s
        END;

        public final boolean isSTART() {
            return this == START;
        }

        public final boolean isLOGIN() {
            return this == LOGIN;
        }

        public final boolean isQUESTIONTIMEFRAME() {
            return this == QUESTIONTIMEFRAME;
        }

        public final boolean isSYNCHROTIME() {
            return this == SYNCHROTIME;
        }

        public final boolean isRANKING() {
            return this == RANKING;
        }

        public final boolean isEND() {
            return this == END;
        }
    }

    final public class CurrentState {
        private State state;
        private int questionN;
        private int question1Counter = 0;

        public CurrentState() {
        }

        public CurrentState(State state, int questionN, int question1Counter) {
            this.state = state;
            this.questionN = questionN;
            this.question1Counter = question1Counter;
        }

        public State getState() {
            return state;
        }

        public void setState(State state) {
            this.state = state;
        }

        public int getQuestionN() {
            return questionN;
        }

        public void setQuestionN(int questionN) {
            this.questionN = questionN;
        }

        public int getQuestion1Counter() {
            return question1Counter;
        }

        public void setQuestion1Counter(int question1Counter) {
            this.question1Counter = question1Counter;
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this) //
                    .add("currentState", state) //
                    .add("questionN", questionN) //
                    .add("question1Counter", question1Counter) //
                    .toString();
        }
    }

    public long getGameCreationTime();

    public void initGame(long gameIdJustForLoggingPurpose /* no time for cleaner stuff */);

    /**
     * Called by a periodic task whose job is to publish the local question1 counter value.
     */
    public int getQuestion1Counter();

    /**
     * Called by a periodic task whose job is to publish the local ranking counter value.
     */
    public int getRankingCounter();

    /**
     * Must be called by HTTP layer upon ranking success.
     */
    void incrementRankingCounter();

    /**
     * truncate, reset etc so a new game can take place.
     */
    void reset();

    /**
     * 
     */
    CurrentState getCurrentState();

    /**
     * Must be called by HTTP layer to determine if the current state permits '/api/game' request.
     */
    boolean isGameAllowed();

    /**
     * Must be called by HTTP layer to determine if the current state permits '/api/user' request.
     */
    boolean isUserAllowed();

    /**
     * Must be called by HTTP layer to determine if the current state permits '/api/login' request.
     */
    boolean isLoginAllowed();

    /**
     * Must be called by HTTP layer upon login success.
     */
    void handleLogin();

    /**
     * Must be called by HTTP layer to determine if the current state permits '/api/question' request.
     */
    RequestQuestionAuthorization isQuestionAllowed(int questionN);

    /**
     * Process question request. Defers processing until we enter QUESTIONTIMEFRAME state for questionN.
     */
    void question(int questionN, StateCallback callback);

    /**
     * Must be called by HTTP layer to determine if the current state permits '/api/answer' request for question questionN
     */
    boolean isAnswerAllowed(int questionN);

    /**
     * Must be called by HTTP layer to determine if the current state permits '/api/ranking' request.
     */
    boolean isRankingAllowed();

    /**
     * Must be called by HTTP layer to determine if the current state permits '/api/audit' request.
     */
    boolean isAuditAllowed();

    /**
     * Must be called by HTTP layer to determine if the current state permits '/api/score' request.
     */
    boolean isScoreAllowed();
}
