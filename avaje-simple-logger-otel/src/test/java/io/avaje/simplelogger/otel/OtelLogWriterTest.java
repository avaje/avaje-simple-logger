package io.avaje.simplelogger.otel;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.slf4j.event.KeyValuePair;
import org.slf4j.event.Level;

import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OtelLogWriterTest {

  @AfterEach
  void clearMdc() {
    MDC.clear();
  }

  @Test
  void writesLogRecordWithContextAndStructuredFields() {
    InMemoryLogRecordExporter exporter = InMemoryLogRecordExporter.create();
    SdkLoggerProvider loggerProvider = SdkLoggerProvider.builder()
      .addLogRecordProcessor(SimpleLogRecordProcessor.create(exporter))
      .build();
    OtelLogWriter writer = new OtelLogWriter(loggerProvider);

    MDC.put("request.id", "request-42");
    SpanContext spanContext = SpanContext.create(
      "0af7651916cd43dd8448eb211c80319c",
      "b7ad6b7169203331",
      TraceFlags.getSampled(),
      TraceState.getDefault());

    try (Scope ignored = Span.wrap(spanContext).makeCurrent()) {
      writer.log("example.Service", "Service", Level.ERROR, "processing order {}",
        new Object[]{42}, new IllegalStateException("broken"),
        List.of(new KeyValuePair("attempt", 3)));
    }

    List<LogRecordData> records = exporter.getFinishedLogRecordItems();
    assertThat(records).hasSize(1);
    LogRecordData record = records.get(0);
    assertThat(record.getBody().asString()).isEqualTo("processing order 42");
    assertThat(record.getSeverity()).isEqualTo(Severity.ERROR);
    assertThat(record.getInstrumentationScopeInfo().getName()).isEqualTo("example.Service");
    assertThat(record.getSpanContext().getTraceId()).isEqualTo(spanContext.getTraceId());
    assertThat(record.getSpanContext().getSpanId()).isEqualTo(spanContext.getSpanId());
    assertThat(record.getAttributes().get(AttributeKey.stringKey("request.id"))).isEqualTo("request-42");
    assertThat(record.getAttributes().get(AttributeKey.longKey("attempt"))).isEqualTo(3L);
    assertThat(record.getAttributes().get(AttributeKey.stringKey("exception.type")))
      .isEqualTo(IllegalStateException.class.getName());
    assertThat(record.getAttributes().get(AttributeKey.stringKey("exception.message"))).isEqualTo("broken");
    assertThat(writer.forceFlush(1000)).isTrue();
    loggerProvider.close();
  }

  @Test
  void validatesPositiveExportTimeout() {
    Properties properties = new Properties();
    properties.put("logger.writer.otel.exportTimeoutMillis", "0");
    OtelLogWriterProvider provider = new OtelLogWriterProvider();

    assertThatThrownBy(() -> provider.create(properties))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("logger.writer.otel.exportTimeoutMillis must be positive");
  }
}
