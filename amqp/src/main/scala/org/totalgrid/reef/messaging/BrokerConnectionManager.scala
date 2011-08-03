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

package org.totalgrid.reef.messaging

import org.totalgrid.reef.japi.client.ConnectionListener
import org.totalgrid.reef.broker.BrokerConnection
import org.totalgrid.reef.executor.{ Executor, Lifecycle }
import org.totalgrid.reef.util.{ Timer, Logging }

/**
 * Keeps the connection to the broker open
 */
class BrokerConnectionManager(exe: Executor, broker: BrokerConnection, initialDelay: Long, maxDelay: Long) extends Lifecycle
    with ConnectionListener with Logging {

  def this(exe: Executor, broker: BrokerConnection) = this(exe, broker, 1000, 60000)

  private var timer: Option[Timer] = None

  broker.addListener(this)

  /// Kicks of the connection process
  final override def afterStart() = this.reconnect()

  /// Terminates all the connections and machinery
  final override def beforeStop() = exe.execute {
    timer.foreach(_.cancel())
    broker.disconnect()
  }

  // helper for starting a new connection chain
  private def reconnect() = exe.execute(attemptConnection(initialDelay))

  /// Makes a connection attempt. Retries if with exponential back-off
  /// if the attempt fails
  private def attemptConnection(retryms: Long): Unit = {
    if (!broker.connect())
      timer = Some(exe.delay(retryms)(attemptConnection(math.min(2 * retryms, maxDelay))))
  }

  /* --- Implement Broker Connection Listener --- */

  final override def onConnectionClosed(expected: Boolean) {
    logger.info(" Connection closed, expected:" + expected)
    if (!expected) timer = Some(exe.delay(initialDelay)(reconnect()))
  }

  final override def onConnectionOpened() = logger.info("Connection opened")
}
