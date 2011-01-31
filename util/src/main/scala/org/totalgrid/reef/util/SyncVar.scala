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
package org.totalgrid.reef.util

import scala.annotation.tailrec
import scala.collection.mutable.Queue

// New implementation of sync var uses standard synchronization and a mutable queue
class SyncVar[T <: Any](initialValue: T) {

  val defaultTimeout = 5000

  private var queue = new Queue[T]
  queue.enqueue(initialValue)

  def current = queue.synchronized { queue.last }

  def update(value: T): Unit = queue.synchronized {
    queue.enqueue(value)
    queue.notifyAll
  }

  def atomic(fun: T => T): Unit = queue.synchronized {
    queue.enqueue(fun(queue.last))
    queue.notifyAll
  }

  def lastValueAfter(msec: Long): T = {
    waitFor(x => false, msec, false)
    current
  }

  def waitUntil(value: T, msec: Long = defaultTimeout, throwOnFailure: Boolean = true): Boolean = {
    waitFor(current => current == value, msec, throwOnFailure)
  }

  def waitWhile(value: T, msec: Long = defaultTimeout, throwOnFailure: Boolean = true): Boolean = {
    waitFor(current => current != value, msec, throwOnFailure)
  }

  def waitFor(fun: T => Boolean, msec: Long = defaultTimeout, throwOnFailure: Boolean = true): Boolean = queue.synchronized {

    @tailrec
    def waitUntilExpiration(fun: T => Boolean, expiration: Long): Boolean = {
      if (evaluate(fun)) true
      else {
        val diff = expiration - System.currentTimeMillis
        if (diff > 0) {
          queue.wait(diff)
          waitUntilExpiration(fun, expiration)
        } else {
          if (throwOnFailure) throw new Exception("Condition not met, final value was: " + queue.last)
          else false
        }
      }
    }

    waitUntilExpiration(fun, System.currentTimeMillis + msec)
  }

  @tailrec
  private def evaluate(fun: T => Boolean): Boolean = {
    val i = queue.dequeue()
    if (queue.size == 0) {
      queue.enqueue(i) //never let the queue be empty
      fun(i)
    } else {
      if (fun(i)) true
      else evaluate(fun)
    }
  }

}

