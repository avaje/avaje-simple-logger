package io.avaje.simplelogger.encoder;

/**
 * No-op TraceContext used when OpenTelemetry API is not on the classpath.
 */
final class NoopTraceContext implements TraceContext {

  @Override
  public String traceId() {
    return null;
  }

  @Override
  public String spanId() {
    return null;
  }
}
