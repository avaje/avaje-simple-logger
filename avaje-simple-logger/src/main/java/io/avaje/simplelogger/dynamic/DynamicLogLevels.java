package io.avaje.simplelogger.dynamic;

import io.avaje.config.Configuration;
import io.avaje.config.ConfigurationPlugin;
import io.avaje.config.ModificationEvent;
import io.avaje.simplelogger.LoggerContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Using avaje config as source of log levels including dynamic log level adjustment.
 */
public final class DynamicLogLevels implements ConfigurationPlugin {

  private static String trimKey(String key) {
    return key.substring(10);
  }

  @Override
  public void apply(Configuration configuration) {
    final var config = configuration.forPath("log.level");
    Map<String, String> nameLevels = new HashMap<>();
    for (String key : config.keys()) {
      String rawLevel = config.getNullable(key);
      if (rawLevel != null) {
        nameLevels.put(trimKey(key), rawLevel);
      }
    }
    if (!nameLevels.isEmpty()) {
      LoggerContext.get().putAll(nameLevels);
    }

    configuration.onChange(this::onChangeAny);
  }

  private void onChangeAny(ModificationEvent modificationEvent) {
    final var config = modificationEvent.configuration();
    Map<String, String> nameLevels = new HashMap<>();
    modificationEvent.modifiedKeys().stream()
      .filter(key -> key.startsWith("log.level."))
      .forEach(key -> {
        String rawLevel = config.getNullable(key);
        if (rawLevel != null) {
          nameLevels.put(trimKey(key), rawLevel);
        }
      });

    if (!nameLevels.isEmpty()) {
      LoggerContext.get().putAll(nameLevels);
    }
  }

}
