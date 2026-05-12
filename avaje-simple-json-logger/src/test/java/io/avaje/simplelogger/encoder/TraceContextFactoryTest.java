package io.avaje.simplelogger.encoder;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TraceContextFactoryTest {

  @Test
  void create_returnsOtelTraceContext_whenOtelApiOnClasspath() {
    assertThat(TraceContextFactory.create()).isInstanceOf(OtelTraceContext.class);
  }
}
