/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  Green Energy Corp licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.reactor

import scala.actors.AbstractActor
import scala.actors.Actor._
import scala.actors.TIMEOUT
import org.totalgrid.reef.util.Timer

/// Companion class with case classes that correspond to Reactable's interface
object Reactor {

  case class Execute(fun: () => Any)

  case class Request[A](fun: () => A)

  case class Link(a: AbstractActor)

  case class UnLink(a: AbstractActor)

}

/**
 *  Generic actor that can be used to execute 
 *  arbitrary blocks of code. Useful for synchronizing
 *  lots of other threads.
 *
 */
trait Reactor extends Reactable with Lifecycle {

  case object CANCEL
  case object NOW
  case object OPERATION_COMPLETE

  import Reactor._

  /// start execution and run fun just afterwards
  override def dispatchStart() = {
    this.doStart()
    this.execute(afterStart)
  }

  /// stop execution and run fun
  override def dispatchStop() = {
    myactor.stop()
    myactor = getReactableActor
  }

  /// Extending classes will implement the AbstractActor that receives these
  /// messages
  var myactor: ReactorBase = getReactableActor

  def getActor: AbstractActor = myactor

  def getReactableActor: ReactorBase

  /* --- Implement Reactable --- */

  /// sends a unit of work to the actor
  def execute(fun: => Unit) = myactor ! Execute(() => fun)

  class ActorDelayHandler(a: AbstractActor) extends Timer {
    def cancel() = a ! CANCEL

    def now() = a ! NOW
  }

  /// sends a unit of work to do after a specficied time has elapsed.    
  def delay(msec: Long)(fun: => Unit): Timer = {
    new ActorDelayHandler(
      actor {
        link(myactor)
        reactWithin(msec) {
          case CANCEL =>
            unlink(myactor)
          case NOW =>
            unlink(myactor)
            this.execute(fun)
          case TIMEOUT =>
            unlink(myactor)
            this.execute(fun)
        }
      })
  }

  /// send a unit of work to do immediatley and then repeat function until the actor is killed
  def repeat(msec: Long)(fun: => Unit): Timer = {
    new ActorDelayHandler(
      actor {
        val timerActor = self // trying to get self in other contexts is problematic, just get reference here
        var waitingForDoneMessage = false
        link(myactor)
        this.execute(fun)
        loop {
          if (waitingForDoneMessage)
            react {
              case CANCEL =>
                unlink(myactor)
                exit()
              case OPERATION_COMPLETE =>
                waitingForDoneMessage = false
            }
          else {
            reactWithin(msec) {
              case CANCEL =>
                unlink(myactor)
                exit()
              case NOW | TIMEOUT =>
                waitingForDoneMessage = true
                this.execute(fun)
                this.execute({ timerActor ! OPERATION_COMPLETE })
            }
          }
        }
      })
  }

  /// execute a function synchronously and return the value
  def request[A](fun: => A): A = (myactor !? Request(() => fun)).asInstanceOf[A]

  def bind(a: AbstractActor) = myactor ! Link(a)

  final override def doStop() = {}

  final override def doStart() = myactor.start()
}

