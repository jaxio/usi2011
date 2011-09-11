package usi2011.task;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.Runtime.getRuntime;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static usi2011.util.LogUtil.isDebugEnabled;
import static usi2011.util.LogUtil.isInfoEnabled;
import static usi2011.util.LogUtil.isWarnEnabled;

import java.util.Date;
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

import usi2011.domain.Question;
import usi2011.domain.UserScoreHistory;
import usi2011.repository.ScoreRepository;
import usi2011.repository.ScoreRepository.Score;
import usi2011.statemachine.StateMachine;
import usi2011.statemachine.StateMachine.CurrentState;
import usi2011.util.NamedThreadFactory;

/**
 * Polls the repository periodically to get login information.
 * <p>
 * This is needed for instances that crashed and need to get back on feed with non ephemeral data.
 */
@Component
public class BatchScoreUpdateTask extends TimerTask {
    private static final Logger logger = LoggerFactory.getLogger(BatchScoreUpdateTask.class);
    private static final long NOW = 0;
    
    @Value("${batchScoreUpdateTask.periodMs}")
    private int periodMs;
    @Value("${batchScoreUpdateTask.batchSize}")
    private int batchSize;
    @Autowired
    private ScoreRepository scoreRepository;
    private final LinkedBlockingQueue<Score> queue = new LinkedBlockingQueue<Score>();
    private AtomicBoolean working = new AtomicBoolean(false);

    @Value("${batchScoreUpdateTask.nThreads}")
    private int nThreads;
    private ExecutorService executorService;

    @Autowired
    private StateMachine stateMachine;
    @Value("${http.enable.bench:false}")
    private boolean httpBenchEnabled;
    
    @Value("${force.garbagecollection.in.middle.of.synchrotime:false}")
    boolean forceGarbageCollectionInMiddleOfSynchroTime;    

    private Boolean previousRunOK;
    
    /**
     * Pass some score info for asynch update.
     */
    public void updateScoreAsynch(UserScoreHistory userScoreHistory, Question question) {
        queue.add(new Score(userScoreHistory, question));
    }

    @PostConstruct
    void start() {
        if (httpBenchEnabled) {
            logger.info("{} not started as we are in http bench mode", getClass().getSimpleName());
            return;
        }

        if (nThreads <= 0) {
            nThreads = getRuntime().availableProcessors();
        }
        if (isWarnEnabled) {
            logger.warn("THREAD INFO: batchScoreUpdateTask.nThreads=" + nThreads);
        }

        executorService = newFixedThreadPool(nThreads, new NamedThreadFactory(getClass().getSimpleName()));
        new Timer(getClass().getSimpleName()).schedule(this, NOW, periodMs);
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
        // we update only during synchrotime to save CPU & IO for /api/answer/x
        if (cs != null && (cs.getState().isSYNCHROTIME() || cs.getState().isRANKING())) { // "ranking" so we can save lingering data...
            if (working.compareAndSet(false, true)) {
                try {
                    process();
                    if (previousRunOK != null && !previousRunOK) {
                        previousRunOK = true;
                        logger.error("Back to normal");
                    }                    
                } catch (Throwable t) {
                    logger.error("Critical: ", t);
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

    int lastBatchSize = 0;
    private void process() {
        final List<Score> batch = buildBatch(batchSize);
        if (!batch.isEmpty()) {
            executorService.execute(new Runnable() {
                public void run() {
                    int batchSize = batch.size();
                    lastBatchSize = batchSize;
                    
                    if (isWarnEnabled) {
                        logger.warn("About to publish {} answers", batchSize);
                    }
                    
                    scoreRepository.updateScores(batch);
                    
                    if (isWarnEnabled) {
                        logger.warn("Done publishing  {} answers", batchSize);
                    }                    
                }
            });
        } else if (lastBatchSize > 0 ) {
            // current batch size is empty but last was not... it was probably the last
            // batch saved for the synchrotime state... let's invoke GC...
            lastBatchSize = 0;
            // we can now invoke GC...
            
            if (forceGarbageCollectionInMiddleOfSynchroTime) {
                logger.warn("call gc: total={}, free={}", Runtime.getRuntime().totalMemory(), Runtime.getRuntime().freeMemory());
                System.gc();
                logger.warn("done gc: total={}, free={}", Runtime.getRuntime().totalMemory(), Runtime.getRuntime().freeMemory());
            }            
        }
    }

    private List<Score> buildBatch(int size) {
        final List<Score> batch = newArrayList();
        Score score = null;
        while ((score = queue.poll()) != null) {
            batch.add(score);
            if (batch.size() == size) {
                return batch;
            }
        }
        return batch;
    }
}
