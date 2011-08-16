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
package org.totalgrid.reef.messaging.javaclient

import org.totalgrid.reef.messaging._
import org.totalgrid.reef.messaging.sync._
import org.totalgrid.reef.broker._
import org.totalgrid.reef.broker.qpid.QpidBrokerConnection

import org.totalgrid.reef.executor.ReactActorExecutor

import org.totalgrid.reef.proto.ReefServicesList
import org.totalgrid.reef.japi.client.{ AMQPConnectionSettings, ConnectionListener, Connection, Session, SessionExecutionPool }
import org.totalgrid.reef.sapi.ServiceList
import org.totalgrid.reef.sapi.client.ClientSession

/**
 * A bridge for easily mapping the Scala messaging constructs onto Java constructs
 *
 * @param settings Settings class to that defines properties of the connection
 * @param services List defining mappings between
 * @param timeoutms Response timeout for service calls in milliseconds
 */
class AMQPConnection(settings: AMQPConnectionSettings, servicesList: ServiceList, timeoutms: Long) extends Connection {

  /**
   * Overloaded constructor that defaults to the Reef services list
   */
  def this(settings: AMQPConnectionSettings, timeoutms: Long) = this(settings, ReefServicesList, timeoutms)

  val config = new BrokerConnectionInfo(settings.getHost, settings.getPort,
    settings.getUser, settings.getPassword,
    settings.getVirtualHost,
    settings.getSsl, settings.getTrustStore, settings.getTrustStorePassword)

  /// Scala factory class we're wrapping to simplify access to java clients
  private val factory = new AMQPSyncFactory with ReactActorExecutor with SessionSource {
    val broker = new QpidBrokerConnection(config)

    def newSession(): ClientSession = new AmqpClientSession(this, servicesList, timeoutms)
  }

  final override def addConnectionListener(listener: ConnectionListener) =
    factory.addConnectionListener(listener)

  final override def removeConnectionListener(listener: ConnectionListener) =
    factory.removeConnectionListener(listener)

  final override def connect(timeoutMs: Long) = factory.connect(timeoutMs)

  final override def start() = factory.start()

  final override def disconnect(timeoutMs: Long) = factory.disconnect(timeoutMs)

  final override def stop() = factory.stop()

  final override def newSession(): Session =
    new SessionWrapper(new AmqpClientSession(factory, servicesList, timeoutms))

  final override def newSessionPool(): SessionExecutionPool = new BasicSessionPool(factory)

}

