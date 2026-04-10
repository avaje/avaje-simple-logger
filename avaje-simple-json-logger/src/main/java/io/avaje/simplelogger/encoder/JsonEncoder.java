package io.avaje.simplelogger.encoder;

import io.avaje.json.PropertyNames;
import io.avaje.json.stream.JsonStream;
import org.slf4j.MDC;
import org.slf4j.event.Level;
import org.slf4j.helpers.MessageFormatter;

import java.io.ByteArrayOutputStream;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

final class JsonEncoder {

  private final JsonStream json;
  private final Map<String, String> customFieldsMap;
  private final PropertyNames properties;
  private final StackHasher stackHasher;
  private final ThrowableConverter throwableConverter;
  private final DateTimeFormatter formatter;
  private final TraceContext traceContext;
  private final int fieldExtra;
  private final String component;
  private final String environment;
  private final boolean includeStackHash;

  JsonEncoder(
        String[] propertyNames,
        JsonStream json,
        String component,
        String environment,
        StackHasher stackHasher,
        DateTimeFormatter formatter,
        boolean includeStackHash,
        Map<String, String> customFieldsMap,
        ThrowableConverter throwableConverter,
        TraceContext traceContext) {

    this.json = json;
    this.properties = this.json.properties(propertyNames);
    this.component = component;
    this.environment = environment;
    this.stackHasher = stackHasher;
    this.formatter = formatter;
    this.includeStackHash = includeStackHash;
    this.customFieldsMap = customFieldsMap;
    this.throwableConverter = throwableConverter;
    this.traceContext = traceContext;
    this.fieldExtra = this.customFieldsMap.entrySet().stream()
      .mapToInt(e -> e.getKey().length() + e.getValue().length() + 6)
      .sum();
  }

  byte[] encode(String loggerName, Level level, String messagePattern, Object[] arguments, Throwable t) {
    final String stackTraceBody = t == null ? "" : throwableConverter.convert(t);
    final int extra = stackTraceBody.isEmpty() ? 0 : 20 + stackTraceBody.length();

    String message = MessageFormatter.basicArrayFormat(messagePattern, arguments);
    if (message == null) {
      message = "";
    }

    final var threadName = Thread.currentThread().getName();
    final int bufferSize = 100 + extra + fieldExtra + message.length() + threadName.length() + loggerName.length();
    final var outputStream = new ByteArrayOutputStream(bufferSize);

    try (var writer = json.writer(outputStream)) {
      writer.beginObject(properties);
      if (component != null) {
        writer.name(0);
        writer.value(component);
      }
      if (environment != null) {
        writer.name(1);
        writer.value(environment);
      }
      writer.name(2);
      writer.value(formatter.format(OffsetDateTime.now()));
      writer.name(3);
      writer.value(level.toString());
      writer.name(4);
      writer.value(loggerName);
      writer.name(5);
      writer.value(message);
      writer.name(6);
      writer.value(threadName);
      String traceId = traceContext.traceId();
      if (traceId != null) {
        writer.name(11);
        writer.value(traceId);
      }
      String spanId = traceContext.spanId();
      if (spanId != null) {
        writer.name(12);
        writer.value(spanId);
      }
      if (!stackTraceBody.isEmpty()) {
        writer.name(7);
        writer.value(t.getClass().getName());
        writer.name(8);
        writer.value(t.getMessage());
        if (includeStackHash) {
          String hash = stackHasher.hexHash(t);
          writer.name(9);
          writer.value(hash);
        }
        writer.name(10);
        writer.value(stackTraceBody);
      }
      customFieldsMap.forEach((k, v) -> {
        writer.name(k);
        writer.rawValue(v);
      });
      Map<String, String> contextMap = MDC.getCopyOfContextMap();
      if (contextMap != null) {
        contextMap.forEach((k, v) -> {
          if (!"trace_id".equals(k) && !"span_id".equals(k)) {
            writer.name(k);
            writer.value(v);
          }
        });
      }
      writer.endObject();
      writer.writeNewLine();
    }
    return outputStream.toByteArray();
  }

}
