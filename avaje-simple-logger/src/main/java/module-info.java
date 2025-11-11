import io.avaje.simplelogger.dynamic.DynamicLogLevels;

module io.avaje.simplelogger.dynamic {

  exports io.avaje.simplelogger.dynamic;

  requires transitive org.slf4j;
  requires transitive io.avaje.simplelogger;
  requires transitive io.avaje.config;
  requires io.avaje.applog;
  requires static org.graalvm.nativeimage;

  provides io.avaje.config.ConfigExtension with DynamicLogLevels;
}
