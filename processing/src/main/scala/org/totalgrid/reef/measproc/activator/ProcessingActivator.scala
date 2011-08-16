package org.totalgrid.reef.measproc.activator

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
import org.osgi.framework._

import org.totalgrid.reef.persistence.squeryl.SqlProperties
import org.totalgrid.reef.osgi.OsgiConfigReader
import org.totalgrid.reef.broker.BrokerProperties
import org.totalgrid.reef.messaging.AMQPProtoFactory
import org.totalgrid.reef.broker.qpid.QpidBrokerConnection
import org.totalgrid.reef.measurementstore.MeasurementStoreFinder
import org.totalgrid.reef.app.ApplicationEnroller
import org.totalgrid.reef.measproc.FullProcessor
import org.totalgrid.reef.executor.{ LifecycleManager, ReactActorExecutor, LifecycleWrapper }

class ProcessingActivator extends BundleActivator {

  var manager: Option[LifecycleManager] = None

  def start(context: BundleContext) {

    org.totalgrid.reef.executor.Executor.setupThreadPools

    val mgr = new LifecycleManager
    manager = Some(mgr)

    val brokerInfo = BrokerProperties.get(new OsgiConfigReader(context, "org.totalgrid.reef.amqp"))
    val dbInfo = SqlProperties.get(new OsgiConfigReader(context, "org.totalgrid.reef.sql"))
    val userSettings = ApplicationEnroller.getDefaultUserSettings
    val nodeSettings = ApplicationEnroller.getDefaultNodeSettings

    val amqp = new AMQPProtoFactory with ReactActorExecutor {
      val broker = new QpidBrokerConnection(brokerInfo)
    }
    mgr.add(amqp)

    val measExecutor = new ReactActorExecutor {}
    val measStore = MeasurementStoreFinder.getInstance(dbInfo, measExecutor, context)
    mgr.add(measExecutor)

    val enroller = new ApplicationEnroller(amqp, userSettings, nodeSettings,
      nodeSettings.getDefaultNodeName + "-meas_proc", List("Processing"),
      new FullProcessor(_, measStore)) with ReactActorExecutor
    mgr.add(enroller)

    mgr.start()
  }

  def stop(context: BundleContext) {
    manager.foreach(_.stop())
  }

}
