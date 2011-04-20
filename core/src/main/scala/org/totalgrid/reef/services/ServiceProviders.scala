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
package org.totalgrid.reef.services

import org.totalgrid.reef.app.CoreApplicationComponents
import org.totalgrid.reef.measurementstore.{ MeasurementStore, RTDatabaseMetrics, HistorianMetrics }

import org.totalgrid.reef.services.core._
import org.totalgrid.reef.services.coordinators._
import org.totalgrid.reef.proto.ReefServicesList
import org.totalgrid.reef.messaging.SessionPool

import org.totalgrid.reef.messaging.serviceprovider.ServiceEventPublisherRegistry
import org.totalgrid.reef.services.core.util.HistoryTrimmer

/**
 * list of all of the service providers in the system
 */
class ServiceProviders(components: CoreApplicationComponents, cm: MeasurementStore) {

  val pubs = new ServiceEventPublisherRegistry(components.amqp, ReefServicesList)
  val summaries = new SummaryPointPublisher(components.amqp)
  val modelFac = new ModelFactories(pubs, summaries, cm)

  val wrappedDb = new RTDatabaseMetrics(cm, components.metricsPublisher.getStore("rtdatbase.rt"))
  val wrappedHistorian = new HistorianMetrics(cm, components.metricsPublisher.getStore("historian.hist"))

  val sessionPool = new SessionPool(components.registry)

  val services = List(

    new EntityService,
    new EntityEdgeService,
    new EntityAttributesService,

    // we do not lock out people without auth tokens from the authtoken service, otherwise we couldn't bootstrap
    new AuthTokenService(modelFac.authTokens),
    new AgentService(modelFac.agents),
    new PermissionSetService(modelFac.permissionSets),

    new CommandAccessService(modelFac.accesses),

    new UserCommandRequestService(modelFac.userRequests, sessionPool),

    new CommandService(modelFac.cmds),
    new CommunicationEndpointService(modelFac.endpoints),
    new ConfigFileService(modelFac.configFiles),

    new ProcessStatusService(modelFac.procStatus),
    new ApplicationConfigService(modelFac.appConfig),
    new FrontEndProcessorService(modelFac.fep),
    new MeasurementProcessingConnectionService(modelFac.measProcConn),
    new CommunicationEndpointConnectionService(modelFac.fepConn),
    new FrontEndPortService(modelFac.fepPort),
    new PointService(modelFac.points),
    new OverrideConfigService(modelFac.overrides),
    new TriggerSetService(modelFac.triggerSets),

    new MeasurementBatchService(components.amqp),
    new MeasurementHistoryService(wrappedHistorian, pubs),
    new MeasurementSnapshotService(wrappedDb, pubs),
    new EventConfigService(modelFac.eventConfig),
    new EventQueryService(modelFac.events, pubs),
    new EventService(modelFac.events),
    new AlarmService(modelFac.alarms),
    new AlarmQueryService(pubs))

  val coordinators = List(
    new ProcessStatusCoordinator(modelFac.procStatus),
    new HistoryTrimmer(cm, 3 * 60 * 60 * 1000, 10000000), // run every 3 hours
    //serviceContainer.addCoordinator(new PointAbnormalsThunker(modelFac.points, summaries))
    //serviceContainer.addCoordinator(new AlarmSummaryInitializer(modelFac.alarms, summaries))
    new EventStreamThunker(modelFac.events, List("raw_events")))

}
