package io.avaje.jsonlogger.encoder;

import org.slf4j.event.Level;

interface LogWriter {

    void log(String loggerName, Level level, String messagePattern, Object[] arguments, Throwable t);
}
