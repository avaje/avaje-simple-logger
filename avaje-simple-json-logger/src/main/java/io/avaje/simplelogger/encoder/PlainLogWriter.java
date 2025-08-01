package io.avaje.simplelogger.encoder;

import org.slf4j.event.Level;
import org.slf4j.helpers.MessageFormatter;
import org.slf4j.spi.LocationAwareLogger;

import java.io.PrintStream;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

final class PlainLogWriter implements LogWriter {

  private static final char SP = ' ';

  private final PrintStream targetStream;
  private final DateTimeFormatter formatter;
  private final boolean showThreadName;

  PlainLogWriter(PrintStream targetStream, DateTimeFormatter formatter, boolean showThreadName) {
    this.targetStream = targetStream;
    this.formatter = formatter;
    this.showThreadName = showThreadName;
  }

  @Override
  public void log(String loggerName, Level level, String messagePattern, Object[] arguments, Throwable t) {
    StringBuilder buf = new StringBuilder(200);
    buf.append(formattedTimestamp());
    buf.append(SP);

    if (showThreadName) {
      buf.append('[').append(Thread.currentThread().getName()).append("] ");
    }

    buf.append(renderLevel(level.toInt()));
    buf.append(SP);

    buf.append(loggerName).append(" - ");
    buf.append(MessageFormatter.basicArrayFormat(messagePattern, arguments));
    write(buf, t);
  }

  private void write(StringBuilder buf, Throwable t) {
    if (t == null) {
      targetStream.println(buf.toString());
    } else {
      synchronized (targetStream) {
        targetStream.println(buf.toString());
        writeThrowable(t, targetStream);
        targetStream.flush();
      }
    }
  }

  private void writeThrowable(Throwable t, PrintStream targetStream) {
    if (t != null) {
      t.printStackTrace(targetStream);
    }
  }

  private String formattedTimestamp() {
    return formatter.format(OffsetDateTime.now());
  }

  private String renderLevel(int levelInt) {
    switch (levelInt) {
      case LocationAwareLogger.TRACE_INT:
        return "TRACE";
      case LocationAwareLogger.DEBUG_INT:
        return ("DEBUG");
      case LocationAwareLogger.INFO_INT:
        return "INFO";
      case LocationAwareLogger.WARN_INT:
        return "WARN";
      case LocationAwareLogger.ERROR_INT:
        return "ERROR";
    }
    throw new IllegalStateException("Unrecognized level [" + levelInt + "]");
  }

}
