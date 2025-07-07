import io.avaje.simplelogger.dynamic.DynamicLogLevels;

module io.avaje.simplelogger.dynamic {

  exports io.avaje.simplelogger.dynamic;

  requires transitive io.avaje.simplelogger;
  requires transitive io.avaje.config;

  provides io.avaje.config.ConfigExtension with DynamicLogLevels;
}
