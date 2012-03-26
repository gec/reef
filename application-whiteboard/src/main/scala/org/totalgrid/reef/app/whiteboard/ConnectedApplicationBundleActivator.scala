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
package org.totalgrid.reef.app.whiteboard

import org.totalgrid.reef.osgi.ExecutorBundleActivator
import com.weiglewilczek.slf4s.Logging
import org.osgi.framework.BundleContext
import net.agileautomata.executor4s.Executor
import org.totalgrid.reef.osgi.OsgiConfigReader
import org.totalgrid.reef.client.settings.{ NodeSettings, UserSettings, AmqpSettings }
import org.totalgrid.reef.app.impl.{ ApplicationManagerSettings, SimpleConnectedApplicationManager }
import org.totalgrid.reef.app._

/**
 * Base class for bundles that expose an application that will be registering and heartbeating
 * with reef. It is only necessary to implement stopApplication() if you need to unregister something
 * outside of the applications, they will be automatically stopped.
 */
abstract class ConnectedApplicationBundleActivator extends ExecutorBundleActivator with Logging {

  protected var manager = Option.empty[ConnectionCloseManagerEx]
  protected var appManager = Option.empty[SimpleConnectedApplicationManager]

  def addApplication(
    context: BundleContext,
    connectionManager: ConnectionProvider,
    appManager: ConnectedApplicationManager,
    executor: Executor)

  def stopApplication() = {}

  def bundleName = this.getClass.getSimpleName

  def start(context: BundleContext, exe: Executor) {

    logger.info("Starting " + bundleName + " bundle..")

    val properties = OsgiConfigReader.load(context, List("org.totalgrid.reef.amqp", "org.totalgrid.reef.user", "org.totalgrid.reef.node"))
    val brokerOptions = new AmqpSettings(properties)
    val userSettings = new UserSettings(properties)
    val nodeSettings = new NodeSettings(properties)

    val applicationSettings = new ApplicationManagerSettings(userSettings, nodeSettings)

    manager = Some(new ConnectionCloseManagerEx(brokerOptions, exe))
    appManager = Some(new SimpleConnectedApplicationManager(exe, manager.get, applicationSettings))

    manager.foreach { _.start }
    appManager.foreach { _.start }

    addApplication(context, manager.get, appManager.get, exe)
  }

  def stop(context: BundleContext, exe: Executor) = {

    appManager.foreach { _.stop }

    manager.foreach { _.stop }

    logger.info("Stopped " + bundleName + " bundle..")
  }

}