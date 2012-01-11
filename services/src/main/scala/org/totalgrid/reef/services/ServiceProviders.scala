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

import org.totalgrid.reef.services.authz.{ AuthServiceMetricsWrapper, AuthService }
import org.totalgrid.reef.services.framework._

import org.totalgrid.reef.client.sapi.client.rest.Connection
import org.totalgrid.reef.metrics.IMetricsSink
import org.totalgrid.reef.services.metrics.MetricsServiceWrapper
import org.totalgrid.reef.persistence.squeryl.DbConnection

/**
 * list of all of the service providers in the system
 */
class ServiceProviders(
    dbConnection: DbConnection,
    connection: Connection,
    cm: MeasurementStore,
    serviceConfiguration: ServiceOptions,
    authzService: AuthService,
    metricsPublisher: IMetricsSink,
    authToken: String) {

  private val eventPublisher = new LocalSystemEventSink
  private val dependencies = new ServiceDependencies(dbConnection, connection, connection, cm, eventPublisher, authToken)

  private val contextSource = new DependenciesSource(dependencies)

  private val modelFac = new ModelFactories(cm, contextSource)

  // we have to fill in the event model after constructing the event service to break the circular
  // dependency on ServiceDepenedencies, should clear up once we OSGI the services
  eventPublisher.setComponents(dbConnection, modelFac.events, contextSource)

  private val wrappedDb = new RTDatabaseMetrics(cm, metricsPublisher.getStore("rtdatbase.rt"))
  private val wrappedHistorian = new HistorianMetrics(cm, metricsPublisher.getStore("historian.hist"))

  // TODO: AuthTokenService can probably be authed service now
  private val unauthorizedServices: List[ServiceEntryPoint[_ <: AnyRef]] = List(
    new SimpleAuthRequestService(modelFac.authTokens),
    new AuthTokenService(modelFac.authTokens))

  private var crudAuthorizedServices: List[ServiceEntryPoint[_ <: AnyRef] with HasAuthService] = List(
    new EntityEdgeService(modelFac.edges),
    new EntityService(modelFac.entities),
    new EntityAttributesService,
    new MeasurementHistoryService(wrappedHistorian),
    new MeasurementSnapshotService(wrappedDb),
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

    new EventService(modelFac.events),
    new AlarmService(modelFac.alarms))

  crudAuthorizedServices ::= new BatchServiceRequestService(unauthorizedServices ::: crudAuthorizedServices)

  val authService = new AuthServiceMetricsWrapper(authzService, metricsPublisher.getStore("services.auth"))
  crudAuthorizedServices.foreach(s => s.authService = authService)

  val allServices = (unauthorizedServices ::: crudAuthorizedServices)

  val metrics = new MetricsServiceWrapper(metricsPublisher, serviceConfiguration)
  val metricWrapped = allServices.map { s => metrics.instrumentCallback(s) }
  val services = metricWrapped.map { s => new ServiceMiddleware(contextSource, s) }

  val coordinators = List(
    new ProcessStatusCoordinator(modelFac.procStatus, contextSource),
    new HistoryTrimmer(cm, serviceConfiguration.trimPeriodMinutes * 1000 * 60, serviceConfiguration.maxMeasurements))

}
