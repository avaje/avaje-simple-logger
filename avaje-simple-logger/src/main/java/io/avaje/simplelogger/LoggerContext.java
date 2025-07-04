package io.avaje.simplelogger;

import io.avaje.simplelogger.encoder.Bootstrap;
import org.slf4j.ILoggerFactory;

import java.util.Map;

public interface LoggerContext extends ILoggerFactory {

    LoggerContext CONTEXT = Bootstrap.init();

    static LoggerContext get() {
        return CONTEXT;
    }

    /**
     * Apply a set of name log level pairs and update log levels for all impacted loggers.
     */
    void putAll(Map<String,String> nameLevels);
}
