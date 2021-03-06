/*
 * Copyright 2018 SPF4J.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.spf4j.test.log;

import java.util.concurrent.TimeUnit;
import org.hamcrest.Matcher;
import org.spf4j.base.TimeSource;

/**
 * @author Zoltan Farkas
 */
abstract class UncaughtExceptionAsserter implements AsyncObservationAssert, UncaughtExceptionConsumer {

  private final Matcher<UncaughtExceptionDetail> matcher;

  private final Object sync;

  private int seen;

  UncaughtExceptionAsserter(final Matcher<UncaughtExceptionDetail> matcher) {
    this.matcher = matcher;
    this.seen = 0;
    this.sync = new Object();
  }

  @Override
  public void assertObservation(final long timeout, final TimeUnit unit) throws InterruptedException {
    long deadline = TimeSource.nanoTime() + unit.toNanos(timeout);
    synchronized (sync) {
      while (seen == 0) {
        long nanosToDeadline = deadline - TimeSource.nanoTime();
        if (nanosToDeadline <= 0) {
            throw new AssertionError("Not seen uncaught exception matching " + matcher);
        }
        TimeUnit.NANOSECONDS.timedWait(sync, nanosToDeadline);
      }
    }
  }


  @Override
  public boolean offer(final UncaughtExceptionDetail exDetail) {
    synchronized (sync) {
      if (matcher.matches(exDetail)) {
        seen++;
        sync.notifyAll();
        return true;
      }
      return false;
    }
  }

  @Override
  public String toString() {
    return "UncaughtExceptionAsserter{" + "matcher=" + matcher + '}';
  }

}
