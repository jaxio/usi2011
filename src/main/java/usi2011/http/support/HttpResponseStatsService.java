package usi2011.http.support;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.Math.max;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.slf4j.LoggerFactory.getLogger;
import static usi2011.util.LogUtil.isInfoEnabled;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.PostConstruct;

import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * periodically grabs the stats from {@link #HttpResponseHelper()} and dumps the
 * <ul>
 * <li>total</li>
 * <li>current stat</li>
 * <li>the highest</li>
 * <li>average on the last {@link #AVERAGE_STAT_FOR_LAST_N_SECONDS} seconds</li>
 * </ul>
 */
@Component
public final class HttpResponseStatsService extends TimerTask {
    private static final Logger logger = getLogger(HttpResponseStatsService.class);
    private static final int AVERAGE_STAT_FOR_LAST_N_SECONDS = 5;
    private HttpResponseStats live = new HttpResponseStats();
    private HttpResponseStats totals = new HttpResponseStats();
    private HttpResponseStats highestRate = new HttpResponseStats();
    private HttpResponseStats highestSecond = new HttpResponseStats();
    private final List<HttpResponseStats> lastStats = newArrayList();

    @Value("${enable.http.statistics:false}")
    boolean enableHttpStatistics;

    @PostConstruct
    public void start() {
        if (enableHttpStatistics && isInfoEnabled) {
            new Timer(HttpResponseService.class.getSimpleName()).schedule(this, 0, SECONDS.toMillis(1));
                logger.info("{} started", getClass().getSimpleName());
        }
    }

    public void newResponse(final HttpResponseStatus status) {
        if (enableHttpStatistics) {
            live.newResponse(status);
        }
    }

    public void reset() {
        totals = new HttpResponseStats();
        highestRate = new HttpResponseStats();
        highestSecond = new HttpResponseStats();
        lastStats.clear();
    }

    @Override
    public void run() {
        final HttpResponseStats current = live;
        lastStats.add(current);
        if (current.count() == 0) {
            return;
        }
        live = new HttpResponseStats();
        totals.addCounters(current);
        final HttpResponseStats rate = rate(lastStats);

        final List<String> vars = newArrayList();
        vars.add(now());
        vars.addAll(params(totals));
        vars.addAll(params(highestSecond));
        vars.addAll(params(highestRate));
        vars.addAll(params(current));
        vars.addAll(params(rate));

        logger.info("\n" //
                + "+--------------+---------+---------+---------+---------+---------+---------+---------+\n" //
                + "| http status  |   200   |   201   |   400   |   401   |   404   |   405   |   500   |\n" //
                + "|   {}   |   ok    | created |bad reque|unauthori|not found|wrongmeth|internalE|\n" //
                + "+--------------+---------+---------+---------+---------+---------+---------+---------+\n" //
                + "| totals       | {} | {} | {} | {} | {} | {} | {} |\n" //
                + "| highest s    | {} | {} | {} | {} | {} | {} | {} |\n" //
                + "| highest rate | {} | {} | {} | {} | {} | {} | {} |\n" //
                + "| last second  | {} | {} | {} | {} | {} | {} | {} |\n" //
                + "| rate last 5s | {} | {} | {} | {} | {} | {} | {} |\n" //
                + "+--------------+---------+---------+---------+---------+---------+---------+---------+\n", //
                vars.toArray(new String[0]));

        // update max
        if (current.hasHigherStatThan(highestSecond)) {
            highestSecond = current;
        }
        if (rate.hasHigherStatThan(highestRate)) {
            highestRate = rate;
        }
    }

    private List<String> params(HttpResponseStats stat) {
        return newArrayList(format("% 7d", stat.ok), format("% 7d", stat.created), format("% 7d", stat.badRequest), format("% 7d", stat.unauthorized),
                format("% 7d", stat.notFound), format("% 7d", stat.methodNotAllowed), format("% 7d", stat.internalServerError));
    }

    private HttpResponseStats rate(final List<HttpResponseStats> stats) {
        final HttpResponseStats rate = new HttpResponseStats();
        final int toBeRemoved = max(stats.size() - AVERAGE_STAT_FOR_LAST_N_SECONDS, 0);
        for (int i = 0; i < toBeRemoved; i++) {
            lastStats.remove(0);
        }
        int count = lastStats.size();
        for (HttpResponseStats stat : stats) {
            rate.addCounters(stat);
        }
        if (count == 0) {
            count = 1;
        }
        rate.ok /= count;
        rate.created /= count;
        rate.badRequest /= count;
        rate.notFound /= count;
        rate.unauthorized /= count;
        rate.internalServerError /= count;
        rate.methodNotAllowed /= count;
        return rate;
    }

    private String now() {
        return new SimpleDateFormat("HH:mm:ss").format(new Date());
    }
}