package io.avaje.jsonlogger.encoder;

import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.LegacyAbstractLogger;

import static org.slf4j.spi.LocationAwareLogger.*;
import static org.slf4j.spi.LocationAwareLogger.DEBUG_INT;
import static org.slf4j.spi.LocationAwareLogger.TRACE_INT;

final class SimpleLogger extends LegacyAbstractLogger {

    private int currentLogLevel;

    private final LogWriter writer;

    SimpleLogger(LogWriter writer, String name, int currentLogLevel) {
        this.writer = writer;
        this.name = name;
        this.currentLogLevel = currentLogLevel;
    }

    @Override
    public boolean isTraceEnabled() {
        return TRACE_INT >= currentLogLevel;
    }

    @Override
    public boolean isDebugEnabled() {
        return DEBUG_INT >= currentLogLevel;
    }

    @Override
    public boolean isInfoEnabled() {
        return INFO_INT >= currentLogLevel;
    }

    @Override
    public boolean isWarnEnabled() {
        return WARN_INT >= currentLogLevel;
    }

    @Override
    public boolean isErrorEnabled() {
        return ERROR_INT >= currentLogLevel;
    }

    @Override
    protected String getFullyQualifiedCallerName() {
        return null;
    }

    @Override
    protected void handleNormalizedLoggingCall(Level level, Marker marker, String messagePattern, Object[] arguments, Throwable throwable) {
//        List<Marker> markers = null;
//        if (marker != null) {
//            markers = new ArrayList<>();
//            markers.add(marker);
//        }
        logNormalized(level, messagePattern, arguments, throwable);
    }

    private void logNormalized(Level level, String messagePattern, Object[] arguments, Throwable t) {
        writer.log(name, level, messagePattern, arguments, t);
    }

//    public void log(LoggingEvent event) {
//        int levelInt = event.getLevel().toInt();
//        if (!(levelInt >= currentLogLevel)) {
//            return;
//        }
//        NormalizedParameters np = NormalizedParameters.normalize(event);
//        logNormalized(event.getLevel(), np.getMessage(), np.getArguments(), event.getThrowable());
//    }

}
