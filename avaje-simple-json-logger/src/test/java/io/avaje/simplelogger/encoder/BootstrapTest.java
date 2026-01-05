package io.avaje.simplelogger.encoder;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class BootstrapTest {


  @Test
  void loadProperties_when_mergeNoClash() {
    System.setProperty("logger.config", "src/test/resources/test-external.properties");
    Properties props = Bootstrap.loadProperties();
    System.clearProperty("logger.config");

    // merged all entries, no clashes
    assertThat(props).hasSize(3);

    assertThat(props.getProperty("log.level.io.banana")).isEqualTo("trace");
    // entries from default properties
    assertThat(props.getProperty("logger.defaultLogLevel")).isEqualTo("debug");
    assertThat(props.getProperty("log.level.io.avaje")).isEqualTo("warn");
  }

  @Test
  void loadProperties_when_merge() {
    System.setProperty("logger.config", "src/test/resources/test-external-merge.properties");
    Properties props = Bootstrap.loadProperties();
    System.clearProperty("logger.config");

    // one merged entry from default avaje-logger.properties
    assertThat(props).hasSize(4);

    assertThat(props.getProperty("log.level.io.apple")).isEqualTo("trace");
    assertThat(props.getProperty("log.level.io.banana")).isEqualTo("trace");
    assertThat(props.getProperty("logger.defaultLogLevel")).isEqualTo("info");
    // entries from default properties
    assertThat(props.getProperty("log.level.io.avaje")).isEqualTo("warn");
  }

  @Test
  void loadProperties_when_override() {
    System.setProperty("logger.config", "src/test/resources/test-external-merge.properties");
    System.setProperty("logger.config.merge", "false");
    Properties props = Bootstrap.loadProperties();
    System.clearProperty("logger.config");
    System.clearProperty("logger.config.merge");

    // no entries from default avaje-logger.properties
    assertThat(props).hasSize(3);

    assertThat(props.getProperty("log.level.io.apple")).isEqualTo("trace");
    assertThat(props.getProperty("log.level.io.banana")).isEqualTo("trace");
    assertThat(props.getProperty("logger.defaultLogLevel")).isEqualTo("info");
  }

  @Test
  void loadExternal() {
    Properties props = Bootstrap.loadExternal("src/test/resources/test-external.properties");

    assertThat(props).hasSize(1);

    String value = props.getProperty("log.level.io.banana");
    assertThat(value).isEqualTo("trace");
  }

  @Test
  void loadExternal_when_missingPropertiesSuffix_expect_addsPropertiesExtension() {
    Properties props = Bootstrap.loadExternal("src/test/resources/test-external");

    assertThat(props).hasSize(1);

    String value = props.getProperty("log.level.io.banana");
    assertThat(value).isEqualTo("trace");
  }
}
