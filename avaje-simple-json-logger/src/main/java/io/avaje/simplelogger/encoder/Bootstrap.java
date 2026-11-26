package io.avaje.simplelogger.encoder;

import io.avaje.simplelogger.LogWriter;
import io.avaje.simplelogger.LogWriterProvider;
import io.avaje.simplelogger.LoggerContext;
import org.slf4j.helpers.Reporter;

import java.io.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Bootstrap the simple logger.
 */
public final class Bootstrap {

  private static final int LOG_LEVEL_PREFIX_LENGTH = "log.level.".length();

  public static LoggerContext init() {
    Properties properties = loadProperties();
    LogWriter logWriter = createWriter(properties, logFormat(properties));

    String defaultLogLevel = properties.getProperty("logger.defaultLogLevel", "info");
    int defaultLevel = SimpleLoggerFactory.stringToLevel(defaultLogLevel);

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
          nameLevels.put(key.substring(LOG_LEVEL_PREFIX_LENGTH), val);
        }
      }
    }
    return nameLevels;
  }

  private static LogWriter createWriter(Properties properties, String writerType) {
    String writerName = property(properties, "logger.writer");
    if (writerName != null && !writerName.isBlank() && !"console".equalsIgnoreCase(writerName)) {
      try {
        return createExternalWriter(properties, writerName);
      } catch (RuntimeException | ServiceConfigurationError e) {
        // fall back to console rather than failing SLF4J initialisation
        Reporter.error("Failed to create log writer '" + writerName + "', falling back to console", e);
      }
    }

    return createConsoleWriter(properties, writerType);
  }

  private static LogWriter createConsoleWriter(Properties properties, String writerType) {
    final PrintStream target = System.out;
    final TimeZone timeZone = TimeZoneUtils.parseTimeZone(property(properties, "logger.timezone"));
    final String timestampPattern = property(properties, "logger.timestampPattern");
    if ("plain".equalsIgnoreCase(writerType)) {
      final DateTimeFormatter formatter = TimeZoneUtils.plainFormatter(timestampPattern, timeZone.toZoneId());
      final boolean showThreadName = propertyShowThreadName(properties);
      return new PlainLogWriter(target, formatter, showThreadName, TraceContextFactory.create());
    }
    var jsonEncoder = new JsonEncoderBuilder()
      .component(property(properties, "logger.component"))
      .environment(property(properties,"logger.environment"))
      .naming(property(properties,"logger.naming"))
      .propertyNames(property(properties,"logger.propertyNames"))
      .customFields(property(properties, "logger.customFields"))
      .timestampPattern(timestampPattern)
      .timeZone(timeZone)
      .build();
    return new JsonLogWriter(jsonEncoder, target);
  }

  private static LogWriter createExternalWriter(Properties properties, String writerName) {
    LogWriterProvider selected = null;
    for (LogWriterProvider provider : ServiceLoader.load(LogWriterProvider.class)) {
      if (writerName.equalsIgnoreCase(provider.name())) {
        if (selected != null) {
          throw new IllegalStateException("Multiple log writer providers named '" + writerName + "'");
        }
        selected = provider;
      }
    }
    if (selected == null) {
      throw new IllegalStateException("Log writer '" + writerName + "' is selected but no provider is available");
    }
    return Objects.requireNonNull(selected.create(evaluated(properties)), "Log writer provider returned null: " + writerName);
  }

  /**
   * Return a copy of the properties with {@code ${...}} expressions resolved, so external
   * writer providers see the same placeholder support as the built-in console writer.
   */
  private static Properties evaluated(Properties properties) {
    Properties resolved = new Properties();
    for (String key : properties.stringPropertyNames()) {
      resolved.setProperty(key, Eval.eval(properties.getProperty(key)));
    }
    return resolved;
  }

  private static String property(Properties properties, String key) {
    return Eval.eval(properties.getProperty(key));
  }

  private static boolean propertyShowThreadName(Properties properties) {
    return Boolean.parseBoolean(Eval.eval(properties.getProperty("logger.showThreadName", "true")));
  }

  static Properties loadProperties() {
    String config = System.getProperty("logger.config", System.getenv("LOGGER_CONFIG"));
    if (config != null && !config.isEmpty()) {
      final var properties = loadExternal(config);
      if (isMergeDefaultProperties(properties)) {
        // merge with default properties from avaje-logger.properties
        for (Map.Entry<Object, Object> entry : defaultProperties().entrySet()) {
          properties.putIfAbsent(entry.getKey(), entry.getValue());
        }
      }
      return properties;
    }
    return loadTestProperties(defaultProperties());
  }

  private static boolean isMergeDefaultProperties(Properties properties) {
    return Optional.ofNullable(System.getProperty("logger.config.merge"))
      .or(() -> Optional.ofNullable(System.getenv("LOGGER_CONFIG_MERGE")))
      .or(() -> Optional.ofNullable(properties.getProperty("logger.config.merge")))
      .orElse("true")
      .equalsIgnoreCase("true");
  }

  static Properties defaultProperties() {
    final var properties = new Properties();
    loadResource(properties, "avaje-logger-common.properties");
    loadResource(properties, "avaje-logger.properties");
    return properties;
  }

  static Properties loadTestProperties(Properties properties) {
    loadResource(properties, "avaje-logger-test.properties");
    return properties;
  }

  static Properties loadExternal(String fileName) {
    final var properties = new Properties();
    if (fileName.startsWith("~/")) {
      fileName = System.getProperty("user.home") + fileName.substring(1);
    }
    if (loadFile(fileName, properties)) {
      return properties;
    }
    if (!fileName.endsWith(".properties") && loadFile(fileName + ".properties", properties)) {
      return properties;
    }
    Reporter.error("External logger config file not found: " + fileName);
    return properties;
  }

  private static boolean loadFile(String fileName, Properties properties) {
    var file = new File(fileName);
    if (!file.exists()) {
      return false;
    } else {
      loadFileIntoProperties(file, properties);
      return true;
    }
  }

  private static void loadFileIntoProperties(File file, Properties properties) {
    try (var is = new FileInputStream(file)) {
      properties.load(is);
    } catch (IOException e) {
      Reporter.error("Error loading external logger config file: " + file, e);
    }
  }

  private static void loadResource(Properties properties, String resourceName) {
    InputStream is = resource(resourceName);
    if (is != null) {
      try (is) {
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
