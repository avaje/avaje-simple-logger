package io.avaje.simplelogger.encoder;

import io.avaje.simplelogger.LoggerContext;
import org.slf4j.helpers.Reporter;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Bootstrap the simple logger.
 */
public final class Bootstrap {

  public static LoggerContext init() {
    Properties properties = loadProperties();
    LogWriter logWriter = createWriter(properties, logFormat(properties));

    String info = properties.getProperty("logger.defaultLogLevel", "info");
    int defaultLevel = SimpleLoggerFactory.stringToLevel(info);

    String nameLength = properties.getProperty("logger.nameTargetLength", "full");
    Abbreviator abbreviator = Abbreviator.create(nameLength);

    final Map<String, String> nameLevels = initialNameLevels(properties);
    return new SimpleLoggerFactory(logWriter, abbreviator, defaultLevel, nameLevels);
  }

  private static String logFormat(Properties properties) {
    return properties.getProperty("logger.format", "json");
  }

  private static Map<String, String> initialNameLevels(Properties properties) {
    Map<String, String> nameLevels = new HashMap<>();
    for (String key : properties.stringPropertyNames()) {
      if (key.startsWith("log.level.")) {
        String val = properties.getProperty(key);
        if (val != null) {
          nameLevels.put(key.substring(10), val);
        }
      }
    }
    return nameLevels;
  }

  private static LogWriter createWriter(Properties properties, String writerType) {
    final PrintStream target = System.out;
    final TimeZone timeZone = TimeZoneUtils.parseTimeZone(property(properties, "logger.timezone"));
    final String timestampPattern = property(properties, "logger.timestampPattern");
    if ("plain".equalsIgnoreCase(writerType)) {
      final DateTimeFormatter formatter = TimeZoneUtils.plainFormatter(timestampPattern, timeZone.toZoneId());
      final boolean showThreadName = propertyShowThreadName(properties);
      return new PlainLogWriter(target, formatter, showThreadName);
    }
    var jsonEncoder = new JsonEncoderBuilder()
      .component(property(properties, "logger.component"))
      .environment(property(properties,"logger.environment"))
      .propertyNames(property(properties,"logger.propertyNames"))
      .customFields(property(properties, "logger.customFields"))
      .timestampPattern(timestampPattern)
      .timeZone(timeZone)
      .build();
    return new JsonWriter(jsonEncoder, target);
  }

  private static String property(Properties properties, String key) {
    return Eval.eval(properties.getProperty(key));
  }

  private static boolean propertyShowThreadName(Properties properties) {
    return Boolean.parseBoolean(Eval.eval(properties.getProperty("logger.showThreadName", "true")));
  }

  private static Properties loadProperties() {
    final var properties = new Properties();
    load(properties, "avaje-logger.properties");
    load(properties, "avaje-logger-test.properties");
    return properties;
  }

  private static void load(Properties properties, String resourceName) {
    InputStream is = resource(resourceName);
    if (is != null) {
      try {
        properties.load(is);
      } catch (IOException e) {
        Reporter.error("Error loading " + resourceName, e);
      }
    }
  }

  private static InputStream resource(String resourceName) {
    ClassLoader threadCL = Thread.currentThread().getContextClassLoader();
    if (threadCL != null) {
      return threadCL.getResourceAsStream(resourceName);
    } else {
      return ClassLoader.getSystemResourceAsStream(resourceName);
    }
  }
}
