package io.avaje.simplelogger.encoder;

import io.avaje.json.stream.JsonStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.slf4j.event.KeyValuePair;
import org.slf4j.event.Level;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.spi.LocationAwareLogger.INFO_INT;

class FluentKeyValueTest {

  private static final String TRACE_ID = "0af7651916cd43dd8448eb211c80319c";
  private static final String SPAN_ID = "b7ad6b7169203331";

  @AfterEach
  void cleanup() {
    MDC.clear();
  }

  @Test
  void simpleLoggerFluent_addKeyValue_passesStructuredPairsToWriter() {
    CapturingLogWriter writer = new CapturingLogWriter();
    SimpleLogger logger = new SimpleLogger(writer, "test.logger.Name", "Name", INFO_INT);

    logger.atInfo()
      .addKeyValue("orderId", 42)
      .addKeyValue("processed", true)
      .log("hello {}", "world");

    assertThat(writer.level).isEqualTo(Level.INFO);
    assertThat(writer.loggerName).isEqualTo("Name");
    assertThat(writer.messagePattern).isEqualTo("hello {}");
    assertThat(writer.arguments).containsExactly("world");
    assertThat(writer.throwable).isNull();
    assertThat(writer.keyValuePairs).hasSize(2);
    assertThat(writer.keyValuePairs.get(0).key).isEqualTo("orderId");
    assertThat(writer.keyValuePairs.get(0).value).isEqualTo(42);
    assertThat(writer.keyValuePairs.get(1).key).isEqualTo("processed");
    assertThat(writer.keyValuePairs.get(1).value).isEqualTo(true);
  }

  @Test
  void jsonEncoder_addKeyValue_writesStructuredFields() {
    JsonEncoder encoder = buildEncoder();
    List<KeyValuePair> keyValuePairs = List.of(
      new KeyValuePair("orderId", 42),
      new KeyValuePair("processed", true),
      new KeyValuePair("note", "done"),
      new KeyValuePair("amount", 12.5d),
      new KeyValuePair("valueObject", new NamedValue("custom"))
    );

    String json = new String(
      encoder.encode("test.Logger", Level.INFO, "hello {}", new Object[]{"world"}, null, keyValuePairs),
      StandardCharsets.UTF_8
    );

    assertThat(json).contains("\"message\":\"hello world\"");
    assertThat(json).contains("\"orderId\":42");
    assertThat(json).contains("\"processed\":true");
    assertThat(json).contains("\"note\":\"done\"");
    assertThat(json).contains("\"amount\":12.5");
    assertThat(json).contains("\"valueObject\":\"custom\"");
  }

  @Test
  void plainLogWriter_withoutStructuredContext_preservesMessage() {
    String logLine = plainLogLine(null);

    assertThat(logLine).endsWith("INFO test.Logger - hello world\n");
  }

  @Test
  void plainLogWriter_addKeyValue_prefixesMessage() {
    String logLine = plainLogLine(List.of(new KeyValuePair("orderId", 42), new KeyValuePair("processed", true)));

    assertThat(logLine).contains("orderId=42 processed=true hello world");
  }

  @Test
  void plainLogWriter_mdc_precedesFluentKeyValuesAndMessage() {
    MDC.put("requestId", "req-42");

    String logLine = plainLogLine(List.of(new KeyValuePair("orderId", 42), new KeyValuePair("processed", true)));

    assertThat(logLine).contains("requestId=req-42 orderId=42 processed=true hello world");
  }

  @Test
  void plainLogWriter_traceKeysFromMdc_areRenderedBeforeMessage() {
    MDC.put("trace_id", TRACE_ID);
    MDC.put("span_id", SPAN_ID);

    String logLine = plainLogLine(null);

    int messageIndex = logLine.indexOf("hello world");
    assertThat(logLine).contains("trace_id=" + TRACE_ID);
    assertThat(logLine).contains("span_id=" + SPAN_ID);
    assertThat(logLine.indexOf("trace_id=" + TRACE_ID)).isLessThan(messageIndex);
    assertThat(logLine.indexOf("span_id=" + SPAN_ID)).isLessThan(messageIndex);
  }

  private JsonEncoder buildEncoder() {
    String[] propertyNames = JsonEncoderBuilder.basePropertyNames(null);
    JsonStream json = JsonStream.builder().build();
    DateTimeFormatter formatter = TimeZoneUtils.jsonFormatter(null, TimeZone.getDefault().toZoneId());
    StackHasher stackHasher = new StackHasher(StackElementFilter.builder().allFilters().build());
    return new JsonEncoder(
      propertyNames,
      json,
      null,
      null,
      stackHasher,
      formatter,
      true,
      new HashMap<>(),
      new ThrowableConverter(),
      new NoopTraceContext()
    );
  }

  private String plainLogLine(List<KeyValuePair> keyValuePairs) {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    PlainLogWriter writer = new PlainLogWriter(new PrintStream(output), DateTimeFormatter.ISO_OFFSET_DATE_TIME, false);

    writer.log(
      "test.Logger",
      Level.INFO,
      "hello {}",
      new Object[]{"world"},
      null,
      keyValuePairs
    );

    return output.toString(StandardCharsets.UTF_8);
  }

  private static final class CapturingLogWriter implements LogWriter {
    private String loggerName;
    private Level level;
    private String messagePattern;
    private Object[] arguments;
    private Throwable throwable;
    private List<KeyValuePair> keyValuePairs;

    @Override
    public void log(String loggerName, Level level, String messagePattern, Object[] arguments, Throwable t, List<KeyValuePair> keyValuePairs) {
      this.loggerName = loggerName;
      this.level = level;
      this.messagePattern = messagePattern;
      this.arguments = arguments == null ? null : arguments.clone();
      this.throwable = t;
      this.keyValuePairs = keyValuePairs == null ? null : new ArrayList<>(keyValuePairs);
    }
  }

  private static final class NamedValue {
    private final String value;

    private NamedValue(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return value;
    }
  }
}
