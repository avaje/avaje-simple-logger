package io.avaje.simplelogger.encoder;

import org.slf4j.MDC;
import org.slf4j.event.Level;
import org.slf4j.event.KeyValuePair;
import org.slf4j.helpers.MessageFormatter;
import org.slf4j.spi.LocationAwareLogger;

import java.io.PrintStream;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static io.avaje.simplelogger.encoder.JsonEncoder.safeToString;

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
  public void log(String loggerName, Level level, String messagePattern, Object[] arguments, Throwable t, List<KeyValuePair> keyValuePairs) {
    StringBuilder buf = new StringBuilder(200);
    buf.append(formattedTimestamp());
    buf.append(SP);

    if (showThreadName) {
      buf.append('[').append(Thread.currentThread().getName()).append("] ");
    }

    buf.append(renderLevel(level.toInt()));
    buf.append(SP);

    buf.append(loggerName).append(" - ");
    final String message = MessageFormatter.basicArrayFormat(messagePattern, arguments);
    buf.append(withContext(message, keyValuePairs));
    write(buf, t);
  }

  private String withContext(String message, List<KeyValuePair> keyValuePairs) {
    Map<String, String> contextMap = MDC.getCopyOfContextMap();
    if ((contextMap == null || contextMap.isEmpty()) && (keyValuePairs == null || keyValuePairs.isEmpty())) {
      return message;
    }
    final StringBuilder content = new StringBuilder(40 + (message == null ? 0 : message.length()));
    appendMdc(content, contextMap);
    appendKeyValues(content, keyValuePairs);
    if (message != null) {
      content.append(message);
    }
    return content.toString();
  }

  private static void appendMdc(StringBuilder content, Map<String, String> contextMap) {
    if (contextMap == null || contextMap.isEmpty()) {
      return;
    }
    contextMap.forEach((key, value) -> content.append(key)
      .append('=')
      .append(safeToString(value))
      .append(SP));
  }

  private static void appendKeyValues(StringBuilder content, List<KeyValuePair> keyValuePairs) {
    if (keyValuePairs == null || keyValuePairs.isEmpty()) {
      return;
    }
    for (KeyValuePair keyValuePair : keyValuePairs) {
      if (keyValuePair == null) {
        continue;
      }
      content.append(keyValuePair.key)
        .append('=')
        .append(safeToString(keyValuePair.value))
        .append(SP);
    }
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
