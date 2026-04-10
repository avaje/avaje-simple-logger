package io.avaje.simplelogger.encoder;

/**
 * Provides trace context (trace_id and span_id) for structured log output.
 */
interface TraceContext {

  /**
   * Return the current trace ID, or null if not available.
   */
  String traceId();

  /**
   * Return the current span ID, or null if not available.
   */
  String spanId();
}
