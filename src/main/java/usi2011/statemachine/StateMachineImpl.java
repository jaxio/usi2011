package usi2011.statemachine;

import static java.lang.System.currentTimeMillis;
import static org.slf4j.LoggerFactory.getLogger;
import static usi2011.statemachine.StateMachine.RequestQuestionAuthorization.KO_GAME_DID_NOT_START;
import static usi2011.statemachine.StateMachine.RequestQuestionAuthorization.KO_GAME_ENDED;
import static usi2011.statemachine.StateMachine.RequestQuestionAuthorization.KO_OUT_OF_BOUND;
import static usi2011.statemachine.StateMachine.RequestQuestionAuthorization.KO_TOO_EARLY;
import static usi2011.statemachine.StateMachine.RequestQuestionAuthorization.KO_TOO_LATE;
import static usi2011.statemachine.StateMachine.RequestQuestionAuthorization.REQUEST_ALLOWED;
import static usi2011.statemachine.StateMachine.State.END;
import static usi2011.statemachine.StateMachine.State.LOGIN;
import static usi2011.statemachine.StateMachine.State.QUESTIONTIMEFRAME;
import static usi2011.statemachine.StateMachine.State.RANKING;
import static usi2011.statemachine.StateMachine.State.START;
import static usi2011.statemachine.StateMachine.State.SYNCHROTIME;
import static usi2011.util.LogUtil.isInfoEnabled;
import static usi2011.util.LogUtil.isWarnEnabled;
import static usi2011.util.Specifications.FIRST_QUESTION;
import static usi2011.util.Specifications.MAX_NUMBER_OF_QUESTIONS;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import usi2011.domain.GameStatus;
import usi2011.domain.Parameters;
import usi2011.exception.NoGameCreatedException;
import usi2011.http.StateMachineHttpRequestHandler;
import usi2011.http.support.HttpResponseStatsService;
import usi2011.repository.GameStatusRepository;
import usi2011.repository.ParameterRepository;
import usi2011.repository.ScoreRepository;
import usi2011.repository.UserRepository;
import usi2011.service.RankingService;
import usi2011.statemachine.support.CallbackFlusher;
import usi2011.statemachine.support.StateCallback;
import usi2011.task.BatchRankingPublisherTask;
import usi2011.task.GameStatusPollerTask;
import usi2011.task.PeriodicTask;

/**
 * State machine reflecting diagram we agreed upon. Please see src/main/site/*vpp.
 */
@Component
@Primary
public final class StateMachineImpl implements StateMachine, PeriodicTask {
    private static final Logger logger = getLogger(StateMachineImpl.class);
    public static final int MAX_NUMBER_OF_CLIENTS_ON_SINGLE_MACHINE = 200000;

    @Autowired
    ParameterRepository parameterRepository;
    @Autowired
    ScoreRepository scoreRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    RankingService rankingService;
    @Autowired
    HttpResponseStatsService httpResponseStatsTask;
    @Autowired
    BatchRankingPublisherTask batchRankingPublisherTask;
    @Autowired
    GameStatusPollerTask gameStatusPollerTask;

    Parameters parameters;

    // context
    LoginTimeout loginTimeout;
    QuestionTimeframeTimeout questionTimeframeTimeout;
    SynchrotimeTimeout synchrotimeTimeout;

    /**
     * In Login state, maintain a local counter of user requesting Q1. Required to determine if global user threshold is reached.
     */
    final AtomicInteger question1Counter = new AtomicInteger(0);
    final AtomicInteger questionCounter[] = new AtomicInteger[MAX_NUMBER_OF_QUESTIONS + 1];
    /**
     * In Ranking state, maintain a local counter of user requesting ranking. Required to determine if global ranking threshold is reached in order to tweet!
     */
    final AtomicInteger rankingCounter = new AtomicInteger(0);
    final AtomicBoolean initialized = new AtomicBoolean(false);

    // timestamp du d√©but de la partie en cours
    long gameCreationTime = 0;
    // Atomic pour √©viter une modif concurrente par handleLogin et ongameStatus
    final AtomicLong tFirstLogin = new AtomicLong(Long.MAX_VALUE); // global time for first user login
    // Atomic pour √©viter une modif concurrente par loginTimerTask & ongameStatus
    final AtomicLong tQuestion1 = new AtomicLong(Long.MAX_VALUE); // global time for max user reached

    // count the nb of time we override the tQ1 during QTIMEFRAME 1.
    private int tQ1OverrideOkCounter = 0;
    
    // count the nb of time we do not need to override tQ1
    private int tQ1StableCounter = 0;
    
