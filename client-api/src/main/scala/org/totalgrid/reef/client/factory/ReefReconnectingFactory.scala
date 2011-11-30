package org.totalgrid.reef.client.factory

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
import org.totalgrid.reef.client.settings.AmqpSettings
import org.totalgrid.reef.client.sapi.client.rest.{ ConnectionWatcher => SConnectionWatcher }
import org.totalgrid.reef.client.javaimpl.ConnectionWrapper
import org.totalgrid.reef.broker.qpid.QpidBrokerConnectionFactory
import net.agileautomata.executor4s.Executors
import org.totalgrid.reef.broker.BrokerConnection
import org.totalgrid.reef.client.sapi.client.rest.impl.{ DefaultConnection, DefaultReconnectingFactory }
import org.totalgrid.reef.client.{ ServicesList, ReconnectingConnectionFactory, ConnectionWatcher }
import org.totalgrid.reef.client.ServicesList

class ReefReconnectingFactory(amqpSettings: AmqpSettings, servicesList: ServicesList, startDelay: Long, maxDelay: Long)
    extends ReconnectingConnectionFactory with SConnectionWatcher {

  private val brokerFactory = new QpidBrokerConnectionFactory(amqpSettings)
  private val exe = Executors.newScheduledThreadPool(5)

  private val factory = new DefaultReconnectingFactory(brokerFactory, exe, startDelay, maxDelay)
  factory.addConnectionWatcher(this)

  private var watchers = Set.empty[ConnectionWatcher]

  def addConnectionWatcher(watcher: ConnectionWatcher) = this.synchronized { watchers += watcher }
  def removeConnectionWatcher(watcher: ConnectionWatcher) = this.synchronized { watchers -= watcher }

  def onConnectionClosed(expected: Boolean) =
    this.synchronized { watchers.foreach { _.onConnectionClosed(expected) } }

  def onConnectionOpened(broker: BrokerConnection) = {
    val connection = new ConnectionWrapper(new DefaultConnection(broker, exe, 5000))
    connection.addServicesList(servicesList)
    this.synchronized { watchers.foreach { _.onConnectionOpened(connection) } }
  }

  def start() = factory.start()
  def stop() = {
    factory.stop()
    exe.terminate()
  }
}