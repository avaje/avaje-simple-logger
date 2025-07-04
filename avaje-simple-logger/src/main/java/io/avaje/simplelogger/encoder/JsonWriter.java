package io.avaje.simplelogger.encoder;

import org.slf4j.event.Level;

import java.io.IOException;
import java.io.PrintStream;

final class JsonWriter implements LogWriter {

  private final JsonEncoder encoder;
  private final PrintStream out;

  JsonWriter(JsonEncoder encoder, PrintStream out) {
    this.encoder = encoder;
    this.out = out;
  }

  @Override
  public void log(String loggerName, Level level, String messagePattern, Object[] arguments, Throwable t) {
    try {
      out.write(encoder.encode(loggerName, level, messagePattern, arguments, t));
    } catch (IOException e) {
      System.err.println("Failed to write to log " + e);
      e.printStackTrace(System.err);
    }
  }
}