    @Value("${stateMachine.tQ1StabilizationReachedWhenStableCounterValueIs:3}")
    private int tQ1StabilizationReachedWhenStableCounterValueIs;

    State currentState = START;
    int currentQuestionN = 1;

    @SuppressWarnings("unchecked")
    final LinkedBlockingQueue<StateCallback> callbacks[] = new LinkedBlockingQueue[MAX_NUMBER_OF_QUESTIONS + 1];
    StateCallback callbacksArray[][];

    private boolean useArrayCallback = true;

    @Autowired
    CallbackFlusher callbackFlusher;
    @Autowired
    GameStatusRepository loginRepository;
    @Autowired
    GameStatusPollerTask loginrepositoryPollerTask;
    @Autowired
    StateMachineHttpRequestHandler stateMachineHttpRequestHandler;

    private long deltaForImmediateRunMs = 10l;

    @Value("${stateMachine.delayToleranceMs:0}")
    private long delayToleranceMs;

    @Value("${stateMachine.loadAllRankingsFromCassandraToMemoryDelayMs:5000}")
    private long loadAllRankingsFromCassandraToMemoryDelayMs;

    public StateMachineImpl() {
        if (useArrayCallback) {            
            callbacksArray = new StateCallback[MAX_NUMBER_OF_QUESTIONS + 1][MAX_NUMBER_OF_CLIENTS_ON_SINGLE_MACHINE];
        } else {
            for (int questionId = FIRST_QUESTION; questionId < MAX_NUMBER_OF_QUESTIONS + 1; questionId++) {
                callbacks[questionId] = new LinkedBlockingQueue<StateCallback>();
            }
        } 
    }

    public long getGameCreationTime() {
        return gameCreationTime;
    }

    @Override
    public synchronized void reset() {
        initialized.set(false);
        if (loginTimeout != null) {
            loginTimeout.cancel();
            loginTimeout = null;
        }

        if (questionTimeframeTimeout != null) {
            questionTimeframeTimeout.cancel();
            questionTimeframeTimeout = null;
        }

        if (synchrotimeTimeout != null) {
            synchrotimeTimeout.cancel();
            synchrotimeTimeout = null;
        }

        question1Counter.set(0);
        for (int i = 0; i < questionCounter.length; i++) {
            questionCounter[i] = new AtomicInteger(0);
        }
        
        rankingCounter.set(0);
        scoreRepository.clearRankingsLoadedInMemory();
        userRepository.resetCacheHitStats();

        // TODO : that's the second time the initialized is reseted ... is that on purpose ?
        initialized.set(false);

        tFirstLogin.set(Long.MAX_VALUE);
        tQuestion1.set(Long.MAX_VALUE);
        tQ1OverrideOkCounter = 0;
        tQ1StableCounter = 0;
        currentState = START;
        currentQuestionN = FIRST_QUESTION;

        if (useArrayCallback) {
            for (int i = 0; i < callbacks.length; i++) {
                if (callbacksArray[i] != null) {
                    callbackFlusher.asyncSuccessArray(callbacksArray[i], i);
                }
            }
            callbacksArray = new StateCallback[MAX_NUMBER_OF_QUESTIONS + 1][MAX_NUMBER_OF_CLIENTS_ON_SINGLE_MACHINE];
        } else {
            for (int i = 0; i < callbacks.length; i++) {
                callbackFlusher.asyncSuccess(callbacks[i], i);
            }            
        }
    }

    public synchronized void initGame(long gameIdForLogger) {
        if (initialized.get()) {
            return;
        }
        rankingService.reset();
        parameters = parameterRepository.getParameters();
        if (parameters == null) {
            if (isWarnEnabled) {
                logger.warn("No parameter found, we should have one, cannot init state machine");
            }
            return;
        }
        batchRankingPublisherTask.setParameters(parameters);

        if (isWarnEnabled) {
            logger.warn("parameters loaded : \n" + parameters);
        }

        stateMachineHttpRequestHandler.init();
        initialized.set(true);
        httpResponseStatsTask.reset();
        if (isWarnEnabled) {
            logger.warn("Game initialized. gameid={}", gameIdForLogger);
        }
    }

    @Override
    public CurrentState getCurrentState() {
        return new CurrentState(currentState, currentQuestionN, question1Counter.get());
    }

    private void doStateTransition(State newState, int newQuestionN) {
        doStateTransition(newState, newQuestionN //
                , getExpirationTimeForState(tQuestion1.get(), newState, newQuestionN));
    }

