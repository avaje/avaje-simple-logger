package io.avaje.simplelogger.encoder;

import io.avaje.json.mapper.JsonMapper;
import io.avaje.json.stream.JsonStream;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

final class JsonEncoderBuilder {

  private final Map<String, String> customFieldsMap = new HashMap<>();
  private JsonStream json;
  private StackHasher stackHasher;
  private ThrowableConverter throwableConverter = new ThrowableConverter();

  private TimeZone timeZone = TimeZone.getDefault();
  /**
   * Null implies default of ISO_OFFSET_DATE_TIME
   */
  private String timestampPattern;
  private String component;
  private String environment;
  private String propertyNames;
  private boolean includeStackHash = true;

  JsonEncoderBuilder json(JsonStream json) {
    this.json = json;
    return this;
  }

  JsonEncoderBuilder stackHasher(StackHasher stackHasher) {
    this.stackHasher = stackHasher;
    return this;
  }

  JsonEncoderBuilder throwableConverter(ThrowableConverter throwableConverter) {
    this.throwableConverter = throwableConverter;
    return this;
  }

  JsonEncoderBuilder timeZone(TimeZone timeZone) {
    this.timeZone = timeZone;
    return this;
  }

  JsonEncoderBuilder timestampPattern(String timestampPattern) {
    this.timestampPattern = timestampPattern;
    return this;
  }

  JsonEncoderBuilder component(String component) {
    this.component = component;
    return this;
  }

  JsonEncoderBuilder environment(String environment) {
    this.environment = environment;
    return this;
  }

  JsonEncoderBuilder propertyNames(String propertyNames) {
    this.propertyNames = propertyNames;
    return this;
  }

  JsonEncoderBuilder includeStackHash(boolean includeStackHash) {
    this.includeStackHash = includeStackHash;
    return this;
  }

  JsonEncoderBuilder customField(String key, String value) {
    customFieldsMap.put(key, value);
    return this;
  }

  JsonEncoderBuilder customFields(String customFieldsJson) {
    if (customFieldsJson == null || customFieldsJson.isBlank()) {
      return this;
    }
    var mapper = JsonMapper.builder().jsonStream(json).build();
    mapper.map().fromJson(customFieldsJson).forEach((key, value) -> {
      if (value instanceof String) {
        value = Eval.eval((String) value);
      }
      customFieldsMap.put(key, mapper.toJson(value));
    });
    return this;
  }

  JsonEncoder build() {
    if (json == null) {
      json = JsonStream.builder().build();
    }
    if (component == null) {
      component = Eval.defaultComponent();
    }
    if (environment == null) {
      environment = System.getenv("ENVIRONMENT");
    }
    if (stackHasher == null) {
      stackHasher = new StackHasher(StackElementFilter.builder().allFilters().build());
    }
    String[] mappedPropertyNames = toPropertyNames(propertyNames);
    final DateTimeFormatter formatter = TimeZoneUtils.jsonFormatter(timestampPattern, timeZone.toZoneId());
    return new JsonEncoder(mappedPropertyNames, json, component, environment, stackHasher, formatter, includeStackHash, customFieldsMap, throwableConverter);
  }

  static String[] toPropertyNames(String mapping) {
    String[] keys = {"component", "env", "timestamp", "level", "logger", "message", "thread", "stackhash", "stacktrace"};
    if (mapping == null || mapping.isEmpty()) {
      return keys;
    }
    return toPropertyNames(parseNameMapping(mapping), keys);
  }

  private static Map<String,String> parseNameMapping(String mapping) {
    Map<String,String> map = new HashMap<>();
    String[] split = mapping.split("[,;]");
    for (String keyVal : split) {
      String[] nameVal = keyVal.trim().split("=");
      if (nameVal.length == 2) {
       map.put(nameVal[0].toLowerCase().trim(), nameVal[1].trim());
      }
    }
    return map;
  }

  private static String[] toPropertyNames(Map<String,String> map, String[] baseNames) {
    String[] names = new String[baseNames.length];
    for (int i = 0; i < baseNames.length; i++) {
      String overrideName = map.get(baseNames[i]);
      names[i] = overrideName != null ? overrideName : baseNames[i];
    }
    return names;
  }

}
