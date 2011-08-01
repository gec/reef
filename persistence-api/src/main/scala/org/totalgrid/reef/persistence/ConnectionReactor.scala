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

import org.totalgrid.reef.executor.{ Executor, Lifecycle }
import org.totalgrid.reef.util.{ Logging, Timer }

import scala.actors.Actor._

object ConnectionReactor {

  val startdelay = 500
  val maxdelay = 10000
  val retryms = 5000

  def nextDelay(delay: Long): Long = {
    val dbl = 2 * delay
    if (dbl < maxdelay) dbl else maxdelay
  }

  type Observer = (Boolean) => Unit

}

import ConnectionReactor._

/// Implements connection/reconnection behavior on a 
trait ConnectionReactor[ConnType] extends Logging {

  val exe: Executor

  val connectOnStart: Boolean

  /// Abstract event handler for connection changes
  protected def onConnectionChange(conn: Option[ConnType])

  /// Abstract connection method defined by concrete class
  protected def connectFun(): Option[ConnType]

  /// Option for the connection state
  protected var connection: Option[ConnType] = None

  if (connectOnStart) connect

  /**
   * Starts connection manually. Only valid if connectOnStart = false
   */
  def connect(): Unit = connect(startdelay)

  protected def setConnection(conn: Option[ConnType]) {
    connection = conn
    onConnectionChange(connection)
  }

  private def connect(delay: Long) = exe.execute {
    val result = connectFun()
    handleConnectionAttempt(result, delay)
  }

  private def handleConnectionAttempt(result: Option[ConnType], delayTime: Long) {
    result match {
      case Some(c) =>
        // I am doing a dynamic cast here to avoid the type-erasure warning, it is not possible
        // for the connection result to be of a different type here
        setConnection(Some(c.asInstanceOf[ConnType]))
      case None =>
        exe.delay(delayTime) { connect(nextDelay(delayTime)) }
    }
  }

}
