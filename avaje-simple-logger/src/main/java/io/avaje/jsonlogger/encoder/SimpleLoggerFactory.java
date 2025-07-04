package io.avaje.jsonlogger.encoder;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.ILoggerFactory;

final class SimpleLoggerFactory implements ILoggerFactory {

    private final ConcurrentMap<String, Logger> loggerMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Integer> levels = new ConcurrentHashMap<>();

    private final LogWriter logWriter;
    private final int defaultLogLevel;

    SimpleLoggerFactory(LogWriter logWriter, int defaultLogLevel) {
        this.logWriter = logWriter;
        this.defaultLogLevel = defaultLogLevel;
    }

    void putLevel(String name, int level) {
        levels.put(name, level);
    }

    @Override
    public Logger getLogger(String name) {
        return loggerMap.computeIfAbsent(name, this::create);
    }

    /**
     * Actually creates the logger for the given name.
     */
    private Logger create(String name) {
        return new SimpleLogger(logWriter, name, level(name));
    }

    private int level(String name) {
        final Integer level = lookupLevel(name);
        return level != null ? level : defaultLogLevel;
    }

    private Integer lookupLevel(String name) {
        String tempName = name;
        Integer levelString = null;
        int indexOfLastDot = tempName.length();
        while ((levelString == null) && (indexOfLastDot > -1)) {
            tempName = tempName.substring(0, indexOfLastDot);
            levelString = levels.get(tempName);
            indexOfLastDot = tempName.lastIndexOf('.');
        }
        return levelString;
    }
}
