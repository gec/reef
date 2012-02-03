package org.totalgrid.reef.metrics.service.activator

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

import com.weiglewilczek.slf4s.Logging
import org.totalgrid.reef.osgi.{ OsgiConfigReader, ExecutorBundleActivator }
import org.osgi.framework.BundleContext
import org.totalgrid.reef.app.ConnectionCloseManagerEx
import net.agileautomata.executor4s.Executor
import org.totalgrid.reef.client.settings.{ NodeSettings, UserSettings, AmqpSettings }

class MetricsActivator extends ExecutorBundleActivator with Logging {

  private var manager = Option.empty[ConnectionCloseManagerEx]

  protected def start(context: BundleContext, executor: Executor) {
    logger.info("Starting Service bundle..")

    val brokerConfig = new AmqpSettings(OsgiConfigReader(context, "org.totalgrid.reef.amqp").getProperties)
    val userSettings = new UserSettings(OsgiConfigReader(context, "org.totalgrid.reef.user").getProperties)
    val nodeSettings = new NodeSettings(OsgiConfigReader(context, "org.totalgrid.reef.node").getProperties)

    manager = Some(new ConnectionCloseManagerEx(brokerConfig, executor))

    manager.get.addConsumer(new MetricsConnection(userSettings, nodeSettings))

    manager.get.start()
  }

  protected def stop(context: BundleContext, executor: Executor) {

    manager.foreach(_.stop())

    logger.info("Stopped Service bundle..")
  }
}