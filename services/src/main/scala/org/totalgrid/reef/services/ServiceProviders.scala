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
package org.totalgrid.reef.services

import org.totalgrid.reef.measurementstore.{ MeasurementStore, RTDatabaseMetrics, HistorianMetrics }

import org.totalgrid.reef.services.core._
import org.totalgrid.reef.services.coordinators._
import org.totalgrid.reef.api.proto.ReefServicesList
import org.totalgrid.reef.messaging.BasicSessionPool

import org.totalgrid.reef.messaging.serviceprovider.ServiceEventPublisherRegistry
import org.totalgrid.reef.services.core.util.HistoryTrimmer

import org.totalgrid.reef.api.sapi.service.AsyncService
import org.totalgrid.reef.api.sapi.auth.AuthService
import org.totalgrid.reef.executor.Executor
import org.totalgrid.reef.services.framework._

/**
 * list of all of the service providers in the system
 */
class ServiceProviders(components: CoreApplicationComponents, cm: MeasurementStore, serviceConfiguration: ServiceOptions, authzService: AuthService, coordinatorExecutor: Executor) {

  private val pubs = new ServiceEventPublisherRegistry(components.amqp, ReefServicesList)
  private val summaries = new SummaryPointPublisher(components.amqp)
  private val eventPublisher = new LocalSystemEventSink
  private val dependencies = ServiceDependencies(pubs, summaries, cm, eventPublisher, coordinatorExecutor)

  private val contextSource = new DependenciesSource(dependencies)

  private val modelFac = new ModelFactories(dependencies, contextSource)

  // we have to fill in the event model after constructing the event service to break the circular
  // dependency on ServiceDepenedencies, should clear up once we OSGI the services
  eventPublisher.setComponents(modelFac.events, contextSource)

  private val wrappedDb = new RTDatabaseMetrics(cm, components.metricsPublisher.getStore("rtdatbase.rt"))
  private val wrappedHistorian = new HistorianMetrics(cm, components.metricsPublisher.getStore("historian.hist"))

  private val sessionPool = new BasicSessionPool(components.registry)

  private val authzMetrics = {
    val hooks = new RestAuthzMetrics("")
    if (serviceConfiguration.metrics) {
      hooks.setHookSource(components.metricsPublisher.getStore("all"))
    }
    hooks
  }

  private val unauthorizedServices: List[ServiceEntryPoint[_ <: AnyRef]] = new AuthTokenService(modelFac.authTokens) :: Nil

  private val restAuthorizedServices: List[AsyncService[_]] = List(
    new EntityService,
    new EntityEdgeService,
    new EntityAttributesService).map(s => new RestAuthzWrapper(s, authzMetrics, authzService))

  private val crudAuthorizedServices: List[ServiceEntryPoint[_ <: AnyRef] with HasAuthService] = List(
    new MeasurementHistoryService(wrappedHistorian),
    new MeasurementSnapshotService(wrappedDb),
    new EventQueryService,
    new AlarmQueryService,
    new MeasurementBatchService(sessionPool),
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

    new EventConfigService(modelFac.eventConfig),

    new EventService(modelFac.events),
    new AlarmService(modelFac.alarms))

  crudAuthorizedServices.foreach(s => s.authService = authzService)

  val rawServices = unauthorizedServices ::: crudAuthorizedServices
  val services = rawServices.map { s => new ServiceMiddleware(contextSource, s) } ::: restAuthorizedServices

  val coordinators = List(
    new ProcessStatusCoordinator(modelFac.procStatus, contextSource),
    new HistoryTrimmer(cm, serviceConfiguration.trimPeriodMinutes * 1000 * 60, serviceConfiguration.maxMeasurements))

}
