package usi2011.task;

import static usi2011.util.LogUtil.isInfoEnabled;
import static usi2011.util.LogUtil.isWarnEnabled;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import usi2011.domain.GameStatus;
import usi2011.repository.GameStatusRepository;
import usi2011.service.TwitterService;
import usi2011.statemachine.StateMachine;
import usi2011.statemachine.StateMachine.CurrentState;

/**
 * Polls the repository periodically to get login information.
 * <p>
 * This is needed for instances that crashed and need to get back on feed with non ephemeral data.
 */
@Component
public class GameStatusPollerTask extends TimerTask {
    private static final Logger logger = LoggerFactory.getLogger(GameStatusPollerTask.class);
    private static final long NOW = 0;
    @Autowired
    private PeriodicTask usiPeriodicTaskTarget;
    @Value("${gameStatusRepositoryPollerTask.periodMs}")
    private int periodMs;

    @Autowired
    private StateMachine stateMachine;
    @Autowired
    private GameStatusRepository gameStatusRepository;
    @Autowired
    private TwitterService twitterService;
    @Value("${http.enable.bench:false}")
    private boolean httpBenchEnabled;

    private long lastGameCreatedMs = -1l;
    private int lastQuestion1CounterSaved = -1;
    private int lastRankingCounterSaved = -1;
    private boolean tweetSent;

    private AtomicBoolean working = new AtomicBoolean(false);
    private Boolean previousRunOK;
    
    private boolean notifyDuringFirstQuestionTimeFrame = true;

    @PostConstruct
    public void start() {
        if (httpBenchEnabled) {
            logger.info("{} not started as we are in http bench mode", getClass().getSimpleName());
            return;
        }
        new Timer(getClass().getSimpleName()).schedule(this, NOW, periodMs);
        if (isInfoEnabled) {
            logger.info("{} started", getClass().getSimpleName());
        }
    }
    
    
    /**
     * Called by state machine once it is no longer interested in game status during TQ1
     * @param notifyDuringFirstQuestionTimeFrame
     */
    public void disableNotifyDuringFirstQuestionTimeFrame() {
        this.notifyDuringFirstQuestionTimeFrame = false;
    }

    @Override
    public void run() {
        final CurrentState cs = stateMachine.getCurrentState();
        
        // should we skip QuestionTime Frame 1 ?        
        if (cs != null && cs.getState().isQUESTIONTIMEFRAME() && cs.getQuestionN() == 1 && !notifyDuringFirstQuestionTimeFrame) {
            // save CPU & IO for /api/answer ...
            return;
        }
        
        // skip Question Time Frame 2 to 20
        if (cs != null && cs.getState().isQUESTIONTIMEFRAME() && cs.getQuestionN() > 1) {
            // save CPU & IO for /api/answer ...
            return;
        }

        if (working.compareAndSet(false, true)) {
            try {
                // we publish the logged in counter only if we have a game...
                if (lastGameCreatedMs > 0) {
                    // utilise une variable locale pour pr√©venir une modification concurrente
                    int question1Counter = stateMachine.getQuestion1Counter();
                    if (question1Counter != lastQuestion1CounterSaved) {
                        // on persiste le compteur local...
                        gameStatusRepository.loggedUsers(question1Counter);
                        lastQuestion1CounterSaved = question1Counter;
                    }
                }

                // let's see if we have a game
                final GameStatus gameStatus = gameStatusRepository.getGameStatus();
                if (gameStatus == null) {
                    return;
                }

                isValid(gameStatus);

                // if we detect a new game, we reset our value.
                if (gameStatus.getGameCreationTime() > 0 && gameStatus.getGameCreationTime() > lastGameCreatedMs) {
                    if (isWarnEnabled) {
                        logger.warn("new gameCreationTime found ({}), we reset our local values", gameStatus.getGameCreationTime());
                    }
                    lastGameCreatedMs = gameStatus.getGameCreationTime();
                    lastQuestion1CounterSaved = -1;
                    lastRankingCounterSaved = -1;
                    tweetSent = false;
                    notifyDuringFirstQuestionTimeFrame = true;
                } 
                // no new game, we can process ranking counter & twitter...
                else if (cs.getState().isRANKING()) {
                    
                    int localRankingCounter = stateMachine.getRankingCounter();

                    if (lastRankingCounterSaved != localRankingCounter) {
                        gameStatusRepository.publishLocalRankingRequestCount(gameStatus.getGameCreationTime(), localRankingCounter);
                        lastRankingCounterSaved = localRankingCounter;
                    }

                    int globalRankingCounter = gameStatusRepository.getGlobalRankingRequestCount(gameStatus.getGameCreationTime());
                    if (globalRankingCounter > 0 && 100 * globalRankingCounter >= 80 * gameStatus.getNbOfUsersLogged() && !tweetSent) {
                        if(gameStatus.getNbTweetSent() == 0) {
                            if (isWarnEnabled) {
                                logger.warn("We can tweet! globalRankingCounter={} nbOfUsersLogged={} gameCreationTime={}", new Object[] { globalRankingCounter,
                                        gameStatus.getNbOfUsersLogged(), gameStatus.getGameCreationTime() });
                            }
                            twitterService.tweet("notre Appli supporte " + gameStatus.getNbOfUsersLogged() + " joueurs #challengeUSI2011 gameid=" + gameStatus.getGameCreationTime());
                            tweetSent = true;
                            // trying to limit the number of tweet sent by other machines...
                            // also useful if we restart this server or another to avoid re-sending the tweet...
                            gameStatusRepository.tweetSent();
                            logger.warn("Tweet sent from this machine for the game {}", gameStatus.getGameCreationTime());
                            
                        } else {
                            tweetSent = true; // to avoid logging the message below on next round...
                            logger.warn("Tweet already sent {} times for the game {}, no need to send it again...", gameStatus.getNbTweetSent(), gameStatus.getGameCreationTime());
                        }
                    }
                }

                // notify the state machine (what an horrible interface :-)
                usiPeriodicTaskTarget.onGameStatus(gameStatus);
                
                // log cosmetic
                if (previousRunOK != null && !previousRunOK) {
                    previousRunOK = true;
                    logger.error("Back to normal");
                }
            } catch (Throwable t) {
                logger.error("Critical:", t);
                previousRunOK = false;
                try {
                    // do not use wait... it hangs!
                    Thread.sleep(periodMs * 20); // to avoid logging tons of exception
                } catch (InterruptedException ie) {                    
                }
            }
            finally {
                working.set(false);
            }
        }
    }

