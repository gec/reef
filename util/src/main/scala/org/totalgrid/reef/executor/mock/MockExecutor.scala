package org.totalgrid.reef.executor.mock

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

import org.totalgrid.reef.executor._
import org.totalgrid.reef.util.Timer

/**
 * Mock executor that fully simulates all executor behaviors, but allows
 * single threaded tests to be be written in lock-step with the tester
 * invoking each action with expectations.
 */

class MockExecutor extends Executor {

  final override def execute(fun: => Unit): Unit = queue.enqueue(Execution(() => fun))
  final override def delay(msec: Long)(fun: => Unit): Timer = addTimerAction(Delay(() => fun, msec))
  final override def repeat(msec: Long)(fun: => Unit): Timer = addTimerAction(Repeat(() => fun, msec))
  final override def request[A](fun: => A): A = throw new Exception("Not implemented")

  class Action(fun: () => Unit) { def execute = fun() }
  case class Execution(fun: () => Unit) extends Action(fun)
  class TimerAction(fun: () => Unit) extends Action(fun)
  case class Delay(fun: () => Unit, ms: Long) extends TimerAction(fun)
  case class Repeat(fun: () => Unit, ms: Long) extends TimerAction(fun)

  private val queue = new scala.collection.mutable.Queue[Action]

  class ActionTimer(action: TimerAction) extends Timer {

    def cancel() = {
      queue.dequeueFirst(_.equals(action))
    }

    def now() = queue.dequeueFirst(_.equals(action)) match {
      case Some(action) => {
        action.execute
        action match {
          case Delay(fun, _) =>
          case Repeat(fun, _) => queue.enqueue(action)
        }
      }
      case None =>
    }

  }

  private def addTimerAction(action: TimerAction): Timer = {
    queue.enqueue(action)
    new ActionTimer(action)
  }

  def exceptionText(expected: String, actual: String = "empty") =
    "Expected " + expected + " at front of queue, but it was " + actual

  def executeNext() {
    if (queue.isEmpty) throw new Exception(exceptionText("Execute"))
    else queue.front match {
      case x: Execution =>
        queue.dequeue()
        x.execute
      case y: Action => throw new Exception(exceptionText("Execute", y.toString))
    }
  }

  def delayNext(): Long = {
    if (queue.isEmpty) throw new Exception(exceptionText("Delay"))
    else queue.front match {
      case Delay(fun, ms) =>
        queue.dequeue()
        fun()
        ms
      case y: Action =>
        throw new Exception(exceptionText("Delay", y.toString))
    }
  }

  def repeatNext(): Long = {
    if (queue.isEmpty) throw new Exception(exceptionText("Repeat"))
    else queue.front match {
      case Repeat(fun, ms) =>
        queue.enqueue(queue.dequeue())
        fun()
        ms
      case y: Action =>
        throw new Exception(exceptionText("Repeat", y.toString))
    }
  }

}