    private synchronized void doStateTransition(State newState, int newQuestionN, Date expirationTime) {
        if (!initialized.get()) {
            throw new IllegalStateException("The machine is not initialized yet!");
        }
        switch (currentState) {
        // ----------------------------------------------------
        // - [from START to xxx]
        // ----------------------------------------------------
        case START:
            switch (newState) {
            case LOGIN:
                if (isWarnEnabled) {
                    logger.warn("State transition: {} -> {}", START, LOGIN);
                }
                setState(LOGIN, 1);
                loginTimeout = new LoginTimeout(expirationTime);
                break;

            default:
                logInvalidTransition(newState, newQuestionN, expirationTime);
                break;
            }

            break;

        // ----------------------------------------------------
        // - [from LOGIN to xxx]
        // ----------------------------------------------------
        case LOGIN:
            switch (newState) {
            case QUESTIONTIMEFRAME:
                if (isWarnEnabled) {
                    logger.warn("State transition: {} -> {} ({})", new Object[] { LOGIN, QUESTIONTIMEFRAME, newQuestionN });
                }
                setState(QUESTIONTIMEFRAME, newQuestionN);
                flushPendingQuestionRequest(currentState, newQuestionN); // flush first question
                questionTimeframeTimeout = new QuestionTimeframeTimeout(currentQuestionN, expirationTime);
                break;

            default:
                logInvalidTransition(newState, newQuestionN, expirationTime);
                break;
            }
            break;

        // ----------------------------------------------------
        // - [from QUESTION to xxx]
        // ----------------------------------------------------
        case QUESTIONTIMEFRAME:
            switch (newState) {
            case SYNCHROTIME:
                if (isWarnEnabled) {
                    if (currentQuestionN != newQuestionN) {
                        logger.warn("ATTENTION This should not happen, unless under extremelly heavy load, we should not try to recover, we are doomed");
                    }
                    logger.warn("State transition: {} ({}) -> {} ({})", new Object[] { QUESTIONTIMEFRAME, currentQuestionN, SYNCHROTIME, newQuestionN });
                }
                setState(SYNCHROTIME, newQuestionN);
                synchrotimeTimeout = new SynchrotimeTimeout(newQuestionN, expirationTime);
                break;

            default:
                logInvalidTransition(newState, newQuestionN, expirationTime);
                break;
            }
            break;

        // ----------------------------------------------------
        // - [from SYNCHRO to xxx]
        // ----------------------------------------------------
        case SYNCHROTIME:
            switch (newState) {
            case QUESTIONTIMEFRAME:
                if (isWarnEnabled) {
                    if (currentQuestionN + 1 != newQuestionN) {
                        logger.warn("ATTENTION This should not happen, unless under extremelly heavy load, we should not try to recover, we are doomed");
                    }
                    logger.warn("State transition: {} ({}) -> {} ({})", new Object[] { SYNCHROTIME, currentQuestionN, QUESTIONTIMEFRAME, newQuestionN });
                }
                setState(QUESTIONTIMEFRAME, newQuestionN);
                flushPendingQuestionRequest(currentState, newQuestionN);
                questionTimeframeTimeout = new QuestionTimeframeTimeout(newQuestionN, expirationTime);
                break;

            case RANKING:
                if (isWarnEnabled) {
                    if (currentQuestionN != parameters.getNbQuestions()) {
                        logger.warn("ATTENTION This should not happen, unless under extremelly heavy load, we should not try to recover, we are doomed");
                    }
                    logger.warn("State transition: {} ({}) -> {}", new Object[] { SYNCHROTIME, currentQuestionN, RANKING });
                }

                setState(RANKING, 0);

                if (loadAllRankingsFromCassandraToMemoryDelayMs > 0) {
                    logger.warn("Scheduling load ranking in memory in {} ms", loadAllRankingsFromCassandraToMemoryDelayMs);
                    new Timer().schedule(new TimerTask() {
                        private long gameCT = gameCreationTime;

                        @Override
                        public void run() {
                            if (gameCT == gameCreationTime) {
                                // we are in the same game
                                logger.warn("Load ranking in memory...");
                                try {
                                    scoreRepository.loadAllRankingsFromCassandraToMemory();
                                } catch (Throwable t) {
                                    logger.warn("While loading ranking from task", t);
                                }
                            } else {
                                logger.warn("Canceling Load ranking in memory. Not in the same game");
                            }
                        }
                    }, loadAllRankingsFromCassandraToMemoryDelayMs);
                } else {
                    logger.warn("Load ranking in memory...");
                    try {
                        scoreRepository.loadAllRankingsFromCassandraToMemory();
                    } catch (Throwable t) {
                        logger.warn("While loading ranking", t);
                    }
                }

                break;

            default:
                logInvalidTransition(newState, newQuestionN, expirationTime);
                break;
            }
            break;

        // ----------------------------------------------------
        // - [from RANKING to xxx]
        // ----------------------------------------------------

        case RANKING:
            switch (newState) {
            case END:
                if (isWarnEnabled) {
                    logger.warn("State transition: {} -> {}", RANKING, END);
                }
                currentState = END;
                break;

            default:
                logInvalidTransition(newState, newQuestionN, expirationTime);
                break;
            }
            break;

        default:
            logInvalidTransition(newState, newQuestionN, expirationTime);
        }
    }

