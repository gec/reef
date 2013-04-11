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
package org.totalgrid.reef.measproc

import com.typesafe.scalalogging.slf4j.Logging

import org.totalgrid.reef.client.service.proto.Application.ApplicationConfig
import org.totalgrid.reef.client.service.proto.Processing.MeasurementProcessingConnection
import org.totalgrid.reef.app.SubscriptionHandler
import org.totalgrid.reef.client.exception.ReefServiceException

import net.agileautomata.executor4s._
import org.totalgrid.reef.util.Lifecycle

/**
 *  Non-entry point meas processor setup
 */
class FullProcessor(
    client: MeasurementProcessorServices,
    connectionContext: SubscriptionHandler[MeasurementProcessingConnection],
    appConfig: ApplicationConfig,
    exe: Executor) extends Logging with Lifecycle {

  private var delayedAnnounce = Option.empty[Cancelable]

  final override def afterStart() {
    subscribeToStreams()
  }

  final override def beforeStop() {

    delayedAnnounce.foreach { _.cancel }

    logger.info("Clearing connections")
    connectionContext.cancel()
  }

  private def subscribeToStreams() {

    try {
      val result = client.subscribeToConnectionsForMeasurementProcessor(appConfig).await
      connectionContext.setSubscription(result)
    } catch {
      case rse: ReefServiceException =>
        logger.warn("Error subscribing to logical nodes to process: " + rse.toString, rse)
        delayedAnnounce = Some(exe.schedule(5000.milliseconds) { subscribeToStreams })
    }
  }
}

