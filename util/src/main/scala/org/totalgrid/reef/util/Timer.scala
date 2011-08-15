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
package org.totalgrid.reef.util

import scala.actors.TIMEOUT
import scala.actors.Actor
import scala.actors.Actor._

trait Cancelable {
  /**
   *  Cancel the pending callback
   */
  def cancel()
}

/**
 * Simple interface to a cancelable timer object
 */
trait Timer extends Cancelable {

  /**
   *  Execute the pending callback immediately
   */
  def now()
}

/**
 *  Provides an actor based timer implementation that calls
 *  the specified function from an actor thread
 *
 */
@deprecated("Use Executor.delay instead, these timers don't get stopped automatically")
object Timer {

  case object Cancel
  case object Now

  private class ActorTimer(a: Actor) extends Timer {
    def cancel() = a ! Cancel
    def now() = a ! Now
  }

  /**
   * Sleeps and then calls a function
   *
   *  @param 	delayms 	delay in milliseconds
   *  @param 	fun 		function to call after delay
   *  @return 				Actor implementing the delay
   */
  def delay(delayms: Long)(fun: => Unit): Timer = {
    val a = actor {
      self.reactWithin(delayms) {
        case Timer.Cancel =>
        case Timer.Now => fun
        case TIMEOUT => fun
      }
    }
    new ActorTimer(a)
  }

  def loop(delayms: Long)(fun: => Unit): Timer = {
    val a = actor {
      self.loop {
        self.reactWithin(delayms) {
          case Timer.Cancel => exit
          case Timer.Now => { fun; exit }
          case TIMEOUT => fun
        }
      }
    }
    new ActorTimer(a)
  }

  /**
   * Overload that fires instantly
   *
   *  @param 	fun 	function to call after delay
   *  @return 	Actor 	implementing the delay
   */
  def now(fun: => Unit): Unit = delay(0)(fun)

}

