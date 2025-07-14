package io.avaje.simplelogger;

import io.avaje.simplelogger.encoder.Bootstrap;
import org.slf4j.ILoggerFactory;

import java.util.Map;

/**
 * Extends ILoggerFactory with ability to set log levels.
 */
public interface LoggerContext extends ILoggerFactory {

  /**
   * The global LoggerContext.
   */
  LoggerContext CONTEXT = Bootstrap.init();

  /**
   * Return the LoggerContext.
   */
  static LoggerContext get() {
    return CONTEXT;
  }

  /**
   * Apply a set of name log level pairs and update log levels for all impacted loggers.
   *
   * @return Map of changed loggers and their new levels.
   */
  Map<String, String> putAll(Map<String, String> nameLevels);
}
