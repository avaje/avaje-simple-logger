# Guide: Add MDC, Fluent Key/Value, and OpenTelemetry Context to Logs

This guide shows how to enrich avaje-simple-logger output with request-scoped MDC fields, per-event fields from the SLF4J 2 fluent API, and distributed trace correlation from OpenTelemetry.

## Prerequisites

Before starting, verify the following:

- [ ] Your project is already using avaje-simple-logger
- [ ] You are using the SLF4J 2 API
- [ ] You have a place in your application where request or business context is available
- [ ] For trace correlation, an OpenTelemetry span is active when the log statement runs

If you have not set up avaje-simple-logger yet, start with [Add avaje-simple-logger to an Existing Maven Project](./add-avaje-simple-logger-to-maven-project.md).

## Choosing the Right Context Mechanism

Use the mechanism that matches the lifetime of the data:

| Use case | Recommended mechanism |
|---|---|
| Values that should follow several log lines in the same request or job | MDC |
| Values that belong to a single log statement | SLF4J fluent `addKeyValue()` |
| Distributed trace IDs and span IDs | OpenTelemetry active span |

## Step 1: Choose the Log Format

Use JSON in production when logs are shipped to a collector. Use plain format in tests or local development when console readability matters more.

**Production example (`src/main/resources/avaje-logger.properties`):**

```properties
logger.format=json
logger.naming=underscore
```

**Test example (`src/test/resources/avaje-logger-test.properties`):**

```properties
logger.format=plain
```

Both formats support:

- MDC fields
- SLF4J 2 fluent `addKeyValue()` fields
- OpenTelemetry trace correlation when the OpenTelemetry API is on the classpath and a span is active

## Step 2: Add Request-Scoped Fields with MDC

Use MDC for context that should appear on multiple log lines in the same logical scope, such as `requestId`, `tenant`, or `userId`.

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

class OrderService {

  private static final Logger log = LoggerFactory.getLogger(OrderService.class);

  void loadOrder(long orderId, String requestId, String tenant) {
    try (var ignoredRequest = MDC.putCloseable("requestId", requestId);
         var ignoredTenant = MDC.putCloseable("tenant", tenant)) {
      log.info("Loaded order {}", orderId);
      log.info("Validated order {}", orderId);
    }
  }
}
```

**Example JSON output:**

```json
{
  "level":"INFO",
  "logger_name":"com.example.OrderService",
  "message":"Loaded order 42",
  "requestId":"req-42",
  "tenant":"blue"
}
```

**Example plain output:**

```text
2026-05-13T10:00:00+12:00 INFO com.example.OrderService - requestId=req-42 tenant=blue Loaded order 42
```

## Step 3: Add Per-Event Fields with SLF4J Fluent `addKeyValue()`

Use the fluent API for values that belong only to one log event.

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class OrderService {

  private static final Logger log = LoggerFactory.getLogger(OrderService.class);

  void completeOrder(long orderId) {
    log.atInfo()
      .addKeyValue("orderId", orderId)
      .addKeyValue("processed", true)
      .log("Order processed");
  }
}
```

**Example JSON output:**

```json
{
  "level":"INFO",
  "logger_name":"com.example.OrderService",
  "message":"Order processed",
  "orderId":42,
  "processed":true
}
```

**Example plain output:**

```text
2026-05-13T10:00:00+12:00 INFO com.example.OrderService - orderId=42 processed=true Order processed
```

## Step 4: Add OpenTelemetry Trace Correlation

avaje-simple-logger writes `trace_id` and `span_id` from the active OpenTelemetry span. No extra logger property is required.

If your tracing setup does not already provide the OpenTelemetry API on the classpath, add it explicitly and align the version with your OpenTelemetry BOM or distribution:

```xml
<dependency>
  <groupId>io.opentelemetry</groupId>
  <artifactId>opentelemetry-api</artifactId>
  <version>${opentelemetry.version}</version>
</dependency>
```

Once a span is current, ordinary log statements automatically include trace correlation:

```java
log.info("Handling order {}", orderId);
```

**Example JSON output:**

