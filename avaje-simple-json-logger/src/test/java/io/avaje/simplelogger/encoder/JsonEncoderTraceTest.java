package io.avaje.simplelogger.encoder;

import io.avaje.json.stream.JsonStream;
import io.opentelemetry.api.trace.*;
import io.opentelemetry.context.Scope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.slf4j.event.Level;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;

class JsonEncoderTraceTest {

  static final String TRACE_ID = "0af7651916cd43dd8448eb211c80319c";
  static final String SPAN_ID = "b7ad6b7169203331";

  @AfterEach
  void cleanup() {
    MDC.clear();
  }

  private JsonEncoder buildEncoder(TraceContext traceContext) {
    String[] propertyNames = JsonEncoderBuilder.basePropertyNames(null);
    JsonStream json = JsonStream.builder().build();
    DateTimeFormatter formatter = TimeZoneUtils.jsonFormatter(null, TimeZone.getDefault().toZoneId());
    StackHasher stackHasher = new StackHasher(StackElementFilter.builder().allFilters().build());
    return new JsonEncoder(propertyNames, json, null, null, stackHasher, formatter, true, new HashMap<>(), new ThrowableConverter(), traceContext);
  }

  private String encode(JsonEncoder encoder, String message, Throwable t) {
    byte[] bytes = encoder.encode("test.Logger", Level.INFO, message, null, t, null);
    return new String(bytes, StandardCharsets.UTF_8);
  }

  @Test
  void noopTraceContext_traceFieldsAbsent() {
    JsonEncoder encoder = buildEncoder(new NoopTraceContext());
    String json = encode(encoder, "hello", null);

    assertThat(json).doesNotContain("\"trace_id\"");
    assertThat(json).doesNotContain("\"span_id\"");
    assertThat(json).contains("\"message\":\"hello\"");
  }

  @Test
  void otelTraceContext_withActiveSpan_traceFieldsPresent() {
    JsonEncoder encoder = buildEncoder(new OtelTraceContext());

    SpanContext spanContext = SpanContext.create(TRACE_ID, SPAN_ID, TraceFlags.getSampled(), TraceState.getDefault());
    Span span = Span.wrap(spanContext);

    try (Scope ignored = span.makeCurrent()) {
      String json = encode(encoder, "traced message", null);

      assertThat(json).contains("\"trace_id\":\"" + TRACE_ID + "\"");
      assertThat(json).contains("\"span_id\":\"" + SPAN_ID + "\"");
      assertThat(json).contains("\"message\":\"traced message\"");
    }
  }

  @Test
  void otelTraceContext_noActiveSpan_traceFieldsAbsent() {
    JsonEncoder encoder = buildEncoder(new OtelTraceContext());
    String json = encode(encoder, "no trace", null);

    assertThat(json).doesNotContain("\"trace_id\"");
    assertThat(json).doesNotContain("\"span_id\"");
  }

  @Test
  void traceFieldsCoexistWithException() {
    JsonEncoder encoder = buildEncoder(new OtelTraceContext());
    RuntimeException exception = new RuntimeException("test error");

    SpanContext spanContext = SpanContext.create(TRACE_ID, SPAN_ID, TraceFlags.getSampled(), TraceState.getDefault());
    Span span = Span.wrap(spanContext);

    try (Scope ignored = span.makeCurrent()) {
      String json = encode(encoder, "error in span", exception);

      assertThat(json).contains("\"trace_id\":\"" + TRACE_ID + "\"");
      assertThat(json).contains("\"span_id\":\"" + SPAN_ID + "\"");
      assertThat(json).contains("\"exceptionType\":\"java.lang.RuntimeException\"");
      assertThat(json).contains("\"exceptionMessage\":\"test error\"");
      assertThat(json).contains("\"stacktrace\":");
    }
  }

  @Test
  void propertyNameRemapping_traceFields() {
    String[] keys = JsonEncoderBuilder.basePropertyNames(null);
    String[] names = JsonEncoderBuilder.toPropertyNames(keys, "trace_id=traceIdentifier;span_id=spanIdentifier");

    assertThat(names[11]).isEqualTo("traceIdentifier");
    assertThat(names[12]).isEqualTo("spanIdentifier");
  }

  @Test
  void initTraceContext_returnsOtelWhenOnClasspath() {
    // OTEL API is on the test classpath, so builder should create OtelTraceContext
    JsonEncoder encoder = new JsonEncoderBuilder().build();

    // Verify by checking that with an active span, trace fields appear
    SpanContext spanContext = SpanContext.create(TRACE_ID, SPAN_ID, TraceFlags.getSampled(), TraceState.getDefault());
    Span span = Span.wrap(spanContext);

    try (Scope ignored = span.makeCurrent()) {
      byte[] bytes = encoder.encode("test.Logger", Level.INFO, "check", null, null, null);
      String json = new String(bytes, StandardCharsets.UTF_8);
      assertThat(json).contains("\"trace_id\":\"" + TRACE_ID + "\"");
    }
  }

  @Test
  void mdcTraceKeys_excludedFromMdcSection() {
    // Simulate what the OTEL agent does: inject trace_id, span_id, trace_flags into MDC
    MDC.put("trace_id", TRACE_ID);
    MDC.put("span_id", SPAN_ID);
    MDC.put("trace_flags", "01");
    MDC.put("customKey", "customValue");

    JsonEncoder encoder = buildEncoder(new NoopTraceContext());
    String json = encode(encoder, "mdc test", null);

    // trace_id/span_id/trace_flags from MDC should be excluded
    assertThat(json).doesNotContain("\"trace_id\"");
    assertThat(json).doesNotContain("\"span_id\"");
    // other MDC entries should still appear
    assertThat(json).contains("\"customKey\":\"customValue\"");
  }

  @Test
  void otelActiveSpan_withMdcDuplicates_noDuplicateFields() {
    // OTEL agent sets MDC AND we read from OTEL API — should only appear once
    MDC.put("trace_id", TRACE_ID);
    MDC.put("span_id", SPAN_ID);
    MDC.put("trace_flags", "01");

    JsonEncoder encoder = buildEncoder(new OtelTraceContext());

    SpanContext spanContext = SpanContext.create(TRACE_ID, SPAN_ID, TraceFlags.getSampled(), TraceState.getDefault());
    Span span = Span.wrap(spanContext);

    try (Scope ignored = span.makeCurrent()) {
      String json = encode(encoder, "no dups", null);

      // trace_id should appear exactly once (from OTEL API, not MDC)
      int firstIdx = json.indexOf("\"trace_id\"");
      int secondIdx = json.indexOf("\"trace_id\"", firstIdx + 1);
      assertThat(firstIdx).isGreaterThan(-1);
      assertThat(secondIdx).isEqualTo(-1);

      int firstSpanIdx = json.indexOf("\"span_id\"");
      int secondSpanIdx = json.indexOf("\"span_id\"", firstSpanIdx + 1);
      assertThat(firstSpanIdx).isGreaterThan(-1);
      assertThat(secondSpanIdx).isEqualTo(-1);
    }
  }
}