    private boolean isValid(GameStatus gameStatus) {
        // let's make sure the values are consistent
        final GameStatus def = GameStatus.DEFAULT;

        if (gameStatus.getGameCreationTime() != def.getGameCreationTime()) {
            if (gameStatus.getWhenFirstUserLoggedInMilliseconds() < gameStatus.getGameCreationTime()) {
                if (isWarnEnabled) {
                    logger.warn("Limit case reached: whenFirstUserLoggedInMilliseconds < gameCreationTime : \n" + gameStatus);
                }
                return false;
            }

            if (gameStatus.getQuestion1TimeframeSTart() < gameStatus.getGameCreationTime()) {
                if (isWarnEnabled) {
                    logger.warn("Limit case reached: question1TimeframeSTart < gameCreationTime : \n" + gameStatus);
                }
                return false;
            }

            if (gameStatus.getQuestion1TimeframeSTart() < gameStatus.getWhenFirstUserLoggedInMilliseconds()) {
                if (isWarnEnabled) {
                    logger.warn("Limit case reached: question1TimeframeSTart < whenFirstUserLoggedInMilliseconds : \n" + gameStatus);
                }
                return false;
            }

        } else if (gameStatus.getGameCreationTime() == def.getGameCreationTime()) {
            if (gameStatus.getWhenFirstUserLoggedInMilliseconds() != def.getWhenFirstUserLoggedInMilliseconds()) {
                if (isWarnEnabled) {
                    logger.warn("Limit case reached: no gameCreationTime but whenFirstUserLoggedInMilliseconds present! : \n" + gameStatus);
                }
                return false;
            }

            if (gameStatus.getQuestion1TimeframeSTart() != def.getQuestion1TimeframeSTart()) {
                if (isWarnEnabled) {
                    logger.warn("Limit case reached: no gameCreationTime but question1TimeframeSTart present! : \n" + gameStatus);
                }
                return false;
            }

            if (gameStatus.getNbOfUsersLogged() != def.getNbOfUsersLogged()) {
                if (isWarnEnabled) {
                    logger.warn("Limit case reached: no gameCreationTime but nbOfUsersLogged present! : \n" + gameStatus);
                }
                return false;
            }
        }
        return true;
    }
}
