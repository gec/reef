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
package org.totalgrid.reef.messaging

import org.totalgrid.reef.util.SyncVar
import org.totalgrid.reef.api.{ ServiceIOException, IConnectionListener }

class BrokerConnectionState extends IConnectionListener {
  private val connected = new SyncVar(false)

  override def opened() = {
    println(connected + "Connected: true")
    connected.update(true)
  }
  override def closed() = {
    println(connected + "Connected: false")
    connected.update(false)
  }

  /**
   * @param timeout how long to wait in milliseconds before failing
   * @param exceptionMessage Text to put into exception on failure
   */
  def waitUntilStarted(timeout: Long, exceptionMessage: => String) = {
    println(connected + "waitStart")
    connected.waitUntil(true, timeout, true, Some(new ServiceIOException(exceptionMessage)))
  }

  /**
   * @param timeout how long to wait in milliseconds before failing
   * @param exceptionMessage Text to put into exception on failure
   */
  def waitUntilStopped(timeout: Long, exceptionMessage: => String) = {
    println(connected + "waitStop")
    connected.waitUntil(false, timeout, true, Some(new ServiceIOException(exceptionMessage)))
  }
}

