package io.avaje.simplelogger.dynamic;


import io.avaje.simplelogger.LoggerContext;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

class DynamicLogLevelsTest {

    static final Logger logFoo = LoggerFactory.getLogger("org.foo.MyFoo");
    static final Logger logBar = LoggerFactory.getLogger("org.bar.extra.MyBar");

    @Test
    void test() {
        logFoo.debug("hi foo");
        logBar.debug("hi bar before");

        LoggerContext.get()
                .putAll(Map.of("org.bar.extra", "trace"));

        logBar.debug("hi bar after log level change");
    }

}