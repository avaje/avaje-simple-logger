/**
 * Copyright (c) 2004-2021 QOS.ch
 * All rights reserved.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
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
package io.avaje.simplelogger.multiThreadedExecution;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;

public class MultithereadedExecutionTest {

  private static final int THREAD_COUNT = 2;
  private static final long TEST_DURATION_IN_MILLIS = 100;
  private final Thread[] threads = new Thread[THREAD_COUNT];
  private final PrintStream oldOut = System.out;
  StateCheckingPrintStream scps = new StateCheckingPrintStream(oldOut);

  volatile boolean signal = false;

  @BeforeEach
  public void setup() {
    System.setErr(scps);
    // System.setProperty(SimpleLogger.LOG_FILE_KEY, "System.err");
    // LoggerFactoryFriend.reset();
  }

  @AfterEach
  public void tearDown() throws Exception {
    // LoggerFactoryFriend.reset();
    // System.clearProperty(SimpleLogger.LOG_FILE_KEY);
    System.setErr(oldOut);
  }

  @Test
  public void test() throws Throwable {
    WithException withException = new WithException();
    Other other = new Other();
    threads[0] = new Thread(withException);
    threads[1] = new Thread(other);
    threads[0].start();
    threads[1].start();
    Thread.sleep(TEST_DURATION_IN_MILLIS);
    signal = true;
    threads[0].join();
    threads[1].join();

    if (withException.throwable != null) {
      throw withException.throwable;
    }

    if (other.throwable != null) {
      throw other.throwable;
    }

  }

  class WithException implements Runnable {

    volatile Throwable throwable;
    Logger logger = LoggerFactory.getLogger(WithException.class);

    @Override
    public void run() { // TODO Auto-generated method stub
      int i = 0;

      while (!signal) {
        try {
          logger.info("Hello {}", i, new Throwable("i=" + i));
          i++;
        } catch (Throwable t) {
          throwable = t;
          MultithereadedExecutionTest.this.signal = true;
          return;
        }
      }

    }
  }

  class Other implements Runnable {
    volatile Throwable throwable;
    Logger logger = LoggerFactory.getLogger(Other.class);

    @Override
    public void run() {
      int i = 0;
      while (!signal) {
        try {
          logger.info("Other {}", i++);
        } catch (Throwable t) {
          throwable = t;
          MultithereadedExecutionTest.this.signal = true;
          return;
        }
      }
    }
  }

}