```json
{
  "level":"INFO",
  "logger_name":"com.example.OrderService",
  "message":"Handling order 42",
  "trace_id":"0af7651916cd43dd8448eb211c80319c",
  "span_id":"b7ad6b7169203331"
}
```

**Example plain output:**

```text
2026-05-13T10:00:00+12:00 INFO com.example.OrderService - trace_id=0af7651916cd43dd8448eb211c80319c span_id=b7ad6b7169203331 Handling order 42
```

If your OpenTelemetry distribution also places `trace_id` and `span_id` into MDC, avaje-simple-logger filters those MDC keys so the trace fields are emitted only once.

## Step 5: Understand Field Names and Plain Output Ordering

### JSON field naming

The built-in JSON fields follow `logger.naming` and `logger.propertyNames`.

- Default naming: `trace_id`, `span_id`
- Camel naming: `traceId`, `spanId`
- Custom property mapping: `logger.propertyNames=trace_id=traceIdentifier;span_id=spanIdentifier`

MDC keys and fluent `addKeyValue()` keys keep the exact names you provide. They are not renamed by `logger.naming` or `logger.propertyNames`.

### Plain output ordering

For `logger.format=plain`, contextual data is written before the message in this order:

1. `trace_id` and `span_id` from the active span
2. Remaining MDC entries
3. Fluent `addKeyValue()` entries
4. The rendered message

For example:

```text
2026-05-13T10:00:00+12:00 INFO com.example.OrderService - trace_id=0af7651916cd43dd8448eb211c80319c span_id=b7ad6b7169203331 requestId=req-42 orderId=42 Order processed
```

## Step 6: Verify the Behavior

1. Run your application or tests with `logger.format=json` and confirm MDC and fluent fields appear as structured JSON fields.
2. Run with `logger.format=plain` and confirm the same fields appear before the message.
3. With an active OpenTelemetry span, confirm `trace_id` and `span_id` appear exactly once.
4. If you changed `logger.naming` or `logger.propertyNames`, confirm the built-in trace field names match that configuration.

## Troubleshooting

### Issue: MDC fields are missing

**Cause:** The MDC values were added outside the scope of the log call, or they were removed too early.

**Solution:**

1. Add MDC values immediately before the logging scope.
2. Use `MDC.putCloseable()` with try-with-resources to avoid leaking or clearing context at the wrong time.
3. Verify the log statement runs inside that scope.

### Issue: Fluent key/value fields are missing

**Cause:** The log statement was emitted with the classic SLF4J methods rather than the fluent API.

**Solution:**

1. Use `logger.atInfo()` / `logger.atDebug()` and call `addKeyValue()` before `log(...)`.
2. Confirm the values are added on the same log event that should contain them.

### Issue: Trace fields are missing

**Cause:** There is no active OpenTelemetry span, or the OpenTelemetry API is not on the classpath.

**Solution:**

1. Verify your tracing instrumentation makes a span current during the log call.
2. Ensure `io.opentelemetry:opentelemetry-api` is present, or already provided by your tracing distribution.
3. Remember that avaje-simple-logger reads trace fields from the active span rather than directly from MDC.

### Issue: Trace fields have unexpected names

**Cause:** `logger.naming` or `logger.propertyNames` changed the built-in trace field names.

**Solution:**

1. Check `logger.naming` for underscore, camel, or legacy mode.
2. Check `logger.propertyNames` for custom mappings of `trace_id` and `span_id`.
3. Remember that MDC keys and fluent key/value keys still keep their original names.

### Issue: AppConfig changed the log level but did not add context fields

**Cause:** AWS AppConfig changes log levels, but MDC fields, fluent key/value fields, and OpenTelemetry spans still come from application code and tracing instrumentation.

**Solution:**

1. Keep using AppConfig for `log.level.*` changes.
2. Add MDC and fluent fields in your log statements.
3. Ensure OpenTelemetry tracing is active if you expect `trace_id` and `span_id`.

For dynamic log level configuration, see [Add AWS AppConfig to Your Project](./add-aws-appconfig-to-project.md).
