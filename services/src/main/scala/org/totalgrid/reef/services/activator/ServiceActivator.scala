package org.totalgrid.reef.services.activator

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

import org.totalgrid.reef.api.sapi.service.AsyncService

import org.totalgrid.reef.osgi.OsgiConfigReader

import com.weiglewilczek.scalamodules._
import org.totalgrid.reef.broker.api.BrokerProperties
import org.totalgrid.reef.persistence.squeryl.{ DbConnector, SqlProperties }
import org.totalgrid.reef.messaging.AMQPProtoFactory
import org.totalgrid.reef.broker.qpid.QpidBrokerConnection
import org.totalgrid.reef.services._
import org.totalgrid.reef.executor.{ LifecycleManager, ReactActorExecutor }
import org.totalgrid.reef.measurementstore.MeasurementStoreFinder
import org.totalgrid.reef.api.japi.client.{ NodeSettings, UserSettings }

class ServiceActivator extends BundleActivator {

  var manager: Option[LifecycleManager] = None

  def start(context: BundleContext) {

    org.totalgrid.reef.executor.Executor.setupThreadPools

    val mgr = new LifecycleManager
    manager = Some(mgr)

    val sql = SqlProperties.get(OsgiConfigReader(context, "org.totalgrid.reef.sql"))
    val brokerConfig = BrokerProperties.get(OsgiConfigReader(context, "org.totalgrid.reef.amqp"))
    val options = ServiceOptions.get(OsgiConfigReader(context, "org.totalgrid.reef.services"))
    val userSettings = new UserSettings(OsgiConfigReader(context, "org.totalgrid.reef.user").getProperties)
    val nodeSettings = new NodeSettings(OsgiConfigReader(context, "org.totalgrid.reef.node").getProperties)

    //val userSettings = ApplicationEnroller.getDefaultUserSettings
    //val nodeSettings = ApplicationEnroller.getDefaultNodeSettings

    val amqp = new AMQPProtoFactory with ReactActorExecutor {
      val broker = new QpidBrokerConnection(brokerConfig)
    }

    mgr.add(amqp)

    DbConnector.connect(sql, context)

    val components = ServiceBootstrap.bootstrapComponents(amqp, userSettings, nodeSettings)
    mgr.add(components.heartbeatActor)

    val metrics = new MetricsServiceWrapper(components, options)

    val measExecutor = new ReactActorExecutor {}
    mgr.add(measExecutor)
    val measStore = MeasurementStoreFinder.getInstance(sql, measExecutor, context)

    val coordinatorExecutor = new ReactActorExecutor {}
    mgr.add(coordinatorExecutor)

    val providers = new ServiceProviders(components, measStore, options, SqlAuthzService, coordinatorExecutor)

    val serviceContext = new ServiceContext(mgr, amqp, metrics)

    serviceContext.addCoordinator(providers.coordinators)

    val services = serviceContext.attachServices(providers.services)

    services.foreach { x =>
      context createService (x, "exchange" -> x.descriptor.id, interface[AsyncService[_]])
    }

    mgr.start()
  }

  def stop(context: BundleContext) {
    manager.foreach(_.stop())
  }

}

