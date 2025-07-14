package io.avaje.simplelogger.dynamic;


import io.avaje.config.Config;
import io.avaje.config.Configuration;
import io.avaje.simplelogger.LoggerContext;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DynamicLogLevelsTest {

  static final Logger logFoo = LoggerFactory.getLogger("org.foo.MyFoo");
  static final Logger logBar = LoggerFactory.getLogger("org.bar.extra.MyBar");

  @Test
  void test() {
    Configuration configuration = Config.asConfiguration();
    assertThat(configuration).isNotNull();

    logFoo.debug("hi foo");
    logBar.debug("hi bar before");

    LoggerContext.get()
      .putAll(Map.of("org.bar.extra", "trace"));

    logBar.debug("hi bar after log level change");

    configuration.putAll(Map.of(
      "junk", "junk",
      "log.level.org.foo", "debug",
      "log.level.org.bar.extra", "warn"));

    logBar.debug("hi bar after dynamic log level change");
    logFoo.debug("hi foo last");
  }

}
