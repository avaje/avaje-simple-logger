package io.avaje.simplelogger.otel;

import io.avaje.simplelogger.LogWriter;
import io.opentelemetry.api.logs.LogRecordBuilder;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import org.slf4j.MDC;
import org.slf4j.event.KeyValuePair;
import org.slf4j.event.Level;
import org.slf4j.helpers.MessageFormatter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Writes Avaje Simple Logger calls to the OpenTelemetry Logs SDK.
 */
final class OtelLogWriter implements LogWriter {

  private static final String THREAD_NAME = "thread.name";

  private final SdkLoggerProvider loggerProvider;
  private final Map<String, Logger> loggers = new ConcurrentHashMap<>();

  OtelLogWriter(SdkLoggerProvider loggerProvider) {
    this.loggerProvider = loggerProvider;
  }

  @Override
  public void log(String loggerName, String displayName, Level level, String messagePattern,
                  Object[] arguments, Throwable throwable, List<KeyValuePair> keyValuePairs) {
    if (isOtelInternal(loggerName)) {
      return;
    }

    Logger logger = loggers.computeIfAbsent(loggerName, loggerProvider::get);
    LogRecordBuilder builder = logger.logRecordBuilder()
      .setTimestamp(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
      .setSeverity(severity(level))
      .setSeverityText(level.toString())
      .setBody(message(messagePattern, arguments))
      .setContext(Context.current())
      .setAttribute(THREAD_NAME, Thread.currentThread().getName());

    if (throwable != null) {
      builder.setException(throwable);
    }
    addMdcAttributes(builder, MDC.getCopyOfContextMap());
    addKeyValueAttributes(builder, keyValuePairs);
    builder.emit();
  }

  @Override
  public boolean forceFlush(long timeoutMillis) {
    CompletableResultCode result = loggerProvider.forceFlush();
    return result.join(timeoutMillis, TimeUnit.MILLISECONDS).isSuccess();
  }

  private static String message(String messagePattern, Object[] arguments) {
    String message = MessageFormatter.basicArrayFormat(messagePattern, arguments);
    return message == null ? "" : message;
  }

  private static boolean isOtelInternal(String loggerName) {
    return loggerName.startsWith("io.opentelemetry.")
      || loggerName.startsWith("io.avaje.simplelogger.otel.");
  }

  private static void addMdcAttributes(LogRecordBuilder builder, Map<String, String> contextMap) {
    if (contextMap == null) {
      return;
    }
    contextMap.forEach((key, value) -> {
      if (!"trace_id".equals(key) && !"span_id".equals(key)) {
        builder.setAttribute(key, value);
      }
    });
  }

  private static void addKeyValueAttributes(LogRecordBuilder builder, List<KeyValuePair> keyValuePairs) {
    if (keyValuePairs == null) {
      return;
    }
    for (KeyValuePair pair : keyValuePairs) {
      if (pair != null && pair.key != null && pair.value != null) {
        setAttribute(builder, pair.key, pair.value);
      }
    }
  }

  private static void setAttribute(LogRecordBuilder builder, String key, Object value) {
    if (value instanceof String) {
      builder.setAttribute(key, (String) value);
    } else if (value instanceof Boolean) {
      builder.setAttribute(key, (Boolean) value);
    } else if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) {
      builder.setAttribute(key, ((Number) value).longValue());
    } else if (value instanceof Number) {
      builder.setAttribute(key, ((Number) value).doubleValue());
    } else {
      builder.setAttribute(key, safeToString(value));
    }
  }

  private static Severity severity(Level level) {
    switch (level) {
      case TRACE:
        return Severity.TRACE;
      case DEBUG:
        return Severity.DEBUG;
      case INFO:
        return Severity.INFO;
      case WARN:
        return Severity.WARN;
      case ERROR:
        return Severity.ERROR;
      default:
        throw new IllegalArgumentException("Unsupported log level: " + level);
    }
  }

  private static String safeToString(Object value) {
    try {
      return String.valueOf(value);
    } catch (RuntimeException e) {
      return "<toString() failed: " + e.getClass().getName() + ">";
    }
  }
}
