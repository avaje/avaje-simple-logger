package io.avaje.simplelogger.encoder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EvalTest {

  @Test
  void k8sComponent() {
    assertEquals("my-component", Eval.k8sComponent("my-component-part0-part1"));
  }

  @Test
  void eval() {
    String expected = System.getProperty("user.home");
    assertEquals(expected, Eval.eval("${user.home:someDefault}"));
    assertEquals(expected, Eval.eval("${user.home}"));
  }

  @Test
  void eval_expect_defaultValue() {
    assertEquals("someDefault", Eval.eval("${user.homeDoesNotExist:someDefault}"));
  }

  @Test
  void toSystemPropertyKey() {
    assertEquals("my.prop", Eval.toSystemPropertyKey("MY_PROP"));
    assertEquals("my.prop", Eval.toSystemPropertyKey("my_prop"));
    assertEquals("my.prop", Eval.toSystemPropertyKey("my.prop"));

  }

  @Test
  void toEnvPropertyKey() {
    assertEquals("MY_PROP", Eval.toEnvPropertyKey("MY_PROP"));
    assertEquals("MY_PROP", Eval.toEnvPropertyKey("my_prop"));
    assertEquals("MY_PROP", Eval.toEnvPropertyKey("my.prop"));
  }
}