    private void setState(State state, int questionN) {
        currentState = state;
        currentQuestionN = questionN;
        if (currentState.isQUESTIONTIMEFRAME() || currentState.isSYNCHROTIME() || currentState.isRANKING()) {
            logCurrentStateElapsedTime();
        }
    }

    private void logInvalidTransition(State newState, int newQuestionN, Date expirationTime) {
        if (isWarnEnabled) {
            logger.warn("Invalid transition: {} ({}) -> {} ({})", new Object[] { currentState, currentQuestionN, newState, newQuestionN }, new Exception());
        }
    }

    @Override
    public boolean isGameAllowed() {
        // TODO: √† voir √ßa....
        return true; // free lunch
    }

    @Override
    public boolean isUserAllowed() {
        return currentState.isSTART() || currentState.isRANKING();
    }

    @Override
    public boolean isLoginAllowed() {
        return currentState.isSTART() || currentState.isLOGIN();
    }

    @Override
    public void handleLogin() {
        if (!(initialized.get())) {
            throw new NoGameCreatedException("no game created");
        }
        
        // note: avoid calling currentTimeMillis after first login
        if (tFirstLogin.get() == Long.MAX_VALUE && tFirstLogin.compareAndSet(Long.MAX_VALUE, currentTimeMillis())) {
            synchronized (this) {
                doStateTransition(LOGIN, 1, new Date(tFirstLogin.get() + parameters.getLoginTimeoutInMs()));
                // on persiste, si un timestamp ant√©rieur existe, il sera ramen√© par le gameStatusPoller, et le Timer repositionn√© en fonction...
                loginRepository.firstLogin(tFirstLogin.get());
            }
        }
        // Si un user passe ici alors que le premier login a d√©clench√© le traitement pr√©c√©dent, et que celui-ci n'est pas termin√©, pas vraiment de pb :
        // Il serait hautement improbable que le doStateTransition au dessus n'ait pas eu le temps de s'√©xecuter avant qu'il demande la question...)
    }

    @Override
    public RequestQuestionAuthorization isQuestionAllowed(int questionN) {
        if (parameters == null) {
            return KO_GAME_DID_NOT_START;
        }
        if (questionN > parameters.getNbQuestions() || questionN < 1) {
            return KO_OUT_OF_BOUND;
        }
        switch (currentState) {
        case START:
            return KO_GAME_DID_NOT_START;
        case LOGIN:
            return questionN == FIRST_QUESTION ? REQUEST_ALLOWED : KO_TOO_EARLY;
        case QUESTIONTIMEFRAME:
        case SYNCHROTIME:
            if (questionN == currentQuestionN + 1) {
                return REQUEST_ALLOWED;
            } else if (questionN > currentQuestionN + 1) {
                return KO_TOO_EARLY;
            } else if (questionN <= currentQuestionN) {
                return KO_TOO_LATE;
            } else {
                // nothing
            }
        case RANKING:
        case END:
        default:
            return KO_GAME_ENDED;
        }
    }

    @Override
    public void question(int questionN, StateCallback callback) {
        switch (currentState) {
        case START:
            callback.failure(KO_GAME_DID_NOT_START.name());
            return;
        case LOGIN:
            if (questionN == FIRST_QUESTION) {
                int index = question1Counter.getAndIncrement();
                if (useArrayCallback) {
                    callbacksArray[questionN][index] = callback;
                } else {
                    callbacks[FIRST_QUESTION].add(callback);
                }
                return;
            } else {
                callback.failure(KO_TOO_EARLY.name());
                return;
            }
        case QUESTIONTIMEFRAME:
        case SYNCHROTIME:
            if (questionN == currentQuestionN + 1) {
                if (useArrayCallback) {
                    callbacksArray[questionN][questionCounter[questionN].getAndIncrement()] = callback;                    
                } else {
                    callbacks[questionN].add(callback);
                }
                return;
            } else if (questionN > currentQuestionN + 1) {
                callback.failure(KO_TOO_EARLY.name());
                return;
            } else if (questionN <= currentQuestionN) {
                callback.failure(KO_TOO_LATE.name());
                return;
            } else {
                // nothing
            }
        case RANKING:
        case END:
            callback.failure(KO_GAME_ENDED.name());
            return;
        }
    }

