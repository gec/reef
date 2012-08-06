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

import org.totalgrid.reef.services.core.util.HistoryTrimmer

import org.totalgrid.reef.services.metrics._
import org.totalgrid.reef.services.authz.AuthzService
import org.totalgrid.reef.services.framework._
import org.totalgrid.reef.services.settings.ServiceOptions

import org.totalgrid.reef.client.Connection
import org.totalgrid.reef.persistence.squeryl.DbConnection
import net.agileautomata.executor4s.Executor
import org.totalgrid.reef.jmx.MetricsManager

/**
 * list of all of the service providers in the system
 */
class ServiceProviders(
    nodeName: String,
    dbConnection: DbConnection,
    connection: Connection,
    cm: MeasurementStore,
    serviceConfiguration: ServiceOptions,
    authzService: AuthzService,
    authToken: String,
    executor: Executor) {

  private val metricsMgr = MetricsManager("org.totalgrid.reef.services", nodeName)

  private val authService = new AuthzServiceMetricsWrapper(authzService, metricsMgr.metrics("Auth"))

  private val eventPublisher = new LocalSystemEventSink(executor)
  private val dependencies = new ServiceDependencies(dbConnection, connection, connection.getServiceRegistration.getEventPublisher, cm, eventPublisher, authToken, authService)

  private val contextSource = new DependenciesSource(dependencies)

  private val modelFac = new ModelFactories(cm, contextSource)

  // we have to fill in the event model after constructing the event service to break the circular
  // dependency on ServiceDepenedencies, should clear up once we OSGI the services
  eventPublisher.setComponents(dbConnection, modelFac.events, contextSource)

  private val wrappedDb = new RTDatabaseMetrics(cm, metricsMgr.metrics("RTDatabase"))
  private val wrappedHistorian = new HistorianMetrics(cm, metricsMgr.metrics("Historian"))

  private val serviceProviders: List[ServiceEntryPoint[_ <: AnyRef]] = List(
    new SimpleAuthRequestService(modelFac.authTokens),
    new AuthTokenService(modelFac.authTokens),
    new EntityEdgeService(modelFac.edges),
    new EntityService(modelFac.entities),
    new EntityAttributesService,
    new EntityAttributeService(modelFac.attributes),
    new MeasurementHistoryService(wrappedHistorian),
    new MeasurementSnapshotService(wrappedDb),
    new MeasurementStatisticsService(wrappedHistorian),
    new EventQueryService,
    new AlarmQueryService,
    new MeasurementBatchService,
    new AgentService(modelFac.agents),
    new PermissionSetService(modelFac.permissionSets),

    new CommandLockService(modelFac.accesses),

    new UserCommandRequestService(modelFac.userRequests),

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
    new CalculationConfigService(modelFac.calculations),
    new EventService(modelFac.events),
    new AlarmService(modelFac.alarms),
    new AuthFilterService,
    new CommandHandlerBindingService,
    new MeasurementStreamBindingService)

  private val metrics = new MetricsServiceWrapper(metricsMgr.metrics("Services"), serviceConfiguration)
  private val metricWrapped = serviceProviders.map { s => metrics.instrumentCallback(s) }

  private val allServices = (new BatchServiceRequestService(metricWrapped) :: metricWrapped)
  val services = allServices.map { s => new ServiceMiddleware(contextSource, s) }

  val coordinators = List(
    new ProcessStatusCoordinator(modelFac.procStatus, contextSource),
    new HistoryTrimmer(cm, serviceConfiguration.trimPeriodMinutes * 1000 * 60, serviceConfiguration.maxMeasurements))

  metricsMgr.register()

  def close() {
    coordinators.foreach { _.stopProcess() }
    metricsMgr.unregister()
  }

}
