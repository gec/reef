/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.app.process

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import net.agileautomata.executor4s._
import net.agileautomata.executor4s.testing._
import net.agileautomata.commons.testing._

// cant use InstantExcecutor because of delay based retrying
//@RunWith(classOf[JUnitRunner])
//class ProcessManagerInstantExceutorTest extends ProcessManagerTestBase {
//  class ExecutorFixture extends Fixture {
//    val executor = new InstantExecutor()
//  }
//
//  def withFixture(fun: Fixture => Unit) = fun(new ExecutorFixture)
//}

@RunWith(classOf[JUnitRunner])
class ProcessManagerMockExecutorTest extends ProcessManagerTestBase {
  class ExecutorFixture extends Fixture {
    val executor = new MockExecutor()

    override def tickExecutor() {
      executor.runUntilIdle()
      executor.tick(10000.milliseconds)
    }
  }

  def withFixture(fun: Fixture => Unit) = fun(new ExecutorFixture)
}

@RunWith(classOf[JUnitRunner])
class ProcessManagerSingleThreadTest extends ProcessManagerTestBase {
  class ExecutorFixture extends Fixture {
    val executor = Executors.newScheduledSingleThread(1.minutes)

    def terminate() {
      executor.terminate
    }
  }

  def withFixture(fun: Fixture => Unit) = {
    val fix = new ExecutorFixture
    try {
      fun(fix)
    } finally {
      fix.terminate()
    }
  }
}

@RunWith(classOf[JUnitRunner])
class ProcessManagerResizingTest extends ProcessManagerTestBase {
  class ExecutorFixture extends Fixture {
    val executor = Executors.newResizingThreadPool(1.minutes)

    def terminate() {
      executor.terminate
    }
  }

  def withFixture(fun: Fixture => Unit) = {
    val fix = new ExecutorFixture
    try {
      fun(fix)
    } finally {
      fix.terminate()
    }
  }
}

abstract class ProcessManagerTestBase extends FunSuite with ShouldMatchers {

  def makeTask(taskName: String, events: SynchronizedList[(String, String)]) = {
    new RetryableProcess(taskName) {
      override def setupRetryDelay = 100
      def setup(p: ProcessManager) = {
        events.append(("setup", name))
      }

      def cleanup(p: ProcessManager) = {
        events.append(("cleanup", name))
      }
    }
  }

  trait Fixture {

    def executor: Executor
    lazy val manager = new SimpleProcessManager(executor)

    val events = new SynchronizedList[(String, String)]

    def tickExecutor() {}

    def expectAllEvents(expected: List[(String, String)]) {

      def comp(t1: (String, String), t2: (String, String)) = if (t1._1 == t2._1) t1._2 < t2._2 else false

      events.value.awaitUntil(1500) { _.sortWith(comp) == expected } match {
        case (_, true) =>
        case (value, false) => value.sortWith(comp) should equal(expected)
      }
    }

    def expectOrderedEvents(expected: List[(String, String)]) {
      events shouldBecome (expected) within 1500
    }
  }

  def withFixture(fun: Fixture => Unit)

  test("Start and stop one task") {
    withFixture { fix =>
      import fix._

      val task = makeTask("example", events)

      manager.addProcess(task)

      manager.start()

      tickExecutor()

      expectOrderedEvents(List(("setup", "example")))

      manager.stop()

      expectOrderedEvents(List(("setup", "example"), ("cleanup", "example")))
    }
  }

  test("Start and stop many tasks") {
    withFixture { fix =>
      import fix._

      (0 to 5).foreach { i =>
        manager.addProcess(makeTask("task" + i, events))
      }

      manager.start()

      tickExecutor()

      var expectedEvents = (0 to 5).map { i => ("setup", "task" + i) }.toList

      expectAllEvents(expectedEvents)

      manager.stop()

      expectedEvents = expectedEvents ::: (0 to 5).map { i => ("cleanup", "task" + i) }.toList

      expectAllEvents(expectedEvents)
    }
  }

  test("Start and stop task with children") {
    withFixture { fix =>
      import fix._

      def addChildTasks(parent: Process, depth: Int, max: Int) {
        if (depth <= max) {
          val task = makeTask("task" + depth, events)
          manager.addChildProcess(parent, task)
          addChildTasks(task, depth + 1, max)
        }
      }

      val parent = makeTask("task0", events)
      manager.addProcess(parent)
      addChildTasks(parent, 1, 5)

      manager.start()

      tickExecutor()

      var expectedEvents = (0 to 5).map { i => ("setup", "task" + i) }.toList

      expectAllEvents(expectedEvents)

      manager.stop()

      expectedEvents = expectedEvents ::: (0 to 5).map { i => ("cleanup", "task" + i) }.toList

      expectAllEvents(expectedEvents)
    }
  }

  test("Task added to started manager gets started") {
    withFixture { fix =>
      import fix._

      manager.start()

      val task = makeTask("example", events)

      manager.addProcess(task)

      tickExecutor()

      expectOrderedEvents(List(("setup", "example")))

      manager.stop()

      expectOrderedEvents(List(("setup", "example"), ("cleanup", "example")))
    }
  }

