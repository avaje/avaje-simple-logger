package io.avaje.jsonlogger;

import org.junit.jupiter.api.Test;
import org.slf4j.helpers.Slf4jEnvUtil;

import static org.assertj.core.api.Assertions.assertThat;

class Slf4jVersionTest {

    @Test
    void slf4jVersionTest() {
        String version = Slf4jEnvUtil.slf4jVersion();
        assertThat(version).isNotNull();
        assertThat(version).startsWith("2");
    }

}
