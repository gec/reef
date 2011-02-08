/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Green Energy Corp licenses this file
 * to you under the GNU Affero General Public License Version 3.0
 * (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.app

import org.totalgrid.reef.event.BusTiedEventLogPublisher

import org.totalgrid.reef.procstatus.ProcessHeartbeatActor
import org.totalgrid.reef.metrics.{ NonOpMetricPublisher, NonOperationalDataPublisher }

import org.totalgrid.reef.proto.Application.ApplicationConfig
import org.totalgrid.reef.reactor.{ ReactActor, PeriodicReactor }
import org.totalgrid.reef.messaging.{ AMQPProtoFactory, AMQPProtoRegistry, ReefServicesList }

import org.totalgrid.reef.protoapi.RequestEnv

/**
 * wraps up all of the common/core components used by bus enabled applications, most of these components get a key
 * part of their setup details from the app config service.
 */
class CoreApplicationComponents(
    /// the interface to the bus
    val amqp: AMQPProtoFactory,
    /// the current appConfig, contains the instanceName and userName
    val appConfig: ApplicationConfig,
    /// authToken we are running the application as
    val defaultEnv: RequestEnv) {

  // registry is recreated here so we get service registry from bus if necessary 
  val registry = new AMQPProtoRegistry(amqp, 5000, ReefServicesList, Some(defaultEnv))

  /// heartbeatActor sends regular updates to the system to let it know we are still running, if this process
  /// dies the system can notice quickly and recover. Its important that the client start this actor and stop
  /// it cleanly before the amqp service is killed, that allows a nice clean shutdown
  val heartbeatActor = new ProcessHeartbeatActor(amqp, appConfig.getHeartbeatCfg) with ReactActor

  /// publisher used to write non-operational points into the system
  val nonOpPub = new NonOperationalDataPublisher(amqp.publish(appConfig.getStreamCfg.getNonopDest, { x: Any => ("") }))

  /// log and event sink, should also be registered as slf4j backend
  val logger = new BusTiedEventLogPublisher(amqp, appConfig.getCapabilites(0), appConfig.getStreamCfg.getEventsDest, appConfig.getStreamCfg.getLogsDest)

  /// specialized publisher for publishing application metrics
  val metricsPublisher = new NonOpMetricPublisher(amqp.publish(appConfig.getStreamCfg.getNonopDest, { x: Any => ("") }))

  /// publishes the metrics values on a fixed schedule
  val nonopPublisher = new PeriodicReactor(15000, metricsPublisher.publishAll _) with ReactActor

}