    private void flushPendingQuestionRequest(State state, int questionN) {
        if (isInfoEnabled) {
            logger.info("{} ({}) requests", state, questionN);
        }
        if (useArrayCallback) {
            callbackFlusher.asyncSuccessArray(callbacksArray[questionN], questionN);            
        } else {
            callbackFlusher.asyncSuccess(callbacks[questionN], questionN);
        }
    }

    @Override
    public boolean isAnswerAllowed(int questionN) {
        boolean result = currentState.isQUESTIONTIMEFRAME() && questionN == currentQuestionN;
        if (result) {
            return true;
        }

        // let's see if we can tolerate this probably 'late' answer
        if (delayToleranceMs > 0 && currentState.isSYNCHROTIME() && questionN + 1 == currentQuestionN) {
            // get the deadLine for the previous QTF state.
            Date deadLine = getExpirationTimeForState(tQuestion1.get(), QUESTIONTIMEFRAME, questionN);
            if (currentTimeMillis() - deadLine.getTime() <= delayToleranceMs) {
                if (isWarnEnabled) {
                    logger.warn("close to answer limit");
                }
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isRankingAllowed() {
        return currentState.isRANKING();
    }

    @Override
    public boolean isAuditAllowed() {
        return currentState.isRANKING();
    }

    @Override
    public boolean isScoreAllowed() {
        return currentState.isRANKING() || currentState.isSTART();
    }

    // -----------------------------
    // LoginTimeout
    // -----------------------------

    /**
     * Schedule the transition from LOGIN to QUESTIONTIMEFRAME 1.
     */
    public class LoginTimeout extends TimerTask {
        long myGameCreationTime = gameCreationTime;

        LoginTimeout(Date expirationTime) {
            if (expirationTime.getTime() - currentTimeMillis() < deltaForImmediateRunMs) {
                if (isWarnEnabled) {
                    logger.warn("LoginTimeout: invoke run() directly to catch up");
                }
                run();
            } else {
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        logger.warn("call gc: total={}, free={}", Runtime.getRuntime().totalMemory(), Runtime.getRuntime().freeMemory());
                        System.gc();
                        logger.warn("done gc: total={}, free={}", Runtime.getRuntime().totalMemory(), Runtime.getRuntime().freeMemory());
                    }
                }, new Date(expirationTime.getTime() - TimeUnit.SECONDS.toMillis(5)));
                new Timer().schedule(this, expirationTime);
            }
        }

        @Override
        public void run() {
            runLoginTimeout(this);
        }
    }

    private synchronized void runLoginTimeout(LoginTimeout lto) {
        if (lto.myGameCreationTime == 0 || lto.myGameCreationTime != gameCreationTime) {
            // not in the same game...
            return;
        }

        if (!initialized.get()) {
            // au cas ou on fait un create game en cours de partie, le cancel n'a pas forc√©ment le temps de passer dans reset();
            return;
        }

        if (isWarnEnabled) {
            logger.warn("{}: expired", LOGIN);
        }
        // upon natural expiration, we can deduce tQuestion1 from the global tLogin
        if (tQuestion1.compareAndSet(Long.MAX_VALUE, tFirstLogin.get() + parameters.getLoginTimeoutInMs())) {
            if (isWarnEnabled) {
                logger.warn("tQuestion1 deduced from natural expiration");
            }
            doStateTransition(QUESTIONTIMEFRAME, 1);
            // on persiste apr√®s le changement d'√©tat, pour √©viter un d√©calage...
            loginRepository.question1TimeframeSTart(tQuestion1.get());
        } // else : le changement de state a d√©j√† √©t√© fait dans ongameStatus...
    }

    // -----------------------------
    // QuestionTimeframeTimeout
    // -----------------------------

    /**
     * Schedule the transition from QUESTIONTIMEFRAME N to SYNCHROTIME N.
     */
    public class QuestionTimeframeTimeout extends TimerTask {
        long myGameCreationTime = gameCreationTime;
        int qn;

        QuestionTimeframeTimeout(int questionN, Date expirationTime) {
            qn = questionN;
            if (expirationTime.getTime() - currentTimeMillis() < deltaForImmediateRunMs) {
                if (isWarnEnabled) {
                    logger.warn("{} ({}): invoke run() directly to catch up", QUESTIONTIMEFRAME, questionN);
                }
                run();
            } else {
                new Timer().schedule(this, expirationTime);
            }
        }

