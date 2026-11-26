package io.avaje.simplelogger;

import org.slf4j.event.KeyValuePair;
import org.slf4j.event.Level;

import java.util.List;

/**
 * Writes normalized SLF4J logging calls to a destination.
 */
public interface LogWriter {

  /**
   * Write a logging call.
   *
   * @param loggerName the full logger name
   * @param displayName the logger name formatted for console output
   * @param level the logging level
   * @param messagePattern the SLF4J message pattern
   * @param arguments the message arguments
   * @param throwable the associated throwable
   * @param keyValuePairs the SLF4J fluent key/value pairs
   */
  void log(String loggerName, String displayName, Level level, String messagePattern,
           Object[] arguments, Throwable throwable, List<KeyValuePair> keyValuePairs);

  /**
   * Flush buffered log records within the supplied time limit.
   *
   * @return true when the flush completed successfully
   */
  default boolean forceFlush(long timeoutMillis) {
    return true;
  }
}
