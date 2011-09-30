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
package org.totalgrid.reef.executor.mock

import org.totalgrid.reef.executor._
import org.totalgrid.reef.util.Timer
import scala.collection.mutable.Queue
import parallel.Future

/**
 * Mock executor that fully simulates all executor behaviors, but allows
 * single threaded tests to be be written in lock-step with the tester
 * invoking each action with expectations.
 */

trait MockExecutorTrait extends Executor {

  final override def execute(fun: => Unit): Unit = queue.enqueue(Execution(() => fun))
  final override def delay(msec: Long)(fun: => Unit): Timer = addTimerAction(Delay(() => fun, msec))
  final override def repeat(msec: Long)(fun: => Unit): Timer = addTimerAction(Repeat(() => fun, msec))
  final override def request[A](fun: => A): Future[A] = throw new Exception("Not implemented")

  final def numActionsPending = queue.size

  //final def executeNext() : Unit = executeNext(None, None)
  final def executeNext(preSize: Int, postsize: Int): Unit = executeNext(Some(preSize), Some(postsize))

  //final def delayNext() : Long = delayNext(None, None)
  final def delayNext(preSize: Int, postsize: Int): Long = delayNext(Some(preSize), Some(postsize))

  //final def repeatNext() : Long = repeatNext(None, None)
  final def repeatNext(preSize: Int, postsize: Int): Long = repeatNext(Some(preSize), Some(postsize))

  private val queue = new Queue[Action]

  private trait Action { def perform(): Unit }

  private case class Execution(fun: () => Unit) extends Action { def perform() = fun() }

  private trait TimerAction extends Action

  private case class Delay(fun: () => Unit, ms: Long) extends TimerAction {
    def perform() = fun()
  }

  private def performNext[A](preSize: Option[Int], postSize: Option[Int], expected: String)(x: PartialFunction[Action, A]): A = {

    def checkSize(size: Int, prefix: String) = if (queue.size != size) {
      throw new Exception(prefix + ": expected queue of size " + size + " but it was " + queue.size)
    }

    preSize.foreach(checkSize(_, "Precondition"))

    val actionOption = queue.dequeueFirst(x.isDefinedAt(_))

    if (actionOption.isEmpty)
      throw new Exception("Expected the execution queue to have a " + expected + " , but it was empty")
    val action = actionOption.get

    action.perform()
    postSize.foreach(checkSize(_, "Postcondition"))
    x.apply(action)
  }

  private case class Repeat(fun: () => Unit, ms: Long) extends TimerAction {
    def perform() = {
      fun()
      queue.enqueue(this)
    }
  }

  private class ActionTimer(action: TimerAction) extends Timer {

    def cancel() = queue.dequeueFirst(_.equals(action))
    def now() = queue.dequeueFirst(_.equals(action)).foreach(_.perform())

  }

  private def addTimerAction(action: TimerAction): Timer = {
    queue.enqueue(action)
    new ActionTimer(action)
  }

  private def executeNext(preSize: Option[Int], postSize: Option[Int]): Unit = performNext(preSize, postSize, "Execute") { case x: Execution => }
  private def delayNext(preSize: Option[Int], postSize: Option[Int]): Long = performNext(preSize, postSize, "Delay") { case x: Delay => x.ms }
  private def repeatNext(preSize: Option[Int], postSize: Option[Int]): Long = performNext(preSize, postSize, "Repeat") { case x: Repeat => x.ms }

}

class MockExecutor extends MockExecutorTrait
