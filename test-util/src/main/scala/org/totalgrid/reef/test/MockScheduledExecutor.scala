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
package org.totalgrid.reef.test

import java.util.concurrent.{ Delayed, ScheduledFuture, Callable, TimeUnit, ScheduledExecutorService }
import java.util.{ ArrayList, List => JavaList, Collection }

// TODO - remove this class in favor of the robust org.jmock.lib.concurrent.DeterministicScheduler)
@deprecated("Use org.jmock.lib.concurrent.DeterministicScheduler instead")
class MockScheduledExecutor extends ScheduledExecutorService {
  private var shutdownFlag: Boolean = false;
  private var terminatedFlag: Boolean = false;
  private var scheduleAtFixedRateCapture: List[Tuple4[Runnable, Long, Long, TimeUnit]] = List.empty

  def getScheduleAtFixedRateCapture(): List[Tuple4[Runnable, Long, Long, TimeUnit]] =
    {
      val reversed: List[(Runnable, Long, Long, TimeUnit)] = scheduleAtFixedRateCapture.reverse
      reversed
    }

  def awaitTermination(timeout: Long, unit: TimeUnit) = false

  def invokeAll[T](tasks: Collection[_ <: Callable[T]]) = null

  def invokeAll[T](tasks: Collection[_ <: Callable[T]], timeout: Long, unit: TimeUnit) = null

  def invokeAny[T](tasks: Collection[_ <: Callable[T]]) = null.asInstanceOf[T]

  def invokeAny[T](tasks: Collection[_ <: Callable[T]], timeout: Long, unit: TimeUnit) = null.asInstanceOf[T]

  def isShutdown = shutdownFlag

  def isTerminated = terminatedFlag

  def shutdown() {
    shutdownFlag = true
    terminatedFlag = true
  }

  def shutdownNow() =
    {
      shutdown()
      new ArrayList[Runnable]().asInstanceOf[JavaList[Runnable]]
    }

  def submit[T](task: Callable[T]) = null

  def submit(task: Runnable) = null

  def submit[T](task: Runnable, result: T) = null

  def schedule[V](callable: Callable[V], delay: Long, unit: TimeUnit) = null

  def schedule(command: Runnable, delay: Long, unit: TimeUnit) = null

  def scheduleAtFixedRate(command: Runnable, initialDelay: Long, period: Long, unit: TimeUnit) =
    {
      scheduleAtFixedRateCapture = Tuple4(command, initialDelay, period, unit) :: scheduleAtFixedRateCapture
      new MockScheduledFuture(initialDelay)
    }

  def scheduleWithFixedDelay(command: Runnable, initialDelay: Long, delay: Long, unit: TimeUnit) = null

  def execute(command: Runnable) {}

}

@deprecated("Use org.jmock.lib.concurrent.DeterministicScheduler instead")
class MockScheduledFuture(delay: Long) extends ScheduledFuture[Object] {
  def cancel(mayInterruptIfRunning: Boolean) = false

  def get() = null

  def get(timeout: Long, unit: TimeUnit) = null

  def isCancelled = false

  def isDone = false

  def compareTo(delayed: Delayed) =
    {
      if (delayed == this) {
        0
      } else {
        1
      }

    }

  def getDelay(unit: TimeUnit) = delay
}