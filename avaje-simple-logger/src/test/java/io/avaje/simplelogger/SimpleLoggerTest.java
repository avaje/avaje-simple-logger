/**
 * Copyright (c) 2004-2022 QOS.ch Sarl (Switzerland)
 * All rights reserved.
 * <p>
 * Permission is hereby granted, free  of charge, to any person obtaining
 * a  copy  of this  software  and  associated  documentation files  (the
 * "Software"), to  deal in  the Software without  restriction, including
 * without limitation  the rights to  use, copy, modify,  merge, publish,
 * distribute,  sublicense, and/or sell  copies of  the Software,  and to
 * permit persons to whom the Software  is furnished to do so, subject to
 * the following conditions:
 * <p>
 * The  above  copyright  notice  and  this permission  notice  shall  be
 * included in all copies or substantial portions of the Software.
 * <p>
 * THE  SOFTWARE IS  PROVIDED  "AS  IS", WITHOUT  WARRANTY  OF ANY  KIND,
 * EXPRESS OR  IMPLIED, INCLUDING  BUT NOT LIMITED  TO THE  WARRANTIES OF
 * MERCHANTABILITY,    FITNESS    FOR    A   PARTICULAR    PURPOSE    AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE,  ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.avaje.simplelogger;

public class SimpleLoggerTest {

//    String A_KEY = SimpleLogger.LOG_KEY_PREFIX + "a";
//    PrintStream original = System.out;
//    ByteArrayOutputStream bout = new ByteArrayOutputStream();
//    PrintStream replacement = new PrintStream(bout);
//
//    @BeforeEach
//    public void before() {
//        System.setProperty(A_KEY, "info");
//    }
//
//    @AfterEach
//    public void after() {
//        System.clearProperty(A_KEY);
//        System.clearProperty(SimpleLogger.CACHE_OUTPUT_STREAM_STRING_KEY);
//        System.clearProperty(SimpleLogger.SHOW_THREAD_ID_KEY);
//        System.clearProperty(SimpleLogger.SHOW_THREAD_NAME_KEY);
//        System.setErr(original);
//    }
//
//    @Test
//    public void emptyLoggerName() {
//        SimpleLogger simpleLogger = new SimpleLogger("a");
//        assertEquals("info", simpleLogger.recursivelyComputeLevelString());
//    }
//
//    @Test
//    public void offLevel() {
//        System.setProperty(A_KEY, "off");
//        SimpleLogger.init();
//        SimpleLogger simpleLogger = new SimpleLogger("a");
//        assertEquals("off", simpleLogger.recursivelyComputeLevelString());
//        assertFalse(simpleLogger.isErrorEnabled());
//    }
//
//    @Test
//    public void loggerNameWithNoDots_WithLevel() {
//        SimpleLogger.init();
//        SimpleLogger simpleLogger = new SimpleLogger("a");
//
//        assertEquals("info", simpleLogger.recursivelyComputeLevelString());
//    }
//
//    @Test
//    public void loggerNameWithOneDotShouldInheritFromParent() {
//        SimpleLogger simpleLogger = new SimpleLogger("a.b");
//        assertEquals("info", simpleLogger.recursivelyComputeLevelString());
//    }
//
//    @Test
//    public void loggerNameWithNoDots_WithNoSetLevel() {
//        SimpleLogger simpleLogger = new SimpleLogger("x");
//        assertNull(simpleLogger.recursivelyComputeLevelString());
//    }
//
//    @Test
//    public void loggerNameWithOneDot_NoSetLevel() {
//        SimpleLogger simpleLogger = new SimpleLogger("x.y");
//        assertNull(simpleLogger.recursivelyComputeLevelString());
//    }

}
