package io.avaje.simplelogger.encoder;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NoopTraceContextTest {

  final NoopTraceContext ctx = new NoopTraceContext();

  @Test
  void traceId_returnsNull() {
    assertThat(ctx.traceId()).isNull();
  }

  @Test
  void spanId_returnsNull() {
    assertThat(ctx.spanId()).isNull();
  }
}
