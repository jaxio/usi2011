package usi2011.util;

import static java.lang.Long.MAX_VALUE;
import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.slf4j.LoggerFactory.getLogger;
import static usi2011.util.LogUtil.isInfoEnabled;

import org.slf4j.Logger;

import com.google.common.base.Objects;

public class Timeout implements Comparable<Timeout> {
    private static final Logger logger = getLogger(Timeout.class);

    private final String name;
    private long durationInMilliSeconds;
    private long startTimeInMilliseconds;

    public Timeout(String name, long startTimeInMilliseconds, long durationInMilliSeconds) {
        this.name = name;
        this.durationInMilliSeconds = durationInMilliSeconds;
        this.startTimeInMilliseconds = startTimeInMilliseconds;
    }

    public Timeout(String name, long durationInMilliseconds) {
        this(name, MAX_VALUE - 1, durationInMilliseconds);
    }

    public void start(long startTimeInMilliseconds) {
        this.startTimeInMilliseconds = startTimeInMilliseconds;
        if (isInfoEnabled) {
            logger.info("Starting {}, timeout will occur in {}s ", name, endsInSeconds());
        }
    }

    public long startTimeInMilliSeconds() {
        return startTimeInMilliseconds;
    }

    public long startTimeInSeconds() {
        return startTimeInMilliseconds / SECONDS.toMillis(1);
    }

    public long durationInMilliSeconds() {
        return durationInMilliSeconds;
    }

    public long durationInSeconds() {
        return durationInMilliSeconds / SECONDS.toMillis(1);
    }

    public long endsAtInMilliSeconds() {
        return startTimeInMilliseconds + durationInMilliSeconds;
    }

    public long endsAtInSeconds() {
        return endsAtInMilliSeconds() / SECONDS.toMillis(1);
    }

    public long endsInMilliSeconds() {
        return endsAtInMilliSeconds() - currentTimeMillis();
    }

    public long endsInSeconds() {
        return endsInMilliSeconds() / SECONDS.toMillis(1);
    }

    public long sinceInMilliseconds() {
        return currentTimeMillis() - startTimeInMilliseconds;
    }

    public long sinceInSeconds() {
        return sinceInMilliseconds() / SECONDS.toMillis(1);
    }

    public long remainingInMilliseconds() {
        return durationInMilliSeconds - sinceInMilliseconds();
    }

    public long remainingInSeconds() {
        return remainingInMilliseconds() / SECONDS.toMillis(1);
    }

    public boolean expired() {
        return remainingInMilliseconds() < 0;
    }

    public boolean running() {
        return !expired();
    }

    @Override
    public String toString() {
        if (expired()) {
            return name + " expired since " + remainingInMilliseconds() + "ms";
        } else if (remainingInMilliseconds() > DAYS.toMillis(1)) {
            return name + " not initialized yet";
        } else {
            return name + " started " + sinceInMilliseconds() + "ms ago " + remainingInMilliseconds() + "ms before expiration";
        }
    }

    @Override
    public int compareTo(Timeout other) {
        return other == null ? -1 : hashCode() - other.hashCode();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(startTimeInMilliseconds, durationInMilliSeconds);
    }

    @Override
    public boolean equals(Object other) {
        return other == null || !(other instanceof Timeout) ? false : compareTo((Timeout) other) == 0;
    }

}
