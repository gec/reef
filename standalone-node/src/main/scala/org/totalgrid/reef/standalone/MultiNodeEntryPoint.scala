/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the GNU Affero General Public License
 * Version 3.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.standalone

import org.totalgrid.reef.client.settings.util.PropertyReader
import org.totalgrid.reef.services.ServiceOptions
import org.totalgrid.reef.client.settings.{ NodeSettings, UserSettings, AmqpSettings }
import org.totalgrid.reef.persistence.squeryl.{ DbConnector, DbInfo }
import org.totalgrid.reef.measurementstore.squeryl.SqlMeasurementStore
import org.totalgrid.reef.client.sapi.service.AsyncService
import org.totalgrid.reef.services.activator.{ ServiceFactory, ServiceModulesFactory }
import org.totalgrid.reef.measproc.activator.ProcessingActivator
import org.totalgrid.reef.entry.FepEntry
import net.agileautomata.executor4s._
import org.totalgrid.reef.simulator.random.{ DefaultSimulator, DefaultSimulatorFactory }
import org.totalgrid.reef.protocol.simulator.SimulatedProtocol
import org.totalgrid.reef.broker.qpid.QpidBrokerConnectionFactory
import org.totalgrid.reef.shell.proto.ProtoShellApplication
import org.totalgrid.reef.client.sapi.client.rest.impl.DefaultConnection
import org.totalgrid.reef.client.service.list.ReefServices
import org.totalgrid.reef.protocol.dnp3.master.Dnp3MasterProtocol
import org.totalgrid.reef.protocol.api.{ AddRemoveValidation, ChannelAlwaysOnline, EndpointAlwaysOnline }
import org.totalgrid.reef.protocol.dnp3.slave.Dnp3SlaveProtocol
import org.totalgrid.reef.measurementstore.InMemoryMeasurementStore
import org.totalgrid.reef.app.{ ConnectionProvider, ConnectionConsumer, ConnectionCloseManagerEx }

object MultiNodeEntryPoint {
  def main(args: Array[String]) = {

    val exe = Executors.newResizingThreadPool(1.minutes)

    val properties = PropertyReader.readFromFile("standalone-node.cfg")

    val sql = new DbInfo(properties)
    val options = new ServiceOptions(properties)
    val userSettings = new UserSettings(properties)
    val rootNodeSettings = new NodeSettings(properties)

    val brokerConfig = new AmqpSettings(properties)
    val brokerConnection = new QpidBrokerConnectionFactory(brokerConfig)
    // val brokerConnection = new MemoryBrokerConnectionFactory(exe)

    val measurementStore = new SqlMeasurementStore({ () => })
    // val measurementStore = new InMemoryMeasurementStore()

    val modules = new ServiceModulesFactory {
      def getDbConnector() = DbConnector.connect(sql)
      def getMeasStore() = measurementStore
      def publishServices(services: Seq[AsyncService[_]]) = {}
    }

    // start up a number of nodes
    val firstNode = 1
    val lastNode = 3

    val nodes = (firstNode to lastNode).map { i =>
      new NodeSettings(rootNodeSettings.getDefaultNodeName.replace("1", i.toString), rootNodeSettings.getLocation, rootNodeSettings.getNetwork)
    }

    val cancelable = new Cancelable { def cancel() {} }

    // we don't use ConnectionCloseManagerEx because it doesn't start things in the order they were added
    // and starts them all one-by-one rather than all at once
    //val manager = new ConnectionCloseManagerEx(brokerConnection, exe)
    val manager = new ConnectionProvider {
      var cancelables = List.empty[(Cancelable, ConnectionConsumer)]
      def addConsumer(consumer: ConnectionConsumer) {
        cancelables ::= (consumer.newConnection(brokerConnection.connect, exe), consumer)
      }
      def removeConsumer(consumer: ConnectionConsumer) {
        cancelables.find(_._2 == consumer).foreach(_._1.cancel())
        cancelables = cancelables.filter(_._2 == consumer)
      }

      def start() = {}
      def stop() = cancelables.foreach { _._1.cancel() }
    }

    nodes.foreach { nodeSettings =>

      manager.addConsumer(ServiceFactory.create(options, userSettings, nodeSettings, modules))

      manager.addConsumer(ProcessingActivator.createMeasProcessor(userSettings, nodeSettings, measurementStore))

      val simFactory = new DefaultSimulatorFactory({ t: DefaultSimulator => cancelable })
      val simProtocol = new SimulatedProtocol(exe) with EndpointAlwaysOnline with ChannelAlwaysOnline
      simProtocol.addPluginFactory(simFactory)

      System.loadLibrary("dnp3java")
      System.setProperty("reef.api.protocol.dnp3.nostaticload", "")
      val masterProtocol = new Dnp3MasterProtocol with AddRemoveValidation
      val slaveProtocol = new Dnp3SlaveProtocol with AddRemoveValidation

      val protocols = List(simProtocol, masterProtocol, slaveProtocol)

      protocols.foreach { protocol =>
        manager.addConsumer(FepEntry.createFepConsumer(userSettings, nodeSettings, protocol))
      }
    }
    try {
      manager.start()

      val connection = new DefaultConnection(brokerConnection.connect, exe, 15000)
      connection.addServicesList(new ReefServices)

      System.setProperty("jline.terminal", "jline.UnsupportedTerminal")
      ProtoShellApplication.runTerminal(connection, userSettings, brokerConnection.toString(), cancelable)

      manager.stop()
    } finally {
      exe.terminate()
    }

  }
}