        @Override
        public void run() {
            runQuestionTimeframeTimeout(this);
        }
    }

    private synchronized void runQuestionTimeframeTimeout(QuestionTimeframeTimeout qto) {
        if (qto.myGameCreationTime == 0 || qto.myGameCreationTime != gameCreationTime) {
            // not in the same game...
            return;
        }

        if (!initialized.get()) {
            // au cas ou on fait un create game en cours de partie, le cancel n'a pas forc√©ment le temps de passer dans reset();
            return;
        }
        if (isInfoEnabled) {
            logger.info("{} ({}): expired ", QUESTIONTIMEFRAME, qto.qn);
        }
        doStateTransition(SYNCHROTIME, qto.qn);
    }

    // -----------------------------
    // SynchrotimeTimeout
    // -----------------------------

    /**
     * Schedule the transition from SYNCHROTIME N to QUESTIONTIMEFRAME N+1 or RANKING.
     */
    public class SynchrotimeTimeout extends TimerTask {
        long myGameCreationTime = gameCreationTime;
        private int qn;

        SynchrotimeTimeout(int questionN, Date expirationTime) {
            qn = questionN;
            if (expirationTime.getTime() - currentTimeMillis() < deltaForImmediateRunMs) {
                if (isWarnEnabled) {
                    logger.warn("{} ({}): invoke run() directly to catch up", SYNCHROTIME, questionN);
                }
                run(); // launching it now, hoping we can catch up.
            } else {
                new Timer().schedule(this, expirationTime);
            }
        }

        @Override
        public void run() {
            runSynchrotimeTimeout(this);
        }
    }

    private synchronized void runSynchrotimeTimeout(SynchrotimeTimeout sto) {
        if (sto.myGameCreationTime == 0 || sto.myGameCreationTime != gameCreationTime) {
            // not in the same game...
            return;
        }

        if (!initialized.get()) {
            // au cas ou on fait un create game en cours de partie, le cancel n'a pas forc√©ment le temps de passer dans reset();
            return;
        }

        if (isInfoEnabled) {
            logger.info("{} ({}): expired", SYNCHROTIME, sto.qn);
        }
        if (sto.qn < parameters.getNbQuestions()) {
            doStateTransition(QUESTIONTIMEFRAME, sto.qn + 1);
            return;
        }

        if (sto.qn == parameters.getNbQuestions()) {
            doStateTransition(RANKING, 0, null);
            return;
        }
    }

    // -----------------------------
    // UsiPeriodicTaskTarget Impl
    // -----------------------------

    @Override
    public synchronized void onGameStatus(GameStatus gameStatus) {
        // new game created
        if (gameStatus.getGameCreationTime() > gameCreationTime) {
            if (isWarnEnabled) {
                logger.warn("A more recent game was published, we must use it");
            }
            gameCreationTime = gameStatus.getGameCreationTime();
            reset();
            initGame(gameCreationTime);
            if (isInfoEnabled) {
                logger.info(gameStatus.toString());
            }
            // we can continue... (do not return here)
        }

        if (catchUp(gameStatus)) {
            return;
        }

        if (currentState.isSTART()) {
            // handle regular state machine flow
            if (gameStatus.firstUserLoggedIn()) {
                if (tFirstLogin.compareAndSet(Long.MAX_VALUE, gameStatus.getWhenFirstUserLoggedInMilliseconds())) {
                    doStateTransition(LOGIN, 1, new Date(tFirstLogin.get() + parameters.getLoginTimeoutInMs()));
                }
            }
        }

        // don't do else if here
        if (currentState.isLOGIN()) {
            // l'appel √† cancel() ne pose pas de pb si la task a d√©j√† √©t√© ex√©cut√©e ou annul√©e...
            if (gameStatus.getNbOfUsersLogged() >= parameters.getNbUsersThreshold()) {
                loginTimeout.cancel();
                if (tQuestion1.compareAndSet(Long.MAX_VALUE, currentTimeMillis())) {
                    // On passe avant loginTimeoutTask
                    // si une autre instance a d√©j√† sett√© le tQuestion1, on le r√©cup√®re, sinon on persiste celui de cette instance.
                    // (==> cette instance est la premi√®re √† d√©tecter le maxUserreached)
                    if (gameStatus.getQuestion1TimeframeSTart() < tQuestion1.get()) {
                        tQuestion1.set(gameStatus.getQuestion1TimeframeSTart());
                        if (isWarnEnabled) {
                            logger.warn("Max user threshold was detected by another server");
                        }
                        doStateTransition(QUESTIONTIMEFRAME, 1);
                    } else {
                        if (isWarnEnabled) {
                            logger.warn("Max user threshold detected by this server");
                        }
                        doStateTransition(QUESTIONTIMEFRAME, 1);
                        // on persiste apr√®s le changement d'√©tat, pour √©viter un d√©calage...
                        loginRepository.question1TimeframeSTart(tQuestion1.get());
                    }
                } else {
                    // la loginTimeOutTask a d√©clench√© le changement de state -> on recale tQuestion1 si besoin.
                    // (ce qui ne devrait pas √™tre le cas...)
                    overrideTQ1ifNeeded(gameStatus);
                }

            } else if (gameStatus.getWhenFirstUserLoggedInMilliseconds() < tFirstLogin.get()) {
                tFirstLogin.set(gameStatus.getWhenFirstUserLoggedInMilliseconds());
                loginTimeout.cancel();
                loginTimeout = new LoginTimeout(new Date(tFirstLogin.get() + parameters.getLoginTimeoutInMs()));
            }
        }

        overrideTQ1ifNeeded(gameStatus);
    }

