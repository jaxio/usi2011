package usi2011.task;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static usi2011.util.LogUtil.isInfoEnabled;
import static usi2011.util.LogUtil.isWarnEnabled;
import static usi2011.util.Specifications.MAX_NUMBER_OF_QUESTIONS;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import usi2011.domain.Parameters;
import usi2011.domain.Ranking.UserScore;
import usi2011.domain.UserScoreHistory;
import usi2011.repository.ScoreRepository;
import usi2011.statemachine.StateMachine;
import usi2011.statemachine.StateMachine.CurrentState;
import usi2011.util.NamedThreadFactory;

/**
 * Polls the repository periodically to get login information.
 * <p>
 * This is needed for instances that crashed and need to get back on feed with non ephemeral data.
 */
@Component
public class BatchRankingPublisherTask extends TimerTask {
    private static final Logger logger = LoggerFactory.getLogger(BatchRankingPublisherTask.class);
    private static final long NOW = 0;
    @Value("${http.enable.bench:false}")
    private boolean httpBenchEnabled;
    @Value("${batchRankingPublisherTask.periodMs}")
    private int periodMs;
    @Value("${batchRankingPublisherTask.batchSize}")
    private int batchSize;
    @Autowired
    private ScoreRepository scoreRepository;
        
    private final LinkedBlockingQueue<UserScoreHistory> queue = new LinkedBlockingQueue<UserScoreHistory>();
    private final AtomicBoolean working = new AtomicBoolean(false);

    @Value("${batchRankingPublisherTask.nThreads}")
    private int nThreads;
    private ExecutorService executorService;

    @Autowired
    private StateMachine stateMachine;
    
    private Parameters parameters;    
    private Boolean previousRunOK;

    /**
     * Pass some score info for asynch update.
     */
    public void publishRankingAsynch(UserScoreHistory ush) {
        queue.add(ush);
    }
    
    /**
     * Invoked from state machine upon initGame.
     * @param parameters
     */
    public void setParameters(Parameters parameters) {
        this.parameters = parameters;
    }
    

    @PostConstruct
    void start() {
        if (httpBenchEnabled) {
            logger.info("{} not started as we are in http bench mode", getClass().getSimpleName());
            return;
        }
        if (nThreads <= 0) {
            nThreads = Runtime.getRuntime().availableProcessors();
        }
        if (isWarnEnabled) {
            logger.warn("THREAD INFO: batchRankingPublisherTask.nThreads=" + nThreads);
        }

        executorService = newFixedThreadPool(nThreads, new NamedThreadFactory(getClass().getSimpleName()));
        new Timer(getClass().getSimpleName()).schedule(this, NOW + 25, periodMs); // delayed from batch score task
        if (isInfoEnabled) {
            logger.info("{} started", getClass().getSimpleName());
        }
    }

    /**
     * create a thread of {@link #batchSize} scores to be updated
     */
    @Override
    public void run() {
        final CurrentState cs = stateMachine.getCurrentState();
        // we update only during last question timeframe & last synchrotime & ranking
        if (parameters != null && cs != null && (
                (cs.getQuestionN() == parameters.getNbQuestions() && (cs.getState().isQUESTIONTIMEFRAME() || cs.getState().isSYNCHROTIME())))
                || cs.getState().isRANKING()
        ) {
            if (working.compareAndSet(false, true)) {
                try {
                    process();
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
                } finally {
                    working.set(false);
                }
            }
        }
    }

    private void process() {
        final List<UserScore> batch = buildBatch(batchSize);
        if (!batch.isEmpty()) {
            executorService.execute(new Runnable() {
                public void run() {
                    int batchSize = batch.size();                    
                    if (isWarnEnabled) {
                        logger.warn("About to publish {} rankings", batchSize);
                    }
                    
                    scoreRepository.publishRankings(batch);
                    
                    if (isWarnEnabled) {
                        logger.warn("Done publishing  {} rankings", batchSize);
                    }                    
                }
            });
        }
    }

    private List<UserScore> buildBatch(int size) {
        final List<UserScore> batch = newArrayList();
        UserScoreHistory ush = null;
        if (parameters == null) {
            if (isWarnEnabled) {
                logger.warn("Sould not happen: parameters is null...");
            }
            return batch;
        }
        
        int nbQ = parameters.getNbQuestions();
        while ((ush = queue.poll()) != null) {
            batch.add(new UserScore("" + ush.getScore(nbQ), ush.getLastName(), ush.getFirstName(), ush.getEmail()));
            if (batch.size() == size) {
                return batch;
            }
        }
        return batch;
    }
}
