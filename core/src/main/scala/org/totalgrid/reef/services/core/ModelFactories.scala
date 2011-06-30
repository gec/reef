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

  val accesses = new CommandAccessServiceModelFactory(dependencies)
  val userRequests = new UserCommandRequestServiceModelFactory(dependencies, accesses)
  val cmds = new CommandServiceModelFactory(dependencies, userRequests, accesses)
  accesses.setCommandsFactory(cmds)

  val coordinator = new MeasurementStreamCoordinatorFactory(dependencies)

  val fepConn = new CommunicationEndpointConnectionModelFactory(dependencies, coordinator)
  val measProcConn = new MeasurementProcessingConnectionModelFactory(dependencies, coordinator)

  val fep = new FrontEndProcessorModelFactory(dependencies, coordinator)
  val fepPort = new FrontEndPortModelFactory(dependencies)

  val overrides = new OverrideConfigModelFactory(dependencies)
  val triggerSets = new TriggerSetServiceModelFactory(dependencies)
  val points = new PointServiceModelFactory(dependencies, triggerSets, overrides)

  val configFiles = new ConfigFileServiceModelFactory(dependencies)
  val endpoints = new CommEndCfgServiceModelFactory(dependencies, cmds, configFiles, points, fepPort, coordinator)

  val alarms = new AlarmServiceModelFactory(dependencies)
  val eventConfig = new EventConfigServiceModelFactory(dependencies)
  val events = new EventServiceModelFactory(dependencies, eventConfig, alarms)

  val authTokens = new AuthTokenServiceModelFactory(dependencies)
  val agents = new AgentServiceModelFactory(dependencies)
  val permissionSets = new PermissionSetServiceModelFactory(dependencies)

  val procStatus = new ProcessStatusServiceModelFactory(dependencies, coordinator)
  val appConfig = new ApplicationConfigServiceModelFactory(dependencies, procStatus)

}