    /**
     * Attempt to jump into the global current state. Handle case of server that is restarted during a game
     * 
     * @param gameStatus
     * @return true if catch was performed
     */
    private boolean catchUp(GameStatus gameStatus) {
        if (currentState.isSTART() && gameStatus.getQuestion1TimeframeSTart() < Long.MAX_VALUE) {
            // we are certainly beyond LOGIN state
            // we can try to go to this state directly.
            CurrentState cs = getCurrentStateFromTQuestion1(gameStatus.getQuestion1TimeframeSTart());

            if (cs.getState().isQUESTIONTIMEFRAME() || cs.getState().isSYNCHROTIME() || cs.getState().isRANKING()) {
                tFirstLogin.set(gameStatus.getWhenFirstUserLoggedInMilliseconds());
                tQuestion1.set(gameStatus.getQuestion1TimeframeSTart());

                switch (cs.getState()) {
                case QUESTIONTIMEFRAME:
                    if (isWarnEnabled) {
                        logger.warn("DIRECT State transition : START -> {} ({})", new Object[] { QUESTIONTIMEFRAME, cs.getQuestionN() });
                    }
                    setState(QUESTIONTIMEFRAME, cs.getQuestionN());
                    questionTimeframeTimeout = new QuestionTimeframeTimeout(cs.getQuestionN(), getExpirationTimeForState(tQuestion1.get(), QUESTIONTIMEFRAME,
                            cs.getQuestionN()));
                    return true;

                case SYNCHROTIME:
                    if (isWarnEnabled) {
                        logger.warn("DIRECT State transition : START -> {} ({})", new Object[] { SYNCHROTIME, cs.getQuestionN() });
                    }
                    setState(SYNCHROTIME, cs.getQuestionN());
                    synchrotimeTimeout = new SynchrotimeTimeout(cs.getQuestionN(), getExpirationTimeForState(tQuestion1.get(), SYNCHROTIME, cs.getQuestionN()));
                    return true;

                case RANKING:
                    if (isWarnEnabled) {
                        logger.warn("DIRECT State transition : START -> RANKING");
                    }
                    setState(RANKING, 0);
                    return true;
                }
            } // else should not happen
        }
        return false;
    }

    private void overrideTQ1ifNeeded(GameStatus gameStatus) {
        if (currentState.isQUESTIONTIMEFRAME() && currentQuestionN == FIRST_QUESTION) {        
            if (gameStatus.getQuestion1TimeframeSTart() < tQuestion1.get()) {
                tQ1StableCounter = 0;
                tQ1OverrideOkCounter++;
                long oldTq1 = tQuestion1.get();            
                tQuestion1.set(gameStatus.getQuestion1TimeframeSTart());
                questionTimeframeTimeout.cancel();
                questionTimeframeTimeout = new QuestionTimeframeTimeout(currentQuestionN, getExpirationTimeForState(tQuestion1.get(), currentState, currentQuestionN));
    
                if (isWarnEnabled) {
                    logger.warn("tQ1 overriden for {} nth time. new={}, old={}, delta={}", new Object[]{ tQ1OverrideOkCounter, tQuestion1.get(), oldTq1, oldTq1 - tQuestion1.get() });
                }
            } else if (gameStatus.getQuestion1TimeframeSTart() == tQuestion1.get()) {
                tQ1StableCounter++;
                
                if (tQ1StableCounter >= tQ1StabilizationReachedWhenStableCounterValueIs) {
                    if (isWarnEnabled) {
                        logger.warn("Got same tQ1 {} times in a raw: {}. Now considered stable => disable game status poller during 1st QuestionTimeFrame", tQ1StableCounter, tQuestion1.get()) ;
                    }                    
                    gameStatusPollerTask.disableNotifyDuringFirstQuestionTimeFrame();
                }
            }
        }
    }

