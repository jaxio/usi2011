package usi2011.statemachine.support;

import static java.lang.Runtime.getRuntime;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.slf4j.LoggerFactory.getLogger;
import static usi2011.util.LogUtil.isWarnEnabled;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import usi2011.util.NamedThreadFactory;

@Component
public class CallbackFlusher {
    private static final Logger logger = getLogger(CallbackFlusher.class);

    private ExecutorService executorService;

    @Value("${callbackFlusher.nThreads}")
    private int nThreads;

    @PostConstruct
    public void init() {
        if (nThreads <= 0) {
            nThreads = getRuntime().availableProcessors(); // total nb of core!
        }
        executorService = newFixedThreadPool(nThreads, new NamedThreadFactory(getClass().getSimpleName()));
    }

    @Async
    public void asyncSuccess(final LinkedBlockingQueue<StateCallback> callbacks, final int questionId) {
        int size = callbacks.size();
        if (isWarnEnabled && size != 0) {
            logger.warn("Flushing {} callbacks for question {}. nThreads={}", new Object[] { size, questionId, nThreads });
        }
        StateCallback callback = null;

        try {
            if (nThreads == 1) {
                size = 0;
                while ((callback = callbacks.poll(1, TimeUnit.SECONDS)) != null) {
                    callback.success();
                    size++;
                }
                logger.warn("Done flushing {} callbacks for question {}", size, questionId);
                callbacks.clear();
            } else {
                while ((callback = callbacks.poll(1, TimeUnit.SECONDS)) != null) {
                    final StateCallback callbackToFlush = callback;
                    executorService.execute(new Runnable() {
                        @Override
                        public void run() {
                            callbackToFlush.success();
                        }
                    });
                }
            }
        } catch (InterruptedException e) {
            if (isWarnEnabled) {
                logger.warn(getClass().getSimpleName() + " was interrupted");
            }
        }
    }

    
    final public void asyncSuccessArray(final StateCallback callbacks[], final int questionId) {
        asyncSuccessArrayWithCallable(callbacks, questionId);
    }

    // @Async
    public void asyncSuccessArraySimple(final StateCallback callbacks[], final int questionId) {
        logger.warn("Flushing array callbacks for question {}", questionId);
        int size = callbacks.length;
        int counter = 0;
        for (int i = 0; i < size; i++) {
            if (callbacks[i] != null) {
                callbacks[i].success();
                callbacks[i] = null;
            } else {
                counter = i;
                break;
            }
        }
        logger.warn("Done flushing {} callbacks(array) for question {}", counter, questionId);
    }

    public void asyncSuccessArrayWithCallable(final StateCallback callbacks[], final int questionId) {
        final long t0 = System.currentTimeMillis();
        
        class Flusher implements Callable<Flusher> {
            private final int start;
            private final int inc;
            private int counter = 0;
            private long duration = 0;

            public Flusher(int start, int inc) {
                this.start = start;
                this.inc = inc;
            }

            @Override
            public Flusher call() {
                int size = callbacks.length;
                for (int i = start; i < size; i += inc) {
                    if (callbacks[i] != null) {
                        callbacks[i].success();
                        counter++;
                        callbacks[i] = null;
                    } else {
                        break;
                    }
                }
                duration = System.currentTimeMillis() - t0;
                return this;
            }

            public int count() {
                return counter;
            }

            public long duration() {
                return duration;
            }
        }
        ;

        List<Callable<Flusher>> tasks = new ArrayList<Callable<Flusher>>();
        for (int i = 0; i < nThreads; i++) {
            tasks.add(new Flusher(i, nThreads));
        }

        try {
            List<Future<Flusher>> results = executorService.invokeAll(tasks);

            long durationMax = 0;
            int counter = 0;
            for (Future<Flusher> res : results) {
                Flusher f = res.get();

                if (f.duration() > durationMax) {
                    durationMax = f.duration();
                }

                counter += f.count();
            }
            logger.warn("Done flushing {} callbacks(array+ {} poolthreads) in {} ms for question {}", new Object[] { counter, nThreads, durationMax, questionId });
        } catch (Exception e) {

        }
    }

    public void asyncSuccessArrayWithThreads(final StateCallback callbacks[], final int questionId) {
        class Flusher extends Thread {
            private final int start;
            private final int inc;
            private int counter = 0;

            public Flusher(int start, int inc) {
                this.start = start;
                this.inc = inc;
            }

            @Override
            public void run() {
                int size = callbacks.length;
                for (int i = start; i < size; i += inc) {
                    if (callbacks[i] != null) {
                        callbacks[i].success();
                        counter++;
                        callbacks[i] = null;
                    }
                }
            }

            public int count() {
                return counter;
            }
        }
        ;
        logger.warn("Flushing array callbacks for question {}", questionId);

        Flusher f0 = new Flusher(0, 4);
        Flusher f1 = new Flusher(1, 4);
        Flusher f2 = new Flusher(2, 4);
        Flusher f3 = new Flusher(3, 4);
        f0.start();
        f1.start();
        f2.start();
        f3.start();
        try {
            f0.join();
            f1.join();
            f2.join();
            f3.join();
            int counter = f0.count() + f1.count() + f2.count() + f3.count();
            logger.warn("Done flushing {} callbacks(array) for question {}", counter, questionId);
        } catch (Exception e) {
            //
        }
    }

}