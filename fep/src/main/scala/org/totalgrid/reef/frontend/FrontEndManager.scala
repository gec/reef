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
package org.totalgrid.reef.frontend

import org.totalgrid.reef.proto.Application.ApplicationConfig
import org.totalgrid.reef.proto.FEP.EndpointConnection
import org.totalgrid.reef.app.SubscriptionHandler
import com.weiglewilczek.slf4s.Logging
import org.totalgrid.reef.clientapi.exceptions.ReefServiceException

import net.agileautomata.executor4s._
import org.totalgrid.reef.util.Lifecycle

class FrontEndManager(
  client: FrontEndProviderServices,
  exe: Executor,
  connectionContext: SubscriptionHandler[EndpointConnection],
  appConfig: ApplicationConfig,
  protocolNames: List[String],
  retryms: Long)
    extends Lifecycle with Logging {

  private var delayedAnnounce = Option.empty[Cancelable]

  final override def afterStart() {
    announceAsFep()
  }

  final override def beforeStop() {

    delayedAnnounce.foreach { _.cancel }

    logger.info("Clearing connections")
    connectionContext.cancel()
  }

  private def announceAsFep() {

    try {
      val fep = client.registerApplicationAsFrontEnd(appConfig.getUuid, protocolNames).await
      logger.info("Registered application as FEP with id: " + fep.getUuid.getValue)
      val result = client.subscribeToEndpointConnectionsForFrontEnd(fep).await
      connectionContext.setSubscription(result)
    } catch {
      case rse: ReefServiceException =>
        logger.warn("Error getting endpoints to talk to: " + rse.toString, rse)
        delayedAnnounce = Some(exe.schedule(retryms.milliseconds) { announceAsFep })
    }
  }
}

