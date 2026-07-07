package ca.favro.vega.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

public class VegaLogger implements Logger {
    private final Logger logger;

    public VegaLogger(Class clazz) {
        logger = LoggerFactory.getLogger(clazz);
    }

    @Override
    public String getName() {
        return logger.getName();
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    @Override
    public void trace(String msg) {
        logger.trace("[Vega]: " + msg);
    }

    @Override
    public void trace(String format, Object arg) {
        logger.trace("[Vega]: " + format, arg);
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        logger.trace("[Vega]: " + format, arg1, arg2);
    }

    @Override
    public void trace(String format, Object... arguments) {
        logger.trace("[Vega]: " + format, arguments);
    }

    @Override
    public void trace(String msg, Throwable t) {
        logger.trace("[Vega]: " + msg, t);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return logger.isTraceEnabled(marker);
    }

    @Override
    public void trace(Marker marker, String msg) {
        logger.trace(marker, "[Vega]: " + msg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg) {
        logger.trace(marker, "[Vega]: " + format, arg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        logger.trace(marker, "[Vega]: " + format, arg1, arg2);
    }

    @Override
    public void trace(Marker marker, String format, Object... argArray) {
        logger.trace(marker, "[Vega]: " + format, argArray);
    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {
        logger.trace(marker, "[Vega]: " + msg, t);
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public void debug(String msg) {
        logger.debug("[Vega]: " + msg);
    }

    @Override
    public void debug(String format, Object arg) {
        logger.debug("[Vega]: " + format, arg);
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        logger.debug("[Vega]: " + format, arg1, arg2);
    }

    @Override
    public void debug(String format, Object... arguments) {
        logger.debug("[Vega]: " + format, arguments);
    }

    @Override
    public void debug(String msg, Throwable t) {
        logger.debug("[Vega]: " + msg, t);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return logger.isDebugEnabled(marker);
    }

    @Override
    public void debug(Marker marker, String msg) {
        logger.debug(marker, "[Vega]: " + msg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg) {
        logger.debug(marker, "[Vega]: " + format, arg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        logger.debug(marker, "[Vega]: " + format, arg1, arg2);
    }

    @Override
    public void debug(Marker marker, String format, Object... arguments) {
        logger.debug(marker, "[Vega]: " + format, arguments);
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
        logger.debug(marker, "[Vega]: " + msg, t);
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    @Override
    public void info(String msg) {
        logger.info("[Vega]: " + msg);
    }

    @Override
    public void info(String format, Object arg) {
        logger.info("[Vega]: " + format, arg);
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        logger.info("[Vega]: " + format, arg1, arg2);
    }

    @Override
    public void info(String format, Object... arguments) {
        logger.info("[Vega]: " + format, arguments);
    }

    @Override
    public void info(String msg, Throwable t) {
        logger.info("[Vega]: " + msg, t);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return logger.isInfoEnabled(marker);
    }

    @Override
    public void info(Marker marker, String msg) {
        logger.info(marker, "[Vega]: " + msg);
    }

    @Override
    public void info(Marker marker, String format, Object arg) {
        logger.info(marker, "[Vega]: " + format, arg);
    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {
        logger.info(marker, "[Vega]: " + format, arg1, arg2);
    }

    @Override
    public void info(Marker marker, String format, Object... arguments) {
        logger.info(marker, "[Vega]: " + format, arguments);
    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {
        logger.info(marker, "[Vega]: " + msg, t);
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    @Override
    public void warn(String msg) {
        logger.warn("[Vega]: " + msg);
    }

    @Override
    public void warn(String format, Object arg) {
        logger.warn("[Vega]: " + format, arg);
    }

    @Override
    public void warn(String format, Object... arguments) {
        logger.warn("[Vega]: " + format, arguments);
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        logger.warn("[Vega]: " + format, arg1, arg2);
    }

    @Override
    public void warn(String msg, Throwable t) {
        logger.warn("[Vega]: " + msg, t);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return logger.isWarnEnabled(marker);
    }

    @Override
    public void warn(Marker marker, String msg) {
        logger.warn(marker, "[Vega]: " + msg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg) {
        logger.warn(marker, "[Vega]: " + format, arg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        logger.warn(marker, "[Vega]: " + format, arg1, arg2);
    }

    @Override
    public void warn(Marker marker, String format, Object... arguments) {
        logger.warn(marker, "[Vega]: " + format, arguments);
    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {
        logger.warn(marker, "[Vega]: " + msg, t);
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    @Override
    public void error(String msg) {
        logger.error("[Vega]: " + msg);
    }

    @Override
    public void error(String format, Object arg) {
        logger.error("[Vega]: " + format, arg);
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        logger.error("[Vega]: " + format, arg1, arg2);
    }

    @Override
    public void error(String format, Object... arguments) {
        logger.error("[Vega]: " + format, arguments);
    }

    @Override
    public void error(String msg, Throwable t) {
        logger.error("[Vega]: " + msg, t);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return logger.isErrorEnabled(marker);
    }

    @Override
    public void error(Marker marker, String msg) {
        logger.error(marker, "[Vega]: " + msg);
    }

    @Override
    public void error(Marker marker, String format, Object arg) {
        logger.error(marker, "[Vega]: " + format, arg);
    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        logger.error(marker, "[Vega]: " + format, arg1, arg2);
    }

    @Override
    public void error(Marker marker, String format, Object... arguments) {
        logger.error(marker, "[Vega]: " + format, arguments);
    }

    @Override
    public void error(Marker marker, String msg, Throwable t) {
        logger.error(marker, "[Vega]: " + msg, t);
    }
}
