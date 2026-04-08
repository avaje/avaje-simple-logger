package io.avaje.simplelogger.encoder;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class JsonEncoderBuilderTest {

  String[] baseKeys = {"component", "env", "timestamp", "level", "logger", "message", "thread", "exceptionType", "exceptionMessage", "stackhash", "stacktrace"};

  @Test
  void toPropertyNames_when_null() {
    String[] originalKeys = JsonEncoderBuilder.basePropertyNames(null);
    String[] names = JsonEncoderBuilder.toPropertyNames(originalKeys, null);
    assertThat(Arrays.equals(names, baseKeys)).isTrue();
  }

  @Test
  void toPropertyNames_several() {
    String[] originalKeys = JsonEncoderBuilder.basePropertyNames(null);
    String[] names = JsonEncoderBuilder.toPropertyNames(originalKeys,"component = c; env=e,thread=_foo ,exceptionType=exType ");
    assertThat(names).isEqualTo(new String[]{"c", "e", "timestamp", "level", "logger", "message", "_foo", "exType", "exceptionMessage", "stackhash", "stacktrace"});
  }

  @Test
  void toPropertyNames_onlyOne() {
    String[] originalKeys = JsonEncoderBuilder.basePropertyNames(null);
    String[] names = JsonEncoderBuilder.toPropertyNames(originalKeys, "logger=loggerName");
    assertThat(names).isEqualTo(new String[]{"component", "env", "timestamp", "level", "loggerName", "message", "thread", "exceptionType", "exceptionMessage", "stackhash", "stacktrace"});
  }

  @Test
  void basePropertyNames() {
    String[] keys = JsonEncoderBuilder.basePropertyNames(null);
    assertThat(keys).isEqualTo(new String[]{"component", "env", "timestamp", "level", "logger", "message", "thread", "exceptionType", "exceptionMessage", "stackhash", "stacktrace"});
  }

  @Test
  void basePropertyNames_underscore() {
    String[] keys = JsonEncoderBuilder.basePropertyNames("underscore");
    assertThat(keys).isEqualTo(new String[]{"component", "env", "timestamp", "level", "logger_name", "message", "thread", "exception_type", "exception_message", "exception_stackhash", "exception_stacktrace"});
  }


  @Test
  void basePropertyNames_camal() {
    String[] keys = JsonEncoderBuilder.basePropertyNames("camel");
    assertThat(keys).isEqualTo(new String[]{"component", "env", "timestamp", "level", "loggerName", "message", "thread", "exceptionType", "exceptionMessage", "exceptionStackhash", "exceptionStacktrace"});
  }
}
