package io.avaje.simplelogger;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.helpers.Slf4jEnvUtil;

import static org.assertj.core.api.Assertions.assertThat;

class Slf4jVersionTest {

  @Disabled
  @Test
  void slf4jVersionTest() {
    String version = Slf4jEnvUtil.slf4jVersion();
    assertThat(version).isNotNull();
    assertThat(version).startsWith("2");
  }

}
