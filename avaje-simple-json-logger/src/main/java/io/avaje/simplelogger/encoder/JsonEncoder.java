package io.avaje.simplelogger.encoder;

import io.avaje.json.PropertyNames;
import io.avaje.json.JsonWriter;
import io.avaje.json.stream.JsonStream;
import org.slf4j.MDC;
import org.slf4j.event.Level;
import org.slf4j.event.KeyValuePair;
import org.slf4j.helpers.MessageFormatter;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
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

  byte[] encode(String loggerName, Level level, String messagePattern, Object[] arguments, Throwable t, List<KeyValuePair> keyValuePairs) {
    final String stackTraceBody = t == null ? "" : throwableConverter.convert(t);
    final int extra = stackTraceBody.isEmpty() ? 0 : 20 + stackTraceBody.length();

    String message = MessageFormatter.basicArrayFormat(messagePattern, arguments);
    if (message == null) {
      message = "";
    }

    final var threadName = Thread.currentThread().getName();
    final int bufferSize = 100 + extra + fieldExtra + keyValueExtra(keyValuePairs) + message.length() + threadName.length() + loggerName.length();
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
      if (keyValuePairs != null) {
        for (KeyValuePair keyValuePair : keyValuePairs) {
          if (keyValuePair == null) {
            continue;
          }
          writer.name(String.valueOf(keyValuePair.key));
          writeKeyValue(writer, keyValuePair.value);
        }
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

  private static int keyValueExtra(List<KeyValuePair> keyValuePairs) {
    if (keyValuePairs == null || keyValuePairs.isEmpty()) {
      return 0;
    }
    int extra = 0;
    for (KeyValuePair keyValuePair : keyValuePairs) {
      if (keyValuePair == null) {
        continue;
      }
      extra += String.valueOf(keyValuePair.key).length();
      extra += String.valueOf(keyValuePair.value).length();
      extra += 6;
    }
    return extra;
  }

  private static void writeKeyValue(JsonWriter writer, Object value) {
    if (value == null) {
      writer.nullValue();
    } else if (value instanceof CharSequence) {
      writer.value(value.toString());
    } else if (value instanceof Boolean) {
      writer.value((Boolean) value);
    } else if (value instanceof Integer) {
      writer.value((Integer) value);
    } else if (value instanceof Long) {
      writer.value((Long) value);
    } else if (value instanceof Double) {
      writer.value((Double) value);
    } else if (value instanceof Float) {
      writer.value(((Float) value).doubleValue());
    } else if (value instanceof Short) {
      writer.value(((Short) value).intValue());
    } else if (value instanceof Byte) {
      writer.value(((Byte) value).intValue());
    } else if (value instanceof BigDecimal) {
      writer.value((BigDecimal) value);
    } else if (value instanceof BigInteger) {
      writer.value((BigInteger) value);
    } else if (value instanceof byte[]) {
      writer.value((byte[]) value);
    } else {
      writer.value(safeToString(value));
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
