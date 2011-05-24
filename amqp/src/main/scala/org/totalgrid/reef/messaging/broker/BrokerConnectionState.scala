package org.totalgrid.reef.messaging.broker

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
import org.totalgrid.reef.util.SyncVar
import org.totalgrid.reef.api.javaclient.ConnectionListener
import org.totalgrid.reef.api.{ ServiceIOException }

class BrokerConnectionState extends ConnectionListener {
  private val connected = new SyncVar(false)

  final override def onConnectionOpened() = connected.update(true)
  final override def onConnectionClosed(expected: Boolean) = connected.update(false)

  /**
   * @param timeout how long to wait in milliseconds before failing
   * @param exceptionMessage Text to put into exception on failure
   */
  def waitUntilConnected(timeout: Long = 5000, exceptionMessage: => String = "Timeout waiting for onConnectionOpen()") =
    connected.waitUntil(true, timeout, true, Some((b: Boolean) => new ServiceIOException(exceptionMessage)))

  /**
   * @param timeout how long to wait in milliseconds before failing
   * @param exceptionMessage Text to put into exception on failure
   */
  def waitUntilDisconnected(timeout: Long = 5000, exceptionMessage: => String = "Timeout waiting for onConnectionClosed()") =
    connected.waitUntil(false, timeout, true, Some((b: Boolean) => new ServiceIOException(exceptionMessage)))
}