    @Override
    public int getQuestion1Counter() {
        return question1Counter.get();
    }

    @Override
    public int getRankingCounter() {
        return rankingCounter.get();
    }

    @Override
    public void incrementRankingCounter() {
        rankingCounter.incrementAndGet();
    }

    // ------------------------------------------
    // Utils
    // ------------------------------------------

    /**
     * Should be called, just after a transition occurs. The purpose is to determine if the timer are on time.
     */
    private void logCurrentStateElapsedTime() {
        CurrentState expectedState = getCurrentStateFromTQuestion1(tQuestion1.get());

        if (currentState != expectedState.getState() || currentQuestionN != expectedState.getQuestionN()) {
            if (isWarnEnabled) {
                logger.warn("currentState is {} ({}) but expecting {} ({})", new Object[] { currentState, currentQuestionN, expectedState.getState(),
                        expectedState.getQuestionN() });
            }
        }

        long timeout = 0;
        if (expectedState.getState().isQUESTIONTIMEFRAME()) {
            timeout = parameters.getQuestionTimeframeInMs();
        } else if (expectedState.getState().isSYNCHROTIME()) {
            timeout = parameters.getSynchrotimeInMs();
        }
        if (expectedState.getState().isQUESTIONTIMEFRAME() || expectedState.getState().isSYNCHROTIME()) {
            if (isInfoEnabled) {
                logger.info("{} ({}) supposed to run since: {}ms", new Object[] { //
                        expectedState.getState(), //
                                expectedState.getQuestionN(), //
                                currentTimeMillis() //
                                        - (getExpirationTimeForState(tQuestion1.get(), expectedState.getState(), expectedState.getQuestionN()).getTime() //
                                        - timeout) });
            }
            return;
        }

        if (expectedState.getState().isRANKING()) {
            if (isInfoEnabled) {
                logger.info("{} supposed to run since: {}ms", expectedState.getState(),
                        currentTimeMillis() - getExpirationTimeForState(tQuestion1.get(), SYNCHROTIME, parameters.getNbQuestions()).getTime());
            }
        }
    }

    /**
     * Return the expected state based on tq1.
     * 
     * @param tq1
     *            the entry time in QuestionTimeframe with questionN == FIRST_QUESTION
     * @return
     */
    final protected CurrentState getCurrentStateFromTQuestion1(long tq1) {
        CurrentState cs = new CurrentState();
        long elapsed = currentTimeMillis() - tq1;

        if (elapsed >= parameters.getNbQuestions() * (parameters.getQuestionTimeframeInMs() + parameters.getSynchrotimeInMs())) {
            // assuming RANKING last forever
            cs.setState(RANKING);
            return cs;
        }

        cs.setQuestionN(1 + (int) (elapsed / (parameters.getQuestionTimeframeInMs() + parameters.getSynchrotimeInMs())));
        cs.setState((elapsed % (parameters.getQuestionTimeframeInMs() + parameters.getSynchrotimeInMs())) < parameters.getQuestionTimeframeInMs() ? QUESTIONTIMEFRAME
                : SYNCHROTIME);
        return cs;
    }

    /**
     * Calculates the expected time for the transition to the passed state for the given questionN
     * 
     * @param tq1
     *            the entry time in QuestionTimeframe with questionN == 1
     * @param state
     * @param questionN
     * @return
     */
    final protected Date getExpirationTimeForState(long tq1, State state, int questionN) {
        if (tq1 < 0) {
            throw new IllegalArgumentException("invalid invokation, tq1 is not valid");
        }

        if (state.isQUESTIONTIMEFRAME() && questionN >= FIRST_QUESTION && questionN <= parameters.getNbQuestions()) {
            return new Date(tq1 + questionN * parameters.getQuestionTimeframeInMs() + (questionN - 1) * parameters.getSynchrotimeInMs());
        }

        if (state.isSYNCHROTIME() && questionN >= FIRST_QUESTION && questionN <= parameters.getNbQuestions()) {
            return new Date(tq1 + questionN * (parameters.getQuestionTimeframeInMs() + parameters.getSynchrotimeInMs()));
        }

        throw new IllegalArgumentException("invalid invokation " + tq1);
    }
}