  test("Tasks can add child tasks during setup") {
    withFixture { fix =>
      import fix._

      manager.start()

      def makeChildTask(depth: Int, max: Int): Process = {
        new OneShotProcess("task" + depth) {

          def setup(p: ProcessManager) = {
            events.append(("setup", name))
            if (depth < max) {
              p.addChildProcess(this, makeChildTask(depth + 1, max))
            }
          }

          def cleanup(p: ProcessManager) = {
            events.append(("cleanup", name))

          }
        }
      }

      manager.addProcess(makeChildTask(0, 5))

      tickExecutor()

      var expectedEvents = (0 to 5).map { i => ("setup", "task" + i) }.toList

      expectOrderedEvents(expectedEvents)

      manager.stop()

      expectedEvents = expectedEvents ::: (0 to 5).reverse.map { i => ("cleanup", "task" + i) }.toList

      expectOrderedEvents(expectedEvents)
    }
  }

  test("Tasks can remove child tasks during teardown") {
    withFixture { fix =>
      import fix._

      manager.start()

      def makeChildTask(depth: Int, max: Int): Process = {
        new OneShotProcess("task" + depth) {

          var child = Option.empty[Process]

          def setup(p: ProcessManager) = {
            events.append(("setup", name))
            if (depth < max) {
              child = Some(makeChildTask(depth + 1, max))
              p.addChildProcess(this, child.get)
            }
          }

          def cleanup(p: ProcessManager) = {
            events.append(("cleanup", name))
            child.foreach { c => p.removeProcess(c) }
          }
        }
      }

      manager.addProcess(makeChildTask(0, 5))

      tickExecutor()

      var expectedEvents = (0 to 5).map { i => ("setup", "task" + i) }.toList

      expectOrderedEvents(expectedEvents)

      manager.stop()

      expectedEvents = expectedEvents ::: (0 to 5).reverse.map { i => ("cleanup", "task" + i) }.toList

      expectOrderedEvents(expectedEvents)
    }
  }

  test("Failing child task stops parent") {
    withFixture { fix =>
      import fix._

      val parent = makeTask("parent", events)
      val child = makeTask("child", events)

      manager.addProcess(parent)
      manager.addChildProcess(parent, child)

      val expectedEvents = List(
        ("setup", "parent"),
        ("setup", "child"),
        ("cleanup", "child"),
        ("cleanup", "parent"))

      manager.start()

      tickExecutor()

      expectOrderedEvents(expectedEvents.slice(0, 2))

      manager.failProcess(child)

      expectOrderedEvents(expectedEvents)

      manager.stop()

    }
  }

  test("Failing parent task stops child") {
    withFixture { fix =>
      import fix._

      val parent = makeTask("parent", events)
      val child = makeTask("child", events)

      manager.addProcess(parent)
      manager.addChildProcess(parent, child)

      val expectedEvents = List(
        ("setup", "parent"),
        ("setup", "child"),
        ("cleanup", "child"),
        ("cleanup", "parent"))

      manager.start()

      tickExecutor()

      expectOrderedEvents(expectedEvents.slice(0, 2))

      manager.failProcess(parent)

      expectOrderedEvents(expectedEvents)

      manager.stop()

      expectOrderedEvents(expectedEvents)
    }
  }

  test("Tasks that throw exceptions in setup are retried") {
    withFixture { fix =>
      import fix._

      def makeExceptingTask(exceptions: Int): Process = {
        new RetryableProcess("task") {
          var exceptionsLeft = exceptions

          override def setupRetryDelay = 1

          override def setupExceptionIsFailure = false

          def setup(p: ProcessManager) = {

            if (exceptionsLeft == 0) {
              events.append(("setup", name))
            } else {
              exceptionsLeft -= 1
              events.append(("exception", name))
              throw new RuntimeException("Intentional failure")
            }
          }

          def cleanup(p: ProcessManager) = {
            events.append(("cleanup", name))
          }
        }
      }

      val parent = makeExceptingTask(3)
      manager.addProcess(parent)
      manager.addChildProcess(parent, makeTask("child", events))

      manager.start()

      tickExecutor()
      tickExecutor()
      tickExecutor()
      tickExecutor()

      var expectedEvents = List(
        ("exception", "task"),
        ("exception", "task"),
        ("exception", "task"),
        ("setup", "task"),
        ("setup", "child"))

      expectOrderedEvents(expectedEvents)

      manager.stop()

      expectedEvents = expectedEvents ::: List(("cleanup", "child"), ("cleanup", "task"))

      expectOrderedEvents(expectedEvents)
    }
  }

  test("Task with child that fails during setup fails parent") {
    withFixture { fix =>
      import fix._

      def makeExceptingTask(exceptions: Int): Process = {
        new OneShotProcess("child") {

          var exceptionsLeft = exceptions

          def setup(p: ProcessManager) = {

            if (exceptionsLeft == 0) {
              events.append(("setup", name))
            } else {
              exceptionsLeft -= 1
              events.append(("exception", name))
              throw new RuntimeException("Intentional failure")
            }
          }

          def cleanup(p: ProcessManager) = {
            events.append(("cleanup", name))
          }
        }
      }

      val parent = makeTask("parent", events)
      manager.addProcess(parent)
      manager.addChildProcess(parent, makeExceptingTask(1))

      manager.start()

      tickExecutor()
      tickExecutor()
      tickExecutor()
      tickExecutor()

      var expectedEvents = List(
        ("setup", "parent"),
        ("exception", "child"),
        ("cleanup", "parent"),
        ("setup", "parent"),
        ("setup", "child"))

      expectOrderedEvents(expectedEvents)

      manager.stop()

      expectedEvents = expectedEvents ::: List(("cleanup", "child"), ("cleanup", "parent"))

      expectOrderedEvents(expectedEvents)
    }
  }
}