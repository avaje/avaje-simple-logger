package io.avaje.simplelogger.encoder;

import io.avaje.simplelogger.LoggerContext;
import org.slf4j.helpers.Reporter;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

public final class Bootstrap {

    public static LoggerContext init() {
        Properties properties = loadProperties();
        String writerType = properties.getProperty("logger.writer", "json");
        LogWriter logWriter = createWriter(properties, writerType);

        String info = properties.getProperty("logger.defaultLogLevel", "info");
        int defaultLevel = SimpleLoggerFactory.stringToLevel(info);

        String nameLength = properties.getProperty("logger.nameTargetLength", "full");
        Abbreviator abbreviator = Abbreviator.create(nameLength);

        var factory = new SimpleLoggerFactory(logWriter, abbreviator, defaultLevel);

        Map<String, String> nameLevels = new LinkedHashMap<>();
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith("log.level.")) {
                String val = properties.getProperty(key);
                if (val != null) {
                    nameLevels.put(key.substring(10), val);
                }
            }
        }
        factory.putAll(nameLevels);
        return factory;
    }

    private static LogWriter createWriter(Properties properties, String writerType) {
        PrintStream target = System.out;
        String timestampPattern = Eval.eval(properties.getProperty("logger.timestampPattern"));
        if ("plain".equalsIgnoreCase(writerType)) {
            final TimeZone tz = TimeZone.getDefault();
            final DateTimeFormatter formatter = TimeZoneUtils.formatter(timestampPattern, tz.toZoneId());

            boolean showThreadName = Boolean.parseBoolean(Eval.eval(properties.getProperty("logger.showThreadName")));

            return new PlainLogWriter(target, showThreadName);
        }
        var jsonEncoder = new JsonEncoderBuilder()
                .component(Eval.eval(properties.getProperty("logger.component")))
                .environment(Eval.eval(properties.getProperty("logger.environment")))
                .customFields(Eval.eval(properties.getProperty("logger.customFields")))
                .timestampPattern(timestampPattern)
                .build();
        return new JsonWriter(jsonEncoder, target);
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
