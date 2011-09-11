package usi2011.util;

import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

public class LogUtil {
    private static final Logger logger = getLogger(LogUtil.class);
    public static final boolean isTraceEnabled = logger.isTraceEnabled();
    public static final boolean isDebugEnabled = logger.isDebugEnabled();
    public static final boolean isInfoEnabled = logger.isInfoEnabled();
    public static final boolean isWarnEnabled = logger.isWarnEnabled();
}
