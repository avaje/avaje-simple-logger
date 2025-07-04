package io.avaje.jsonlogger.encoder;

import org.slf4j.ILoggerFactory;
import org.slf4j.helpers.Reporter;
import org.slf4j.spi.LocationAwareLogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.TimeZone;

import static org.slf4j.spi.LocationAwareLogger.ERROR_INT;

public final class Bootstrap {

    private static final int LOG_LEVEL_OFF = ERROR_INT + 10;

    public static ILoggerFactory init() {
        Properties properties = loadProperties();
        String writerType = properties.getProperty("logger.writer", "json");
        LogWriter logWriter = createWriter(properties, writerType);

        String info = properties.getProperty("logger.defaultLogLevel", "info");
        int defaultLevel = stringToLevel(info);


        var factory = new SimpleLoggerFactory(logWriter, defaultLevel);

        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith("log.")) {
                String val = properties.getProperty(key);
                if (val != null) {
                    int level = stringToLevel(val);
                    String name = key.substring(4);
                    factory.putLevel(name, level);
                }
            }
        }
        return factory;
    }

    private static LogWriter createWriter(Properties properties, String writerType) {
        PrintStream target = System.out;
        String timestampPattern = Eval.eval(properties.getProperty("logger.timestampPattern"));
        if ("plain".equalsIgnoreCase(writerType)) {
            final TimeZone tz = TimeZone.getDefault();
            final DateTimeFormatter formatter = TimeZoneUtils.formatter(timestampPattern, tz.toZoneId());

            boolean showThreadName = Boolean.parseBoolean(Eval.eval(properties.getProperty("logger.showThreadName")));

            Abbreviator abbreviator = Abbreviator.create(42);
            return new PlainLogWriter(target, abbreviator, showThreadName);
        }
        var jsonEncoder = new JsonEncoderBuilder()
                .component(Eval.eval(properties.getProperty("logger.component")))
                .environment(Eval.eval(properties.getProperty("logger.environment")))
                .customFields(Eval.eval(properties.getProperty("logger.customFields")))
                .timestampPattern(timestampPattern)
                .build();
        return new JsonWriter(jsonEncoder, target);
    }

    static int stringToLevel(String levelStr) {
        if ("trace".equalsIgnoreCase(levelStr)) {
            return LocationAwareLogger.TRACE_INT;
        } else if ("debug".equalsIgnoreCase(levelStr)) {
            return LocationAwareLogger.DEBUG_INT;
        } else if ("info".equalsIgnoreCase(levelStr)) {
            return LocationAwareLogger.INFO_INT;
        } else if ("warn".equalsIgnoreCase(levelStr)) {
            return LocationAwareLogger.WARN_INT;
        } else if ("error".equalsIgnoreCase(levelStr)) {
            return LocationAwareLogger.ERROR_INT;
        } else if ("off".equalsIgnoreCase(levelStr)) {
            return LOG_LEVEL_OFF;
        }
        // assume INFO by default
        return LocationAwareLogger.INFO_INT;
    }

    static Properties loadProperties() {
        final var properties = new Properties();
        load(properties, "avajelogger.properties");
        load(properties, "avajelogger-test.properties");
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
