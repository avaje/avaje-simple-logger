package io.avaje.simplelogger.encoder;

import org.slf4j.event.Level;
import org.slf4j.event.KeyValuePair;

import java.util.List;

interface LogWriter {

  void log(String loggerName, Level level, String messagePattern, Object[] arguments, Throwable t, List<KeyValuePair> keyValuePairs);

}
