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
package org.totalgrid.reef.persistence

import org.totalgrid.reef.executor.Executor
import org.totalgrid.reef.metrics.{ MetricsHooks }

/**
 * Defines synchronous and asynchronous operation interface
 * for a particular connection type
 * TODO: get ConnectionOperations to work with Unit
 */
trait ConnectionOperations[ConnType] {

  type AsyncOperation = ConnType => Unit
  type SyncOperation[A] = ConnType => Option[A]

  def doAsync(func: AsyncOperation)
  def doSync[A](func: SyncOperation[A]): Option[A]
}

/**
 * A totally synchronous implementation of the cache so we can easily test
 * the actual behavior of the cache without relying on the reconnecting/caching
 * actor behaving correctly.
 */
class LockStepConnection[ConnectionType](r: ConnectionType) extends ConnectionOperations[ConnectionType] {
  def doAsync(fun: AsyncOperation): Unit = {
    fun(r)
  }
  def doSync[A](fun: SyncOperation[A]): Option[A] = {
    fun(r)
  }
}

/**
 * Reactor class that buffers async operations in a list until a connection is restored.
 */
abstract class AsyncBufferReactor[ConnType](val exe: Executor, obs: ConnectionReactor.Observer, val connectOnStart: Boolean = true)
    extends ConnectionReactor[ConnType] with ConnectionOperations[ConnType] with MetricsHooks {

  private lazy val delayedPuts = counterHook("delayedPuts")
  private lazy val puts = counterHook("puts")
  private lazy val gets = counterHook("gets")
  private lazy val getLatency = timingHook("getTime")

  private var buffer: List[AsyncOperation] = Nil

  /// try to flush all of the operations in the buffer
  private def flush() = {
    buffer = buffer.filterNot { fun => op { fun } }
    delayedPuts(buffer.size)
  }

  /// Implementation for abstract from ConnectionReactor
  override def onConnectionChange(conn: Option[ConnType]) {
    conn match {
      case Some(x) =>
        obs(true)
        flush()
      case None =>
        obs(false)
    }
  }

  /// Execute an empty block, this makes us block for a response
  def sync(): Unit = exe.sync()

  def doAsync(fun: AsyncOperation): Unit = {
    puts(1)
    exe.execute {
      op { fun } match {
        case true =>
        case false =>
          buffer = fun :: buffer
          delayedPuts(buffer.size)
      }
    }
  }

  def doSync[A](fun: SyncOperation[A]): Option[A] = {
    gets(1)
    //TODO : fix the timingHook so that it doesn't require a dynamic cast
    getLatency {
      exe.request(op(fun)).apply()
    }.asInstanceOf[Option[A]]
  }

  protected def op(f: (ConnType) => Unit): Boolean = {
    try {
      connection match {
        case Some(c) => f(c); true
        case None => false
      }
    } catch {
      case t: Throwable =>
        logger.error(t.getMessage, t)
        setConnection(None)
        connect()
        false
    }
  }

  protected def op[A](f: (ConnType) => Option[A]): Option[A] = {
    try {
      connection match {
        case Some(c) => f(c)
        case None => None
      }
    } catch {
      case t: Throwable =>
        logger.error(t.getMessage, t)
        setConnection(None)
        connect()
        None
    }
  }

}