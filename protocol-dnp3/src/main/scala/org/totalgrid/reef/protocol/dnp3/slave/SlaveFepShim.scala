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
package org.totalgrid.reef.protocol.dnp3.slave

import org.totalgrid.reef.broker.BrokerProperties
import org.totalgrid.reef.osgi.OsgiConfigReader
import org.totalgrid.reef.broker.qpid.QpidBrokerConnection
import org.totalgrid.reef.proto.ReefServicesList
import org.totalgrid.reef.messaging.AmqpClientSession
import org.totalgrid.reef.japi.request.impl.{ SingleSessionClientSource, AllScadaServiceImpl }
import com.weiglewilczek.scalamodules._
import org.totalgrid.reef.protocol.api.{ Protocol, AddRemoveValidation }
import org.totalgrid.reef.japi.client.{ ConnectionListener, UserSettings }
import org.osgi.framework.{ ServiceRegistration, BundleContext }
import org.totalgrid.reef.messaging.sync.AMQPSyncFactory
import org.totalgrid.reef.executor.{ Executor, LifecycleManager, ReactActorExecutor }
import org.totalgrid.reef.util.{ Logging, Timer }

/**
 * this class is a stop gap measure until we get the FEP reimplemented to provide a Client and exe to the
 * Protocols.
 * TODO: reimplement FEP to give client to Protocols
 */
class SlaveFepShim extends Logging {

  private val manager = new LifecycleManager

  private var protocol: Option[Dnp3SlaveProtocol] = None
  private var registration: Option[ServiceRegistration] = None
  private var reconnect: Option[Timer] = None

  def start(context: BundleContext) {
    org.totalgrid.reef.executor.Executor.setupThreadPools

    val brokerOptions = BrokerProperties.get(new OsgiConfigReader(context, "org.totalgrid.reef.amqp"))
    val userSettings = new UserSettings(new OsgiConfigReader(context, "org.totalgrid.reef.user").getProperties)

    val factory = new AMQPSyncFactory with ReactActorExecutor {
      val broker = new QpidBrokerConnection(brokerOptions)
    }

    val exe = new ReactActorExecutor {}

    factory.addConnectionListener(new ConnectionListener {

      def onConnectionOpened() {
        // need to get off this callback thread
        exe.execute { createProtocol(factory, userSettings, exe, context) }
      }

      def onConnectionClosed(expected: Boolean) {
        stopConnecting()
      }
    })

    manager.add(factory)
    manager.add(exe)
    manager.start()
  }

  def stop(context: BundleContext) {
    stopConnecting()
    manager.stop()
  }

  private def stopConnecting() {
    reconnect.foreach { _.cancel() }
    protocol.foreach { _.Shutdown() }
    protocol = None
    registration.foreach { _.unregister() }
    registration = None
  }

  private def createProtocol(factory: AMQPSyncFactory, userSettings: UserSettings, exe: Executor, context: BundleContext) {
    try {
      val client = new AmqpClientSession(factory, ReefServicesList, 5000) with AllScadaServiceImpl with SingleSessionClientSource {
        def session = this
      }
      val token = client.createNewAuthorizationToken(userSettings.getUserName, userSettings.getUserPassword)
      client.getDefaultHeaders.setAuthToken(token)

      val slaveProtocol = new Dnp3SlaveProtocol(client, exe) with AddRemoveValidation
      protocol = Some(slaveProtocol)
      registration = Some(context.createService(slaveProtocol, "protocol" -> slaveProtocol.name, interface[Protocol]))
    } catch {
      case ex: Exception =>
        logger.warn("Dnp3 Slave shim couldn't connect.")
        reconnect = Some(exe.delay(1000) { createProtocol(factory, userSettings, exe, context) })
    }
  }
}