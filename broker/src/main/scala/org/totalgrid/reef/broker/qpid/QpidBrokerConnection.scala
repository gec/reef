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
package org.totalgrid.reef.broker.qpid

import org.totalgrid.reef.broker._
import org.apache.qpid.transport._
import com.weiglewilczek.slf4s.Logging
import org.totalgrid.reef.clientapi.exceptions.ServiceIOException

final class QpidBrokerConnection(conn: Connection) extends QpidBrokerChannelPool with ConnectionListener with Logging {

  private var disconnected = false

  conn.addConnectionListener(this)

  override def newChannel() = new QpidWorkerChannel(conn.createSession(0))

  override def disconnect(): Boolean = mutex.synchronized {
    if (!disconnected) {
      disconnected = true
      closeWorkerChannels()
      conn.close()
      true
    } else true
  }

  def listen(): BrokerSubscription = {
    if (disconnected) throw new ServiceIOException("Connection closed")
    val session = conn.createSession(0)
    val q = QpidChannelOperations.declareQueue(session, "*", true, true)
    val subscription = new QpidBrokerSubscription(session, q)
    subscription
  }

  def listen(queue: String): BrokerSubscription = {
    if (disconnected) throw new ServiceIOException("Connection closed")
    val session = conn.createSession(0)
    val q = QpidChannelOperations.declareQueue(session, queue, false, false)
    assert(queue == q)
    val subscription = new QpidBrokerSubscription(session, q)
    subscription
  }

  /* --- Implement ConnectionListener --- */

  def closed(conn: Connection) = {
    this.onDisconnect(disconnected)
    disconnected = true
  }

  def opened(conn: Connection) = {}

  def exception(conn: Connection, ex: ConnectionException) = logger.error("Exception on qpid connection", ex)

  /* -- End Qpid Connection Listener -- */
}
