package usi2011.http.support;

import static usi2011.util.LogUtil.isInfoEnabled;
import static usi2011.util.LogUtil.isWarnEnabled;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AsyncWriteResponseTask extends TimerTask {
    private static final Logger logger = LoggerFactory.getLogger(AsyncWriteResponseTask.class);
    private static final long NOW = 0;
    @Value("${asyncWriteResponse.enable}")
    private boolean enable;
    @Value("${asyncWriteResponseTask.periodMs}")
    private int periodInMs = 10;

    public static interface WriteResponseCallback {
        void write();
    }

    private final AtomicBoolean working = new AtomicBoolean(false);
    private final LinkedBlockingQueue<WriteResponseCallback> queue = new LinkedBlockingQueue<WriteResponseCallback>();

    @PostConstruct
    public void start() {
        if (enable) {
            new Timer(getClass().getSimpleName()).schedule(this, NOW, periodInMs);
            if (isInfoEnabled) {
                logger.info("{} started", getClass().getSimpleName());
            }
        }
    }

    public void write(WriteResponseCallback callback) {
        queue.add(callback);
    }

    @Override
    public void run() {
        if (working.compareAndSet(false, true)) {
            try {
                WriteResponseCallback callback = null;
                while ((callback = queue.poll()) != null) {
                    callback.write();
                }
            } catch (Exception e) {
                if (isWarnEnabled) {
                    logger.warn("Could not write response", e);
                }
            } finally {
                working.set(false);
            }
        }
    }
}
