package org.totalgrid.reef.messaging.synchronous

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

import org.totalgrid.reef.japi.client.ConnectionListener
import org.totalgrid.reef.broker.api.BrokerConnection
import org.totalgrid.reef.util.Logging
import org.totalgrid.reef.executor.Lifecycle

import net.agileautomata.executor4s._

/**
 * Keeps the connection to the broker open
 */
class BrokerConnectionManager(broker: BrokerConnection, initialDelay: Long, maxDelay: Long, strand: StrandLifeCycle)
    extends Lifecycle with ConnectionListener with Logging {

  broker.addListener(this)

  final override def onConnectionClosed(expected: Boolean) = {
    logger.info(" Connection closed, expected:" + expected)
    if (!expected) retry(initialDelay)
  }

  final override def onConnectionOpened() = logger.info("Connection opened")

  /// Kicks of the connection process
  final override def doStart() = strand.execute(connect(initialDelay))

  /// Terminates all the connections and machinery
  final override def doStop() = strand.terminate(broker.disconnect())

  /// Makes a connection attempt. Retries if with exponential back-off
  /// if the attempt fails
  private def connect(retryms: Long): Unit = if (!broker.connect()) retry(retryms)

  private def retry(retryms: Long) =
    strand.delay(retryms.milliseconds)(connect(math.min(2 * retryms, maxDelay)))

}