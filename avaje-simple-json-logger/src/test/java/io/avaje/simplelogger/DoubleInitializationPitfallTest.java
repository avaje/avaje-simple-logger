package io.avaje.simplelogger;

import org.junit.jupiter.api.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.MarkerFactory;

import static org.junit.jupiter.api.Assertions.fail;

// See https://jira.qos.ch/browse/SLF4J-463
public class DoubleInitializationPitfallTest {

  // See https://jira.qos.ch/browse/SLF4J-463
  @Test
  public void verifyImpactOfMarkerFactory() {
    ILoggerFactory firstFactory = LoggerFactory.getILoggerFactory();
    MarkerFactory.getMarker("DOUBLE_INIT");
    ILoggerFactory secondFactory = LoggerFactory.getILoggerFactory();

    if (firstFactory != secondFactory) {
      fail("MarkerFactory.getMarker causes multiple provider initialization");
    }
  }

  @Test
  public void verifyImpactOfMDC() {
    ILoggerFactory firstFactory = LoggerFactory.getILoggerFactory();
    MDC.put("DoubleInitializationPitfallTest", "a");
    ILoggerFactory secondFactory = LoggerFactory.getILoggerFactory();

    if (firstFactory != secondFactory) {
      fail("MarkerFactory.getMarker causes multiple provider initialization");
    }
  }

}
