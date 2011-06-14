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
package org.totalgrid.reef.services.core

import org.totalgrid.reef.services.ServiceDependencies
import org.totalgrid.reef.services.coordinators.MeasurementStreamCoordinatorFactory

class ModelFactories(dependencies: ServiceDependencies = new ServiceDependencies) {

  val cmds = new CommandServiceModelFactory(dependencies.pubs)

  val triggerSets = new TriggerSetServiceModelFactory(dependencies.pubs)
  val accesses = new CommandAccessServiceModelFactory(dependencies.pubs, cmds)
  val userRequests = new UserCommandRequestServiceModelFactory(dependencies.pubs, cmds, accesses)

  val coordinator = new MeasurementStreamCoordinatorFactory(dependencies.pubs, dependencies.cm)

  val fepConn = new CommunicationEndpointConnectionModelFactory(dependencies.pubs, coordinator)
  val measProcConn = new MeasurementProcessingConnectionModelFactory(dependencies.pubs, coordinator)

  val fep = new FrontEndProcessorModelFactory(dependencies.pubs, coordinator)
  val fepPort = new FrontEndPortModelFactory(dependencies.pubs)

  val points = new PointServiceModelFactory(dependencies.pubs)
  val overrides = new OverrideConfigModelFactory(dependencies.pubs)

  val configFiles = new ConfigFileServiceModelFactory(dependencies.pubs)
  val endpoints = new CommEndCfgServiceModelFactory(dependencies.pubs, cmds, configFiles, points, fepPort, coordinator)

  val alarms = new AlarmServiceModelFactory(dependencies.pubs, dependencies.summaries)
  val eventConfig = new EventConfigServiceModelFactory(dependencies.pubs)
  val events = new EventServiceModelFactory(dependencies.pubs, eventConfig, alarms)

  val authTokens = new AuthTokenServiceModelFactory(dependencies.pubs, dependencies.eventSink.publishSystemEvent _)
  val agents = new AgentServiceModelFactory(dependencies.pubs)
  val permissionSets = new PermissionSetServiceModelFactory(dependencies.pubs)

  val procStatus = new ProcessStatusServiceModelFactory(dependencies.pubs, coordinator)
  val appConfig = new ApplicationConfigServiceModelFactory(dependencies.pubs, procStatus)

}