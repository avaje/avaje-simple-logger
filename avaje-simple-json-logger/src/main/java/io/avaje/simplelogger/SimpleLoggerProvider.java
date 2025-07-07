package io.avaje.simplelogger;

import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.helpers.BasicMDCAdapter;
import org.slf4j.helpers.BasicMarkerFactory;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;

/**
 * Provides the simple logger as SLF4J service provider.
 */
public final class SimpleLoggerProvider implements SLF4JServiceProvider {

  /**
   * Declare the version of the SLF4J API this implementation is compiled against.
   * The value of this field is modified with each major release.
   */
  // to avoid constant folding by the compiler, this field must *not* be final
  private static final String REQUESTED_API_VERSION = "2.0.99"; // !final

  private final ILoggerFactory loggerFactory;
  private final IMarkerFactory markerFactory;
  private final MDCAdapter mdcAdapter;

  public SimpleLoggerProvider() {
    this.markerFactory = new BasicMarkerFactory();
    this.mdcAdapter = new BasicMDCAdapter();
    this.loggerFactory = LoggerContext.get();
  }

  @Override
  public ILoggerFactory getLoggerFactory() {
    return loggerFactory;
  }

  @Override
  public IMarkerFactory getMarkerFactory() {
    return markerFactory;
  }

  @Override
  public MDCAdapter getMDCAdapter() {
    return mdcAdapter;
  }

  @Override
  public String getRequestedApiVersion() {
    return REQUESTED_API_VERSION;
  }

  @Override
  public void initialize() {
  }

}
