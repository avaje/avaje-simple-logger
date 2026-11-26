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
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Configures the OTEL log writer using logger properties and standard OTEL environment variables.
 */
public final class OtelLogWriterProvider implements LogWriterProvider {

  private static final String PROPERTY_PREFIX = "logger.writer.otel.";
  private static final String LOGS_ENDPOINT = "OTEL_EXPORTER_OTLP_LOGS_ENDPOINT";
  private static final String BASE_ENDPOINT = "OTEL_EXPORTER_OTLP_ENDPOINT";

  private final Function<String, String> env;

  public OtelLogWriterProvider() {
    this(System::getenv);
  }

  OtelLogWriterProvider(Function<String, String> env) {
    this.env = env;
  }

  @Override
  public String name() {
    return "otel";
  }

  @Override
  public LogWriter create(Properties properties) {
    String protocol = env("OTEL_EXPORTER_OTLP_LOGS_PROTOCOL", env("OTEL_EXPORTER_OTLP_PROTOCOL", "http/protobuf"));
    if (!"http/protobuf".equalsIgnoreCase(protocol)) {
      throw new IllegalStateException("The Avaje OTEL log writer supports only http/protobuf, got: " + protocol);
    }

    long exportTimeoutMillis = positiveLong(properties, "exportTimeoutMillis",
      envLong("OTEL_BLRP_EXPORT_TIMEOUT", envLong("OTEL_EXPORTER_OTLP_TIMEOUT", 10000)));
    long scheduleDelayMillis = positiveLong(properties, "scheduleDelayMillis",
      envLong("OTEL_BLRP_SCHEDULE_DELAY", 1000));
    int maxQueueSize = positiveInt(properties, "maxQueueSize",
      envInt("OTEL_BLRP_MAX_QUEUE_SIZE", 2048));
    int maxExportBatchSize = positiveInt(properties, "maxExportBatchSize",
      envInt("OTEL_BLRP_MAX_EXPORT_BATCH_SIZE", 512));

    Map<String, String> headers = headers();
    OtlpHttpLogRecordExporter exporter = OtlpHttpLogRecordExporter.builder()
      .setEndpoint(endpoint(properties))
      .setTimeout(Duration.ofMillis(exportTimeoutMillis))
      .setConnectTimeout(Duration.ofMillis(exportTimeoutMillis))
      .setHeaders(() -> headers)
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

  private String endpoint(Properties properties) {
    String endpoint = properties.getProperty(PROPERTY_PREFIX + "endpoint");
    if (endpoint != null && !endpoint.isBlank()) {
      return endpoint;
    }
    endpoint = env(LOGS_ENDPOINT, null);
    if (endpoint != null) {
      return endpoint;
    }
    String baseEndpoint = env(BASE_ENDPOINT, null);
    if (baseEndpoint == null) {
      throw new IllegalStateException("Set " + LOGS_ENDPOINT + ", " + BASE_ENDPOINT
        + ", or logger.writer.otel.endpoint when logger.writer=otel");
    }
    int end = baseEndpoint.length();
    while (end > 0 && baseEndpoint.charAt(end - 1) == '/') {
      end--;
    }
    return baseEndpoint.substring(0, end) + "/v1/logs";
  }

  private Map<String, String> headers() {
    String value = env("OTEL_EXPORTER_OTLP_LOGS_HEADERS", env("OTEL_EXPORTER_OTLP_HEADERS", null));
    if (value == null) {
      return Map.of();
    }
    Map<String, String> result = new HashMap<>();
    forEachPair(value, "header", result::put);
    return Map.copyOf(result);
  }

  Resource resource() {
    ResourceBuilder builder = Resource.getDefault().toBuilder();
    String resourceAttributes = env("OTEL_RESOURCE_ATTRIBUTES", null);
    if (resourceAttributes != null) {
      forEachPair(resourceAttributes, "resource attribute", builder::put);
    }
    String serviceName = env("OTEL_SERVICE_NAME", null);
    if (serviceName != null) {
      builder.put("service.name", serviceName);
    }
    // Non-standard convenience (not in the OTEL spec): equivalent to
    // OTEL_RESOURCE_ATTRIBUTES=deployment.environment.name=<value>, applied last so it wins.
    String environmentName = env("OTEL_DEPLOYMENT_ENVIRONMENT_NAME", null);
    if (environmentName != null) {
      builder.put("deployment.environment.name", environmentName);
    }
    return builder.build();
  }

  /** Parse W3C Baggage-style {@code key=value,key2=value2} with percent-decoded keys and values. */
  private static void forEachPair(String value, String kind, BiConsumer<String, String> consumer) {
    for (String pair : value.split(",")) {
      if (pair.isBlank()) {
        continue;
      }
      int separator = pair.indexOf('=');
      if (separator <= 0) {
        throw new IllegalStateException("Invalid OTEL " + kind + ": " + pair);
      }
      consumer.accept(decode(pair.substring(0, separator).trim()), decode(pair.substring(separator + 1).trim()));
    }
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

  private String env(String name, String defaultValue) {
    String value = env.apply(name);
    return value == null || value.isBlank() ? defaultValue : value;
  }

  private long envLong(String name, long defaultValue) {
    String value = env(name, null);
    return value == null ? defaultValue : Long.parseLong(value);
  }

  private int envInt(String name, int defaultValue) {
    String value = env(name, null);
    return value == null ? defaultValue : Integer.parseInt(value);
  }
}
