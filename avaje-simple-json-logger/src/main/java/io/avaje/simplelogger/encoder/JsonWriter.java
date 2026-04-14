package io.avaje.simplelogger.encoder;

import org.slf4j.event.Level;
import org.slf4j.event.KeyValuePair;
import org.slf4j.helpers.Reporter;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

final class JsonWriter implements LogWriter {

  private final JsonEncoder encoder;
  private final PrintStream out;

  JsonWriter(JsonEncoder encoder, PrintStream out) {
    this.encoder = encoder;
    this.out = out;
  }

  @Override
  public void log(String loggerName, Level level, String messagePattern, Object[] arguments, Throwable t, List<KeyValuePair> keyValuePairs) {
    try {
      out.write(encoder.encode(loggerName, level, messagePattern, arguments, t, keyValuePairs));
    } catch (IOException e) {
      Reporter.error("Failed to write to log", e);
    }
  }
}
