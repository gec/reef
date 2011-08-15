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

import akka.actor.Actor._
import org.totalgrid.reef.executor.ActorTimerMessages._
import org.totalgrid.reef.util.{ Timer, Logging }
import akka.actor.{ ActorRef, ReceiveTimeout, Actor }

class AkkaExecutorActor extends Actor with Logging {

  import ActorExecutorMessages._

  private def safeExecute(fun: () => Unit): Unit = {
    try {
      fun()
    } catch {
      case ex: Exception => logger.error("Exception thrown from execute", ex)
    }
  }

  def receive() = {

    case Execute(fun) => safeExecute(fun)

    case Request(calculate, reply) => {
      try {
        reply(Right(calculate()))
      } catch {
        case ex: Exception => reply(Left(ex))
      }
    }

    case x: Any => logger.error("Unhandled message: " + x)

  }

}

trait AkkaTimer extends Timer {

  val actor: ActorRef

  def cancel() = actor ! CANCEL
  def now() = actor ! NOW
}

class SingleAkkaTimer(delay: Long, executor: Executor)(onStop: Timer => Unit)(fun: => Unit) extends AkkaTimer {
  val actor = actorOf(new AkkaSingleTimerActor(delay, executor)(onStop(this))(fun)).start()
}

class RepeatAkkaTimer(delay: Long, executor: Executor)(onStop: Timer => Unit)(fun: => Unit) extends AkkaTimer {
  executor.execute(fun) //call the function initially
  val actor = actorOf(new AkkaRepeatTimerActor(delay, executor)(onStop(this))(fun)).start()
}

abstract class AkkaTimerActor(delay: Long)(beforeExit: => Unit) extends Actor {

  import org.totalgrid.reef.executor.ActorTimerMessages._

  self.receiveTimeout = Some(delay)

  def specificMessageHandler: Receive

  def defaultMessageHandler: Receive = {
    case CANCEL =>
      beforeExit
      self.exit()
  }

  def receive() = specificMessageHandler orElse defaultMessageHandler
}

class AkkaSingleTimerActor(delay: Long, executor: Executor)(beforeExit: => Unit)(fun: => Unit)
    extends AkkaTimerActor(delay)(beforeExit) with Logging {

  override def specificMessageHandler() = {
    case NOW =>
      executor.execute(fun)
      beforeExit
      self.exit()
    case ReceiveTimeout =>
      executor.execute(fun)
      beforeExit
      self.exit()
  }

}
class AkkaRepeatTimerActor(delay: Long, executor: Executor)(beforeExit: => Unit)(fun: => Unit)
    extends AkkaTimerActor(delay)(beforeExit) with Logging {

  def specificMessageHandler() = {
    case NOW => doFun
    case ReceiveTimeout => doFun
  }

  def doFun {
    executor.execute(fun)
    executor.execute(self ! OPERATION_COMPLETE)
    self.receiveTimeout = None
    become {
      case OPERATION_COMPLETE =>
        self.receiveTimeout = Some(delay)
        unbecome()
      case CANCEL =>
        beforeExit
        self.exit()
      case NOW =>
        doFun
    }
  }

}