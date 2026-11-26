package io.avaje.simplelogger.otel;

import io.avaje.simplelogger.LogWriter;
import io.avaje.simplelogger.LogWriterProvider;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Configures the OTEL log writer using logger properties and standard OTEL environment variables.
 */
public final class OtelLogWriterProvider implements LogWriterProvider {

  private static final String PROPERTY_PREFIX = "logger.writer.otel.";
  private static final String LOGS_ENDPOINT = "OTEL_EXPORTER_OTLP_LOGS_ENDPOINT";
  private static final String BASE_ENDPOINT = "OTEL_EXPORTER_OTLP_ENDPOINT";

  @Override
  public String name() {
    return "otel";
  }

  @Override
  public LogWriter create(Properties properties) {
    String protocol = environment("OTEL_EXPORTER_OTLP_PROTOCOL", "http/protobuf");
    if (!"http/protobuf".equalsIgnoreCase(protocol)) {
      throw new IllegalStateException("The Avaje OTEL log writer supports only http/protobuf");
    }

    long exportTimeoutMillis = positiveLong(properties, "exportTimeoutMillis",
      environmentLong("OTEL_BLRP_EXPORT_TIMEOUT", environmentLong("OTEL_EXPORTER_OTLP_TIMEOUT", 10000)));
    long scheduleDelayMillis = positiveLong(properties, "scheduleDelayMillis",
      environmentLong("OTEL_BLRP_SCHEDULE_DELAY", 1000));
    int maxQueueSize = positiveInt(properties, "maxQueueSize",
      environmentInt("OTEL_BLRP_MAX_QUEUE_SIZE", 2048));
    int maxExportBatchSize = positiveInt(properties, "maxExportBatchSize",
      environmentInt("OTEL_BLRP_MAX_EXPORT_BATCH_SIZE", 512));

    OtlpHttpLogRecordExporter exporter = OtlpHttpLogRecordExporter.builder()
      .setEndpoint(endpoint(properties))
      .setTimeout(Duration.ofMillis(exportTimeoutMillis))
      .setConnectTimeout(Duration.ofMillis(exportTimeoutMillis))
      .setHeaders(OtelLogWriterProvider::headers)
      .build();

    BatchLogRecordProcessor processor = BatchLogRecordProcessor.builder(exporter)
      .setScheduleDelay(scheduleDelayMillis, TimeUnit.MILLISECONDS)
      .setExporterTimeout(exportTimeoutMillis, TimeUnit.MILLISECONDS)
      .setMaxQueueSize(maxQueueSize)
      .setMaxExportBatchSize(maxExportBatchSize)
      .build();

    SdkLoggerProvider loggerProvider = SdkLoggerProvider.builder()
      .setResource(resource())
      .addLogRecordProcessor(processor)
      .build();

    return new OtelLogWriter(loggerProvider);
  }

  private static String endpoint(Properties properties) {
    String endpoint = properties.getProperty(PROPERTY_PREFIX + "endpoint");
    if (endpoint != null && !endpoint.isBlank()) {
      return endpoint;
    }
    endpoint = System.getenv(LOGS_ENDPOINT);
    if (endpoint != null && !endpoint.isBlank()) {
      return endpoint;
    }
    String baseEndpoint = System.getenv(BASE_ENDPOINT);
    if (baseEndpoint == null || baseEndpoint.isBlank()) {
      throw new IllegalStateException("Set " + LOGS_ENDPOINT + ", " + BASE_ENDPOINT
        + ", or logger.writer.otel.endpoint when logger.writer=otel");
    }
    int end = baseEndpoint.length();
    while (end > 0 && baseEndpoint.charAt(end - 1) == '/') {
      end--;
    }
    return baseEndpoint.substring(0, end) + "/v1/logs";
  }

  private static Map<String, String> headers() {
    String value = System.getenv("OTEL_EXPORTER_OTLP_LOGS_HEADERS");
    if (value == null || value.isBlank()) {
      value = System.getenv("OTEL_EXPORTER_OTLP_HEADERS");
    }
    if (value == null || value.isBlank()) {
      return Map.of();
    }

    Map<String, String> result = new HashMap<>();
    for (String pair : value.split(",")) {
      int separator = pair.indexOf('=');
      if (separator <= 0) {
        throw new IllegalStateException("Invalid OTEL exporter header: " + pair);
      }
      result.put(decode(pair.substring(0, separator).trim()), decode(pair.substring(separator + 1).trim()));
    }
    return Map.copyOf(result);
  }

  private static Resource resource() {
    ResourceBuilder builder = Resource.getDefault().toBuilder();
    String resourceAttributes = System.getenv("OTEL_RESOURCE_ATTRIBUTES");
    if (resourceAttributes != null && !resourceAttributes.isBlank()) {
      for (String pair : resourceAttributes.split(",")) {
        int separator = pair.indexOf('=');
        if (separator <= 0) {
          throw new IllegalStateException("Invalid OTEL resource attribute: " + pair);
        }
        builder.put(pair.substring(0, separator).trim(), pair.substring(separator + 1).trim());
      }
    }

    String serviceName = System.getenv("OTEL_SERVICE_NAME");
    if (serviceName != null && !serviceName.isBlank()) {
      builder.put("service.name", serviceName);
    }
    String environmentName = System.getenv("OTEL_DEPLOYMENT_ENVIRONMENT_NAME");
    if (environmentName != null && !environmentName.isBlank()) {
      builder.put("deployment.environment.name", environmentName);
    }
    return builder.build();
  }

  private static String decode(String value) {
    return URLDecoder.decode(value, StandardCharsets.UTF_8);
  }

  private static long positiveLong(Properties properties, String name, long defaultValue) {
    String value = properties.getProperty(PROPERTY_PREFIX + name);
    if (value == null) {
      return defaultValue;
    }
    long parsed = Long.parseLong(value);
    if (parsed <= 0) {
      throw new IllegalArgumentException(PROPERTY_PREFIX + name + " must be positive");
    }
    return parsed;
  }

  private static int positiveInt(Properties properties, String name, int defaultValue) {
    String value = properties.getProperty(PROPERTY_PREFIX + name);
    if (value == null) {
      return defaultValue;
    }
    int parsed = Integer.parseInt(value);
    if (parsed <= 0) {
      throw new IllegalArgumentException(PROPERTY_PREFIX + name + " must be positive");
    }
    return parsed;
  }

  private static String environment(String name, String defaultValue) {
    String value = System.getenv(name);
    return value == null || value.isBlank() ? defaultValue : value;
  }

  private static long environmentLong(String name, long defaultValue) {
    String value = System.getenv(name);
    return value == null || value.isBlank() ? defaultValue : Long.parseLong(value);
  }

  private static int environmentInt(String name, int defaultValue) {
    String value = System.getenv(name);
    return value == null || value.isBlank() ? defaultValue : Integer.parseInt(value);
  }
}
