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

import org.totalgrid.reef.osgi.OsgiConfigReader
import com.weiglewilczek.scalamodules._
import org.totalgrid.reef.api.protocol.api.{ Protocol, AddRemoveValidation }

import org.osgi.framework.BundleContext

import com.weiglewilczek.slf4s.Logging
import org.totalgrid.reef.api.sapi.client.rest.{ Client, Connection }
import org.totalgrid.reef.client.sapi.rpc.AllScadaService
import org.totalgrid.reef.proto.Application.ApplicationConfig

import org.totalgrid.reef.util.Cancelable
import org.totalgrid.reef.app.{ ConnectionCloseManagerEx, ApplicationEnrollerEx, AppEnrollerConsumer, UserLogin }
import org.totalgrid.reef.api.japi.settings.{ AmqpSettings, UserSettings, NodeSettings }
import net.agileautomata.executor4s.Executor

object SlaveFepShim {
  def createFepShim(userSettings: UserSettings, nodeSettings: NodeSettings, context: BundleContext): UserLogin = {
    val appConfigConsumer = new AppEnrollerConsumer {
      def applicationRegistered(client: Client, services: AllScadaService, appConfig: ApplicationConfig) = {

        val slaveProtocol = new Dnp3SlaveProtocol(services) with AddRemoveValidation
        val protocol = Some(slaveProtocol)
        val registration = Some(context.createService(slaveProtocol, "protocol" -> slaveProtocol.name, interface[Protocol]))

        new Cancelable {
          def cancel() {
            protocol.foreach(_.Shutdown)
            registration.foreach { _.unregister() }
          }
        }
      }
    }
    val appEnroller = new ApplicationEnrollerEx(nodeSettings, "Processing-" + nodeSettings.getDefaultNodeName, List("Processing"), appConfigConsumer)
    val userLogin = new UserLogin(userSettings, appEnroller)
    userLogin
  }
}

/**
 * this class is a stop gap measure until we get the FEP reimplemented to provide a Client and exe to the
 * Protocols.
 * TODO: reimplement FEP to give client to Protocols
 */
class SlaveFepShim extends Logging {

  private var manager = Option.empty[ConnectionCloseManagerEx]

  def start(context: BundleContext) {

    val brokerOptions = new AmqpSettings(OsgiConfigReader(context, "org.totalgrid.reef.amqp").getProperties)
    val userSettings = new UserSettings(OsgiConfigReader(context, "org.totalgrid.reef.user").getProperties)
    val nodeSettings = new NodeSettings(OsgiConfigReader(context, "org.totalgrid.reef.node").getProperties)

    val exe = context findService withInterface[Executor] andApply (x => x) match {
      case Some(x) => x
      case None => throw new Exception("Unable to find required executor pool")
    }

    manager = Some(new ConnectionCloseManagerEx(brokerOptions, exe))

    manager.get.addConsumer(SlaveFepShim.createFepShim(userSettings, nodeSettings, context))

    manager.foreach { _.start }
  }

  def stop(context: BundleContext) = {
    manager.foreach { _.stop }
  }
}