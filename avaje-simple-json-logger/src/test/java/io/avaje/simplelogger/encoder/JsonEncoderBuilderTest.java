package io.avaje.simplelogger.encoder;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class JsonEncoderBuilderTest {

  String[] baseKeys = {"component", "env", "timestamp", "level", "logger", "message", "thread", "stackhash", "stacktrace"};

  @Test
  void toPropertyNames_when_null() {
    String[] names = JsonEncoderBuilder.toPropertyNames(null);
    assertThat(Arrays.equals(names, baseKeys)).isTrue();
  }

  @Test
  void toPropertyNames_several() {
    String[] names = JsonEncoderBuilder.toPropertyNames("component = c; env=e,thread=_foo  ");
    assertThat(names).isEqualTo(new String[]{"c", "e", "timestamp", "level", "logger", "message", "_foo", "stackhash", "stacktrace"});
  }

  @Test
  void toPropertyNames_onlyOne() {
    String[] names = JsonEncoderBuilder.toPropertyNames("logger=loggerName");
    assertThat(names).isEqualTo(new String[]{"component", "env", "timestamp", "level", "loggerName", "message", "thread", "stackhash", "stacktrace"});
  }
}
