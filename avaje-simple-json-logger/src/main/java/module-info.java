import io.avaje.simplelogger.SimpleLoggerProvider;

module io.avaje.simplelogger {
  exports io.avaje.simplelogger;

  requires transitive org.slf4j;
  requires transitive io.avaje.json;
  provides org.slf4j.spi.SLF4JServiceProvider with SimpleLoggerProvider;
  opens io.avaje.simplelogger to org.slf4j;
}
