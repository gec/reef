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
import org.totalgrid.reef.client.sapi.service.AsyncService
import org.totalgrid.reef.services.{ ServiceBootstrap, ServiceOptions }
import net.agileautomata.executor4s._
import org.totalgrid.reef.services.activator.{ ServiceFactory, ServiceModulesFactory }
import org.totalgrid.reef.measproc.activator.ProcessingActivator
import org.totalgrid.reef.entry.FepEntry
import org.totalgrid.reef.client.sapi.client.rest.impl.DefaultConnection
import org.totalgrid.reef.client.service.list.ReefServices
import org.totalgrid.reef.shell.proto.ProtoShellApplication
import org.totalgrid.reef.loader.LoadManager
import org.totalgrid.reef.loader.commons.{ LoaderServices, LoaderServicesList }
import org.totalgrid.reef.client.settings.util.{ PropertyLoading, PropertyReader }
import com.weiglewilczek.slf4s.Logging

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
    def publishServices(services: Seq[AsyncService[_]]) = {}
  }

  if (resetFirst) {
    logger.info("Resetting database and measurement store")
    DbConnector.connect(sql)
    measurementStore.connect()
    ServiceBootstrap.resetDb()
    ServiceBootstrap.seed(userSettings.getUserPassword)
    measurementStore.reset()
  }

  // we don't use ConnectionCloseManagerEx because it doesn't start things in the order they were added
  // and starts them all one-by-one rather than all at once
  //val manager = new ConnectionCloseManagerEx(brokerConnection, exe)
  val manager = new SimpleConnectionProvider(brokerConnection, exe)

  nodeSettings.foreach { nodeSettings =>

    manager.addConsumer(ServiceFactory.create(options, userSettings, nodeSettings, modules))

    manager.addConsumer(ProcessingActivator.createMeasProcessor(userSettings, nodeSettings, measurementStore))

    // we need to load the protocol separately for each node
    loadProtocols(properties, exe).foreach { protocol =>
      manager.addConsumer(FepEntry.createFepConsumer(userSettings, nodeSettings, protocol))
    }
  }

  def connection() = {
    val clientConnection = new DefaultConnection(brokerConnection.connect, exe, 15000)
    clientConnection.addServicesList(new ReefServices)
    clientConnection.addServicesList(new LoaderServicesList)
    clientConnection
  }

  def runTerminal() {

    System.setProperty("jline.terminal", "jline.UnsupportedTerminal")
    ProtoShellApplication.runTerminal(connection(), userSettings, brokerConnection.toString(), NullCancelable)
  }

  def loadModel(modelFile: String) {
    val client = connection().login(userSettings.getUserName, userSettings.getUserPassword).await
    LoadManager.loadFile(client.getRpcInterface(classOf[LoaderServices]), modelFile, false, false, false, 25)
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
      new NodeSettings(nodeName, rootNodeSettings.getLocation, rootNodeSettings.getNetwork)
    }
  }
}
