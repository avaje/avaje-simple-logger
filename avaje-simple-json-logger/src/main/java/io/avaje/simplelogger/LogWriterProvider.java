package io.avaje.simplelogger;

import java.util.Properties;

/**
 * Creates a {@link LogWriter} discovered through {@link java.util.ServiceLoader}.
 */
public interface LogWriterProvider {

  /**
   * Return the writer name selected by the {@code logger.writer} property.
   */
  String name();

  /**
   * Create a writer using the logger configuration.
   */
  LogWriter create(Properties properties);
}
