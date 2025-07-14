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
  private final int fieldExtra;
  private final String component;
  private final String environment;
  private final boolean includeStackHash;

  JsonEncoder(JsonStream json, String component, String environment, StackHasher stackHasher, DateTimeFormatter formatter, boolean includeStackHash, Map<String, String> customFieldsMap, ThrowableConverter throwableConverter) {
    this.json = json;
    this.properties = this.json.properties("component", "env", "timestamp", "level", "logger", "message", "thread", "stackhash", "stacktrace");
    this.component = component;
    this.environment = environment;
    this.stackHasher = stackHasher;
    this.formatter = formatter;
    this.includeStackHash = includeStackHash;
    this.customFieldsMap = customFieldsMap;
    this.throwableConverter = throwableConverter;
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
      if (!stackTraceBody.isEmpty()) {
        if (includeStackHash) {
          String hash = stackHasher.hexHash(t);
          writer.name(7);
          writer.value(hash);
        }
        writer.name(8);
        writer.value(stackTraceBody);
      }
      customFieldsMap.forEach((k, v) -> {
        writer.name(k);
        writer.rawValue(v);
      });
      Map<String, String> contextMap = MDC.getCopyOfContextMap();
      if (contextMap != null) {
        contextMap.forEach((k, v) -> {
          writer.name(k);
          writer.value(v);
        });
      }
      writer.endObject();
      writer.writeNewLine();
    }
    return outputStream.toByteArray();
  }

}
