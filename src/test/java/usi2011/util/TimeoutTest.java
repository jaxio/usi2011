package usi2011.util;

import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;

public class TimeoutTest {
    @Test
    public void valid() throws InterruptedException {
        long now = currentTimeMillis();
        Timeout timeout = new Timeout("test", now, SECONDS.toMillis(1));

        assertThat(timeout.startTimeInMilliSeconds()).isEqualTo(now);
        assertThat(timeout.startTimeInSeconds()).isEqualTo(now / 1000);

        assertThat(timeout.durationInMilliSeconds()).isEqualTo(1000);
        assertThat(timeout.durationInSeconds()).isEqualTo(1);

        assertThat(timeout.endsAtInMilliSeconds()).isEqualTo(now + 1000);
        assertThat(timeout.endsAtInSeconds()).isEqualTo((now + 1000) / 1000);

        assertThat(timeout.remainingInMilliseconds()).isPositive();
        long remaining = timeout.remainingInMilliseconds();

        MILLISECONDS.sleep(100);

        assertThat(timeout.sinceInMilliseconds()).isPositive();
        long since = timeout.sinceInMilliseconds();
        assertThat(timeout.expired()).isFalse();
        assertThat(timeout.running()).isTrue();

        SECONDS.sleep(2);

        assertThat(timeout.remainingInMilliseconds()).isNegative();
        assertThat(timeout.remainingInMilliseconds()).isLessThan(remaining);
        assertThat(timeout.sinceInMilliseconds()).isPositive();
        assertThat(timeout.sinceInMilliseconds()).isGreaterThanOrEqualTo(since);
        assertThat(timeout.expired()).isTrue();
        assertThat(timeout.running()).isFalse();
    }
}