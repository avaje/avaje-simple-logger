package io.avaje.simplelogger.encoder;

import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.LegacyAbstractLogger;

import static org.slf4j.spi.LocationAwareLogger.*;

final class SimpleLogger extends LegacyAbstractLogger {

  private final LogWriter writer;
  private final String shortName;
  private int level;

  SimpleLogger(LogWriter writer, String name, String shortName, int level) {
    this.writer = writer;
    this.name = name;
    this.shortName = shortName;
    this.level = level;
  }

  void setNewLevel(int newLevel) {
    // atomic assignment
    this.level = newLevel;
  }

  @Override
  public boolean isTraceEnabled() {
    return TRACE_INT >= level;
  }

  @Override
  public boolean isDebugEnabled() {
    return DEBUG_INT >= level;
  }

  @Override
  public boolean isInfoEnabled() {
    return INFO_INT >= level;
  }

  @Override
  public boolean isWarnEnabled() {
    return WARN_INT >= level;
  }

  @Override
  public boolean isErrorEnabled() {
    return ERROR_INT >= level;
  }

  @Override
  protected String getFullyQualifiedCallerName() {
    return null;
  }

  @Override
  protected void handleNormalizedLoggingCall(Level level, Marker marker, String messagePattern, Object[] arguments, Throwable throwable) {
//        List<Marker> markers = null;
//        if (marker != null) {
//            markers = new ArrayList<>();
//            markers.add(marker);
//        }
    logNormalized(level, messagePattern, arguments, throwable);
  }

  private void logNormalized(Level level, String messagePattern, Object[] arguments, Throwable t) {
    writer.log(shortName, level, messagePattern, arguments, t);
  }

//    public void log(LoggingEvent event) {
//        int levelInt = event.getLevel().toInt();
//        if (!(levelInt >= currentLogLevel)) {
//            return;
//        }
//        NormalizedParameters np = NormalizedParameters.normalize(event);
//        logNormalized(event.getLevel(), np.getMessage(), np.getArguments(), event.getThrowable());
//    }

}
