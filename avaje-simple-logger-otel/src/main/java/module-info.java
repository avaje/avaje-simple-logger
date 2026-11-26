module io.avaje.simplelogger.otel {

  requires io.avaje.simplelogger;
  requires io.opentelemetry.api;
  requires io.opentelemetry.context;
  requires io.opentelemetry.sdk;
  requires io.opentelemetry.sdk.common;
  requires io.opentelemetry.sdk.logs;
  requires io.opentelemetry.exporter.otlp;

  provides io.avaje.simplelogger.LogWriterProvider with io.avaje.simplelogger.otel.OtelLogWriterProvider;
}
