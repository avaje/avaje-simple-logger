import io.avaje.jsonlogger.SimpleServiceProvider;

module io.avaje.jsonlogger {
  requires org.slf4j;
  provides org.slf4j.spi.SLF4JServiceProvider with SimpleServiceProvider;
  exports io.avaje.jsonlogger;
  opens io.avaje.jsonlogger to org.slf4j;
}
