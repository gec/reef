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

import org.totalgrid.reef.client.settings.{ NodeSettings, UserSettings }
import org.totalgrid.reef.persistence.squeryl.{ DbConnector, DbInfo }
import org.totalgrid.reef.services.ServiceBootstrap
import org.totalgrid.reef.services.settings.ServiceOptions
import net.agileautomata.executor4s._
import org.totalgrid.reef.services.activator.{ ServiceFactory, ServiceModulesFactory }
import org.totalgrid.reef.client.service.list.ReefServices
import org.totalgrid.reef.loader.LoadManager
import org.totalgrid.reef.loader.commons.{ LoaderServices, LoaderServicesList }
import org.totalgrid.reef.client.settings.util.{ PropertyLoading, PropertyReader }
import com.weiglewilczek.slf4s.Logging
import org.totalgrid.reef.models.CoreServicesSchema
import org.totalgrid.reef.app.impl.{ ApplicationManagerSettings, SimpleConnectedApplicationManager }
import org.totalgrid.reef.measproc.activator.MeasurementProcessorConnectedApplication
import org.totalgrid.reef.frontend.{ ProtocolTraitToManagerShim, FepConnectedApplication }
import org.totalgrid.reef.metrics.service.activator.MetricsServiceApplication
import org.totalgrid.reef.client.factory.ReefConnectionFactory
import org.totalgrid.reef.client.Connection
import org.totalgrid.reef.protocol.api.ProtocolManager

class IntegratedSystem(exe: Executor, configFile: String, resetFirst: Boolean) extends Logging {

  val properties = PropertyReader.readFromFile(configFile)

  val sql = new DbInfo(properties)
  val options = new ServiceOptions(properties)
  val userSettings = new UserSettings(properties)

  import ImplLookup._

  val brokerConnection = loadBrokerConnection(properties, exe)
  val measurementStore = loadMeasurementStore(properties, exe)
  val nodeSettings = loadNodeSettings()

  val modules = new ServiceModulesFactory {
    def getDbConnector() = DbConnector.connect(sql)
    def getMeasStore() = measurementStore
  }

  if (resetFirst) {
    logger.info("Resetting database and measurement store")
    val dbConnection = DbConnector.connect(sql)
    measurementStore.connect()
    CoreServicesSchema.prepareDatabase(dbConnection, true, true)
    ServiceBootstrap.seed(dbConnection, userSettings.getUserPassword)
    measurementStore.reset()
    measurementStore.disconnect()
  }

  val connectionFactory = new ReefConnectionFactory(brokerConnection, exe, new ReefServices)

  // we don't use ConnectionCloseManagerEx because it doesn't start things in the order they were added
  // and starts them all one-by-one rather than all at once
  val manager = new SimpleConnectionProvider(connectionFactory.connect)

  nodeSettings.foreach { nodeSettings =>

    val appManagerSettings = new ApplicationManagerSettings(userSettings, nodeSettings)
    val applicationManager = new SimpleConnectedApplicationManager(exe, manager, appManagerSettings)
    applicationManager.start()

    manager.addConsumer(ServiceFactory.create(options, userSettings, nodeSettings, modules))

    applicationManager.addConnectedApplication(new MeasurementProcessorConnectedApplication(measurementStore))

    // we need to load the protocol separately for each node
    loadProtocols(properties, exe).foreach { protocol =>
      val manager = new ProtocolTraitToManagerShim(protocol)
      applicationManager.addConnectedApplication(new FepConnectedApplication(protocol.name, manager, userSettings))
    }

    applicationManager.addConnectedApplication(new MetricsServiceApplication)
  }

  def connection(): Connection = {
    val conn = connectionFactory.connect()
    conn.addServicesList(new LoaderServicesList)
    conn
  }

  def loadModel(modelFile: String) {
    val client = connection().login(userSettings)
    LoadManager.loadFile(client.getService(classOf[LoaderServices]), modelFile, false, false, false, 25)
  }

  def addProtocol(protocolName: String, protocol: ProtocolManager) {

    val firstNodeSettings = nodeSettings.head

    val appManagerSettings = new ApplicationManagerSettings(userSettings, firstNodeSettings)
    val applicationManager = new SimpleConnectedApplicationManager(exe, InMemoryNode.system.manager, appManagerSettings)
    applicationManager.start()

    applicationManager.addConnectedApplication(new FepConnectedApplication(protocolName, protocol, userSettings))
  }

  def start() = {
    manager.start()
  }
  def stop() = {
    manager.stop()
  }

  private def loadNodeSettings() = {
    val rootNodeSettings = new NodeSettings(properties)
    val nodeNames = PropertyLoading.getString("org.totalgrid.reef.nodeNames", properties, "node01").split(",").toList
    logger.info("Nodes: " + nodeNames)
    nodeNames.map { nodeName =>
      new NodeSettings(nodeName, rootNodeSettings.getLocation, rootNodeSettings.getNetworks)
    }
  }
}
