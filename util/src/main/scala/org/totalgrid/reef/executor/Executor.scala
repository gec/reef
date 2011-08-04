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

import org.totalgrid.reef.util.Timer
import parallel.Future

/**
 * Concurrency pattern for abstracting the execution of work on some thread-like implementation.
 */
trait Executor {

  /**
   * dispatches a unit of work immediately. If the fun throws, the executor catches the exception. This
   * function is "fire-and-forget"
   */
  def execute(fun: => Unit): Unit

  /**
   * dispatches a unit of work to do after a specified time has elapsed.
   */
  def delay(msec: Long)(fun: => Unit): Timer

  /**
   * dispatches a unit of work immediately and then periodically keeping a constant _delay_
   * between the end of previous unit of work and the beginning of the next invocation of the callback.
   *
   * This means calling repeat(500){Thread.sleep(1000)} will get executed at t = 0 and t = 1500 milliseconds,
   * not t = 0, t = 500, t = 1000.
   */
  def repeat(msec: Long)(fun: => Unit): Timer

  /**
   * dispatches a unit of work, the result of which is returned as a future. If the fun throws,
   * the future's apply method will throw the same exception
   */
  def request[A](fun: => A): Future[A]

  /**
   * Blocks until all previously made "execute/request" calls have completed
   */
  final def sync() = request {}()

}

object Executor {
  /**
   *  sets the scala runtime up with the standard thread pool setup (resizable for now)
   */
  def setupThreadPools {
    // http://scala-programming-language.1934581.n4.nabble.com/Increase-actor-thread-pool-td1936329.html
    // un-deadlocks the measproc when it tries to load lots of resources at one time. Problem is due to
    // inline actors all blocking on service futures and starving the AMQP actors so it cant receive the 
    // service resposes and causing erroneous timeouts
    import scala.actors.Scheduler
    import scala.actors.scheduler.ResizableThreadPoolScheduler
    Scheduler.impl = {
      val s = new ResizableThreadPoolScheduler(false)
      s.start()
      s
    }
  }
}

