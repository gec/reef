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
package org.totalgrid.reef.entry

import org.osgi.framework._

import org.totalgrid.reef.messaging.AMQPProtoFactory
import org.totalgrid.reef.broker.qpid.QpidBrokerConnection

import org.totalgrid.reef.executor.{ ReactActorExecutor, LifecycleWrapper, Lifecycle, LifecycleManager }

import org.totalgrid.reef.frontend.{ FrontEndActor }
import org.totalgrid.reef.app.{ ApplicationEnroller, CoreApplicationComponents }
import org.totalgrid.reef.protocol.api.IProtocol
import org.totalgrid.reef.util.Logging
import org.totalgrid.reef.osgi.OsgiConfigReader

import com.weiglewilczek.scalamodules._

import org.totalgrid.reef.broker.BrokerProperties

class FepActivator extends BundleActivator with Logging {

  private var map = Map.empty[IProtocol, Lifecycle]
  private var amqp: Option[AMQPProtoFactory] = None
  private val manager = new LifecycleManager

  def start(context: BundleContext) {

    org.totalgrid.reef.executor.Executor.setupThreadPools

    amqp = Some(new AMQPProtoFactory with ReactActorExecutor {
      val broker = new QpidBrokerConnection(BrokerProperties.get(new OsgiConfigReader(context, "org.totalgrid.reef")))
    })

    manager.add(amqp.get)

    context watchServices withInterface[IProtocol] andHandle {
      case AddingService(p, _) => addProtocol(p)
      case ServiceRemoved(p, _) => removeProtocol(p)
    }

    manager.start
  }

  def stop(context: BundleContext) = manager.stop()

  private def addProtocol(p: IProtocol) = map.synchronized {
    map.get(p) match {
      case Some(x) => logger.info("Protocol already added: " + p.name)
      case None =>
        val enroller = new ApplicationEnroller(amqp.get, Some("FEP-" + p.name), List("FEP"), create(List(p), _)) with ReactActorExecutor
        map = map + (p -> enroller)
        manager.add(enroller)
    }
  }

  private def removeProtocol(p: IProtocol) = map.synchronized {
    map.get(p) match {
      case Some(lifecycle) =>
        map = map - p
        manager.remove(lifecycle)
      case None => logger.warn("Protocol not found: " + p.name)
    }
  }

  private def create(protocols: Seq[IProtocol], components: CoreApplicationComponents): Lifecycle = {

    // the actor does all the work of announcing the system, retrieving resources and starting/stopping
    // protocol masters in response to events
    val fepActor = new FrontEndActor(
      components.registry,
      protocols,
      components.logger,
      components.appConfig,
      FrontEndActor.retryms) with ReactActorExecutor

    new LifecycleWrapper(components.heartbeatActor :: fepActor :: Nil)
  }

}
