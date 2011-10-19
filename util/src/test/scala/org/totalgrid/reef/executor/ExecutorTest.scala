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
package org.totalgrid.reef.executor

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import org.totalgrid.reef.util.SyncVar
import net.agileautomata.commons.testing._
import org.scalatest.FunSuite
import parallel.Future

@RunWith(classOf[JUnitRunner])
class ReactActorExecutorTest extends ExecutorTestBase {
  override val maxActorPowerOf2 = 6 // 64
  def _getActor = new TestActorBase with ReactActorExecutor

  override def toString = "ReactActorExecutorTest"
}

@RunWith(classOf[JUnitRunner])
class ReceiveActorExecutorTest extends ExecutorTestBase {
  override val maxActorPowerOf2 = 6 // 64
  def _getActor = new TestActorBase with ReceiveActorExecutor
}

abstract class ExecutorTestBase extends FunSuite with ShouldMatchers {

  val maxActorPowerOf2: Int

  def fixture(test: TestActorBase with ActorExecutor => Unit): Unit = {
    val actor = getActor
    try {
      test(actor)
    } finally {
      actor.stop()
    }
  }

  def fixture(repeats: Int)(test: TestActorBase with ActorExecutor => Unit): Unit = repeats.times(fixture(test))

  abstract class TestActorBase extends Executor with Lifecycle {
    var running = new SyncVar[Option[Boolean]](None: Option[Boolean])
    var called = new SyncVar(0)

    private var children: List[TestActorBase] = Nil

    def increment() = called.atomic(i => i + 1)

    def addChild(): TestActorBase = {
      children = _getActor :: children
      children.head
    }

    override def afterStart() = {
      children.foreach(_.start())
      running.update(Some(true))
    }
    override def beforeStop() = {
      children.foreach(_.stop())
      running.update(Some(false))
    }

    def waitForStarted = running.waitFor(_ == Some(true), 25) should equal(true)
    def waitForStopped = running.waitFor(_ == Some(false), 25) should equal(true)

    def waitForIncrement(value: Int, until: Long = 5000, after: Long = 50) {
      called.waitUntil(value, until, false) should equal(true)
      called.waitWhile(value, after, false) should equal(false)
    }
    def waitForAtleastIncrement(lowLimit: Int, until: Long = 5000) {
      called.waitFor(_ >= lowLimit, until, false) should equal(true)
    }
    def checkUnchanged(value: Int, after: Long = 50) {
      called.waitWhile(value, after, false) should equal(false)
    }
  }

  // let concrete classes define the right reactable implementation
  def _getActor: TestActorBase with ActorExecutor

  // auto-starts the actors
  def getActor: TestActorBase with ActorExecutor = {
    val a = _getActor
    a.start
    a
  }

  test("stop and start calls are idempotent") {
    fixture(5) { a =>
      a.start()
      a.start()

      a.stop()
      a.stop()
    }
  }

  test("unrevoked delay") {
    fixture { a =>
      a.delay(1)(a.increment())
      a.waitForIncrement(1)
    }
  }

  test("revoked delay") {
    fixture { a =>
      val revoker = a.delay(500)(a.increment())
      revoker.cancel()

      // make sure it didn't execute
      a.waitForIncrement(0, 0, 1000)
    }
  }

  test("delay done now") {
    fixture { a =>
      val revoker = a.delay(5000)(a.increment())
      revoker.now()
      a.waitForIncrement(1)
    }
  }

  test("repeat called initially") {
    fixture { a =>
      a.repeat(5000)(a.increment())
      a.waitForIncrement(1)
    }
  }

  test("repeat") {
    fixture { a =>
      a.repeat(1)(a.increment())
      a.waitForAtleastIncrement(5)
    }
  }

  test("canceled repeat") {
    fixture { a =>
      val revoker = a.repeat(1) { a.increment() }
      a.waitForAtleastIncrement(5)
      revoker.cancel()
      val stoppedVal = a.called.lastValueAfter(500)
      a.checkUnchanged(stoppedVal)
    }
  }

  test("outstanding repeat on stop") {
    fixture(5) { a =>
      a.repeat(10000)(a.increment())
      a.waitForIncrement(1)
      a.stop()
      a.waitForIncrement(1)
    }
  }

  test("canceled outstanding repeat on stop") {
    fixture(5) { a =>
      val revoker = a.repeat(10000)(a.increment())
      revoker.cancel()
      a.waitForIncrement(1)
      a.stop()
      a.waitForIncrement(1)
    }
  }

  test("Request marshalls exceptions") {
    fixture { a =>
      5.times {
        val future: Future[Int] = a.request(1 / 0)
        intercept[ArithmeticException](future())
      }
    }
  }

  test("Futures can be parallelized") {
    fixture { a =>
      fixture { b =>
        val f1 = a.request(4)
        val f2 = b.request(5)
        (f1() + f2()) should equal(9)
      }
    }
  }

  test("timer doesnt kill parent") {
    // race condition on the unlink, need to repeat to make error occur
    fixture(5) { a =>
      a.delay(0)(1)
      a.request(1)()
      a.waitForStarted
      a.stop()
    }
  }

  /*
  test("bind kills linked actors") {
    5.times {
      fixture { a =>
        fixture { b =>
          a.bind(b.getActor)

          a.waitForStarted
          b.waitForStarted

          a.stop()

          a.waitForStopped
          b.waitForStopped
        }
      }
    }
  }
  */

  test("restart executor") {
    fixture { a =>
      a.request(1)()
      a.stop()
      a.start()
      a.request(1)()
      a.stop()
    }
  }

  test("killing parent kills timers") {

    // race condition on the unlink, need to repeat to make error occur
    fixture(2) { a =>
      a.waitForStarted
      a.repeat(1)(a.increment())
      a.waitForAtleastIncrement(5)
      a.stop()
      a.waitForStopped
      val atStop = a.called.lastValueAfter(500)
      a.checkUnchanged(atStop)
    }
  }

  test("ActorTrees") {

    // creates a binary tree of reactors of specified depth
    def btree(depth: Int): List[TestActorBase] = {
      assert(depth >= 1)
      def btree(p: TestActorBase, depth: Int): List[TestActorBase] = {
        if (depth == 0) Nil
        else p :: btree(p.addChild(), depth - 1) ::: btree(p.addChild(), depth - 1)
      }
      btree(_getActor, depth - 1)
    }

    val actors = btree(this.maxActorPowerOf2)
    actors.head.start()
    actors.head.stop()
    actors.foreach(_.waitForStopped)

  }

}