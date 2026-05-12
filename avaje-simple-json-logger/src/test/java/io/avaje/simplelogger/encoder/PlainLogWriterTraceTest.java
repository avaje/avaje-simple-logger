package io.avaje.simplelogger.encoder;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Scope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.slf4j.event.Level;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

class PlainLogWriterTraceTest {

  static final String TRACE_ID = "0af7651916cd43dd8448eb211c80319c";
  static final String SPAN_ID = "b7ad6b7169203331";

  @AfterEach
  void cleanup() {
    MDC.clear();
  }

  @Test
  void noopTraceContext_traceFieldsAbsent() {
    String logLine = logLine(new NoopTraceContext(), "hello");

    assertThat(logLine).doesNotContain("trace_id=");
    assertThat(logLine).doesNotContain("span_id=");
    assertThat(logLine).contains("hello");
  }

  @Test
  void otelTraceContext_withActiveSpan_traceFieldsPresent() {
    SpanContext spanContext = SpanContext.create(TRACE_ID, SPAN_ID, TraceFlags.getSampled(), TraceState.getDefault());
    Span span = Span.wrap(spanContext);

    try (Scope ignored = span.makeCurrent()) {
      String logLine = logLine(new OtelTraceContext(), "traced message");

      int messageIndex = logLine.indexOf("traced message");
      assertThat(logLine).contains("trace_id=" + TRACE_ID);
      assertThat(logLine).contains("span_id=" + SPAN_ID);
      assertThat(logLine.indexOf("trace_id=" + TRACE_ID)).isLessThan(messageIndex);
      assertThat(logLine.indexOf("span_id=" + SPAN_ID)).isLessThan(messageIndex);
    }
  }

  @Test
  void otelActiveSpan_withMdcDuplicates_noDuplicateFields() {
    MDC.put("trace_id", TRACE_ID);
    MDC.put("span_id", SPAN_ID);
    MDC.put("trace_flags", "01");
    MDC.put("requestId", "req-42");

    SpanContext spanContext = SpanContext.create(TRACE_ID, SPAN_ID, TraceFlags.getSampled(), TraceState.getDefault());
    Span span = Span.wrap(spanContext);

    try (Scope ignored = span.makeCurrent()) {
      String logLine = logLine(new OtelTraceContext(), "no dups");

      assertThat(occurrences(logLine, "trace_id=" + TRACE_ID)).isEqualTo(1);
      assertThat(occurrences(logLine, "span_id=" + SPAN_ID)).isEqualTo(1);
      assertThat(logLine).contains("trace_flags=01");
      assertThat(logLine).contains("requestId=req-42");
    }
  }

  private String logLine(TraceContext traceContext, String message) {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    PlainLogWriter writer = new PlainLogWriter(new PrintStream(output), DateTimeFormatter.ISO_OFFSET_DATE_TIME, false, traceContext);

    writer.log("test.Logger", Level.INFO, message, null, null, null);
    return output.toString(StandardCharsets.UTF_8);
  }

  private int occurrences(String source, String token) {
    int count = 0;
    int index = source.indexOf(token);
    while (index != -1) {
      count++;
      index = source.indexOf(token, index + token.length());
    }
    return count;
  }
}
