package io.avaje.simplelogger.encoder;

import io.opentelemetry.api.trace.*;
import io.opentelemetry.context.Scope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OtelTraceContextTest {

  final OtelTraceContext ctx = new OtelTraceContext();

  @Test
  void noActiveSpan_traceId_returnsNull() {
    assertThat(ctx.traceId()).isNull();
  }

  @Test
  void noActiveSpan_spanId_returnsNull() {
    assertThat(ctx.spanId()).isNull();
  }

  @Test
  void activeSpan_returnsTraceId() {
    String expectedTraceId = "0af7651916cd43dd8448eb211c80319c";
    String expectedSpanId = "b7ad6b7169203331";

    SpanContext spanContext = SpanContext.create(expectedTraceId, expectedSpanId, TraceFlags.getSampled(), TraceState.getDefault());
    Span span = Span.wrap(spanContext);

    try (Scope ignored = span.makeCurrent()) {
      assertThat(ctx.traceId()).isEqualTo(expectedTraceId);
      assertThat(ctx.spanId()).isEqualTo(expectedSpanId);
    }
  }

  @Test
  void afterSpanEnds_returnsNull() {
    String traceId = "0af7651916cd43dd8448eb211c80319c";
    String spanId = "b7ad6b7169203331";

    SpanContext spanContext = SpanContext.create(traceId, spanId, TraceFlags.getSampled(), TraceState.getDefault());
    Span span = Span.wrap(spanContext);

    try (Scope ignored = span.makeCurrent()) {
      assertThat(ctx.traceId()).isNotNull();
    }
    // after scope closes, no active span
    assertThat(ctx.traceId()).isNull();
    assertThat(ctx.spanId()).isNull();
  }
}
