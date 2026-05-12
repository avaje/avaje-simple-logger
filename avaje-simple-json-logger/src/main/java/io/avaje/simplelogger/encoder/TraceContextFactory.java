package io.avaje.simplelogger.encoder;

final class TraceContextFactory {

  private TraceContextFactory() {}

  static TraceContext create() {
    try {
      Class.forName("io.opentelemetry.api.trace.Span");
      return (TraceContext) Class.forName("io.avaje.simplelogger.encoder.OtelTraceContext")
        .getDeclaredConstructor()
        .newInstance();
    } catch (Exception e) {
      return new NoopTraceContext();
    }
  }
}
