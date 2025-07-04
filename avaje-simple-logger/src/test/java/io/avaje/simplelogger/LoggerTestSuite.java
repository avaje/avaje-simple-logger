package io.avaje.simplelogger;


import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class LoggerTestSuite {

  private ListAppendingOutputStream prepareSink(List<String> source) {
    return new ListAppendingOutputStream(source);

  }

  @Test
  public void testTrace() {
    ArrayList<String> loggingEvents = new ArrayList<>();
    Logger configuredLogger = createLogger(prepareSink(loggingEvents), Level.TRACE);

    assertThat(configuredLogger.isTraceEnabled())
      .describedAs("Trace level should be enabled for this test")
      .isTrue();
    configuredLogger.trace("Simple trace message");

    assertThat(loggingEvents).hasSize(1);
    assertTrue(isTraceMessage(loggingEvents.get(0)));
    assertEquals(
      "Simple trace message",
      extractMessage(loggingEvents.get(0)));

    loggingEvents.clear();

    configuredLogger.debug("Simple debug message");
    configuredLogger.info("Simple info message");
    configuredLogger.warn("Simple warn message");
    configuredLogger.error("Simple error message");
    assertThat(loggingEvents).hasSize(4);
  }

  @Test
  public void testDebug() {
    ArrayList<String> loggingEvents = new ArrayList<>();
    Logger configuredLogger = createLogger(prepareSink(loggingEvents), Level.DEBUG);

    configuredLogger.trace("Simple trace message");
    assertThat(loggingEvents)
      .describedAs("Lower levels should have been ignored")
      .isEmpty();

    assertThat(configuredLogger.isDebugEnabled())
      .describedAs("Debug level should be enabled for this test")
      .isTrue();
    configuredLogger.debug("Simple debug message");

    assertThat(loggingEvents).hasSize(1);
    assertTrue(isDebugMessage(loggingEvents.get(0)));
    assertEquals(
      "Simple debug message",
      extractMessage(loggingEvents.get(0)));

    loggingEvents.clear();

    configuredLogger.info("Simple info message");
    configuredLogger.warn("Simple warn message");
    configuredLogger.error("Simple error message");
    assertThat(loggingEvents).hasSize(3);
  }

  @Test
  public void testInfo() {
    ArrayList<String> loggingEvents = new ArrayList<>();
    Logger configuredLogger = createLogger(prepareSink(loggingEvents), Level.INFO);

    configuredLogger.trace("Simple trace message");
    configuredLogger.debug("Simple debug message");
    assertEquals(0, loggingEvents.size());

    assertTrue(configuredLogger.isInfoEnabled());
    configuredLogger.info("Simple info message");

    assertEquals(1, loggingEvents.size());
    assertTrue(isInfoMessage(loggingEvents.get(0)));
    assertEquals("Supplied info message wasn't found in the log",
      "Simple info message",
      extractMessage(loggingEvents.get(0)));

    loggingEvents.clear();

    configuredLogger.warn("Simple warn message");
    configuredLogger.error("Simple error message");
    assertEquals(2, loggingEvents.size());
  }

  @Test
  public void testWarn() {
    ArrayList<String> loggingEvents = new ArrayList<>();
    Logger configuredLogger = createLogger(prepareSink(loggingEvents), Level.WARN);

    configuredLogger.trace("Simple trace message");
    configuredLogger.debug("Simple debug message");
    configuredLogger.info("Simple info message");
    assertEquals(0, loggingEvents.size());

    assertTrue(configuredLogger.isWarnEnabled());
    configuredLogger.warn("Simple warn message");

    assertEquals(1, loggingEvents.size());
    assertTrue(isWarnMessage(loggingEvents.get(0)));
    assertEquals("Simple warn message",
      extractMessage(loggingEvents.get(0)));

    loggingEvents.clear();

    configuredLogger.error("Simple error message");
    assertEquals(1, loggingEvents.size());
  }

  @Test
  public void testError() {
    ArrayList<String> loggingEvents = new ArrayList<>();
    Logger configuredLogger = createLogger(prepareSink(loggingEvents), Level.ERROR);

    configuredLogger.trace("Simple trace message");
    configuredLogger.debug("Simple debug message");
    configuredLogger.info("Simple info message");
    configuredLogger.warn("Simple warn message");
    assertEquals(0, loggingEvents.size());

    assertTrue(configuredLogger.isErrorEnabled());
    configuredLogger.error("Simple error message");

    assertEquals(1, loggingEvents.size());
    assertTrue(isErrorMessage(loggingEvents.get(0)));
    assertEquals("Simple error message",
      extractMessage(loggingEvents.get(0)));
  }

  @Test
  public void testFormatting() {
    ArrayList<String> loggingEvents = new ArrayList<>();
    Logger configuredLogger = createLogger(prepareSink(loggingEvents), Level.INFO);

    configuredLogger.info("Some {} string", "formatted");
    assertEquals(1, loggingEvents.size());
    assertEquals("Message should've been formatted", "Some formatted string", extractMessage(loggingEvents.get(0)));
  }

  @Test
  public void testException() {
    ArrayList<String> loggingEvents = new ArrayList<>();
    Logger configuredLogger = createLogger(prepareSink(loggingEvents), Level.INFO);

    Exception exception = new RuntimeException("My error");

    configuredLogger.info("Logging with an exception", exception);
    assertEquals(1, loggingEvents.size());
    assertEquals("Message should've been formatted",
      "My error",
      extractExceptionMessage(loggingEvents.get(0)));

    assertEquals("Message should've been formatted",
      "java.lang.RuntimeException",
      extractExceptionType(loggingEvents.get(0)));
  }

  /**
   * Allows tests to check whether the log message contains a trace message.
   * Override if needed.
   *
   * @param message String containing the full log message
   * @return whether it is a trace message or not
   */
  protected boolean isTraceMessage(String message) {
    return message.toLowerCase().contains("trace");
  }

  /**
   * Allows tests to check whether the log message contains a debug message.
   * Override if needed.
   *
   * @param message String containing the full log message
   * @return whether it is a debug message or not
   */
  protected boolean isDebugMessage(String message) {
    return message.toLowerCase().contains("debug");
  }

  /**
   * Allows tests to check whether the log message contains an info message.
   * Override if needed.
   *
   * @param message String containing the full log message
   * @return whether it is an info message or not
   */
  protected boolean isInfoMessage(String message) {
    return message.toLowerCase().contains("info");
  }

  /**
   * Allows tests to check whether the log message contains a warn message.
   * Override if needed.
   *
   * @param message String containing the full log message
   * @return whether it is a warn message or not
   */
  protected boolean isWarnMessage(String message) {
    return message.toLowerCase().contains("warn");
  }

  /**
   * Allows tests to check whether the log message contains an error message.
   * Override if needed.
   *
   * @param message String containing the full log message
   * @return whether it is an error message or not
   */
  protected boolean isErrorMessage(String message) {
    return message.toLowerCase().contains("error");
  }

  /**
   * Extracts only the part of the log string that should represent the `message` string.
   *
   * @param message the full log message
   * @return only the supplied message
   */
  public abstract String extractMessage(String message);

  /**
   * Extracts only the part of the log string that should represent the supplied exception message, if any.
   *
   * @param message the full log message
   * @return only the supplied exception message
   */
  public abstract String extractExceptionMessage(String message);

  /**
   * Extracts only the part of the log string that should represent the supplied exception type.
   *
   * @param message the full log message
   * @return only the supplied exception type name
   */
  public abstract String extractExceptionType(String message);

  /**
   * Configures the logger for running the tests.
   *
   * @param outputStream The output stream for logs to be written to
   * @param level        The expected level the tests will run for this logger
   * @return a configured logger able to run the tests
   */
  public abstract Logger createLogger(ListAppendingOutputStream outputStream, Level level);

  public static class ListAppendingOutputStream extends OutputStream {
    private final StringBuilder word = new StringBuilder();
    private final List<String> list;
    private int index = 0;

    private ListAppendingOutputStream(List<String> list) {
      this.list = list;
    }


    @Override
    public void write(int b) throws IOException {
      word.append((char) b);
    }

    @Override
    public void flush() {
      list.add(word.toString());
      word.delete(0, word.length());
      index++;
    }
  }

}
