package io.avaje.simplelogger.encoder;

import io.avaje.simplelogger.LoggerContext;
import org.slf4j.Logger;
import org.slf4j.spi.LocationAwareLogger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.slf4j.spi.LocationAwareLogger.ERROR_INT;

final class SimpleLoggerFactory implements LoggerContext {

  private static final int LOG_LEVEL_OFF = ERROR_INT + 10;

  private final ConcurrentMap<String, SimpleLogger> loggerMap = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, Integer> levels = new ConcurrentHashMap<>();

  private final LogWriter logWriter;
  private final Abbreviator abbreviator;
  private final int defaultLogLevel;

  SimpleLoggerFactory(LogWriter logWriter, Abbreviator abbreviator, int defaultLogLevel) {
    this.logWriter = logWriter;
    this.abbreviator = abbreviator;
    this.defaultLogLevel = defaultLogLevel;
  }

  static int stringToLevel(String levelStr) {
    if ("trace".equalsIgnoreCase(levelStr)) {
      return LocationAwareLogger.TRACE_INT;
    } else if ("debug".equalsIgnoreCase(levelStr)) {
      return LocationAwareLogger.DEBUG_INT;
    } else if ("info".equalsIgnoreCase(levelStr)) {
      return LocationAwareLogger.INFO_INT;
    } else if ("warn".equalsIgnoreCase(levelStr)) {
      return LocationAwareLogger.WARN_INT;
    } else if ("error".equalsIgnoreCase(levelStr)) {
      return LocationAwareLogger.ERROR_INT;
    } else if ("off".equalsIgnoreCase(levelStr)) {
      return LOG_LEVEL_OFF;
    }
    // assume INFO by default
    return LocationAwareLogger.INFO_INT;
  }

  @Override
  public void putAll(Map<String, String> nameLevels) {
    nameLevels.forEach(this::putLevel);

    for (Map.Entry<String, SimpleLogger> entry : loggerMap.entrySet()) {
      final String key = entry.getKey();
      if (adjustedKey(key, nameLevels)) {
        entry.getValue().setNewLevel(level(key));
      }
    }
  }

  private boolean adjustedKey(String key, Map<String, String> nameLevels) {
    return nameLevels.keySet().stream()
      .anyMatch(key::startsWith);
  }

  private void putLevel(String name, String level) {
    putLevel(name, stringToLevel(level));
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
  private SimpleLogger create(String fullName) {
    final String shortName = abbreviator.abbreviate(fullName);
    return new SimpleLogger(logWriter, fullName, shortName, level(fullName));
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
