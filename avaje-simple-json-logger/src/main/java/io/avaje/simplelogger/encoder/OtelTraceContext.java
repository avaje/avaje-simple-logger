package io.avaje.simplelogger.encoder;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;

/**
 * TraceContext implementation that reads from the OpenTelemetry API.
 */
final class OtelTraceContext implements TraceContext {

  @Override
  public String traceId() {
    SpanContext ctx = spanContext();
    return ctx != null ? ctx.getTraceId() : null;
  }

  @Override
  public String spanId() {
    SpanContext ctx = spanContext();
    return ctx != null ? ctx.getSpanId() : null;
  }

  private static SpanContext spanContext() {
    SpanContext ctx = Span.current().getSpanContext();
    return ctx.isValid() ? ctx : null;
  }
}
