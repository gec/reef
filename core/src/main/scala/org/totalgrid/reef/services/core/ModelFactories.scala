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
package org.totalgrid.reef.services.core

import org.totalgrid.reef.services.ServiceEventPublishers

import org.totalgrid.reef.measurementstore.{ MeasurementStore, InMemoryMeasurementStore }

class ModelFactories(pubs: ServiceEventPublishers, summaries: SummaryPoints, cm: MeasurementStore = new InMemoryMeasurementStore) {
  val cmds = new CommandServiceModelFactory(pubs)

  val triggerSets = new TriggerSetServiceModelFactory(pubs)
  val accesses = new CommandAccessServiceModelFactory(pubs, cmds)
  val userRequests = new UserCommandRequestServiceModelFactory(pubs, cmds, accesses)

  val fepConn = new CommunicationEndpointConnectionModelFactory(pubs, cm)
  val measProcConn = new MeasurementProcessingConnectionModelFactory(pubs, fepConn)

  val fep = new FrontEndProcessorModelFactory(pubs, fepConn)
  val fepPort = new FrontEndPortModelFactory(pubs)

  val points = new PointServiceModelFactory(pubs)
  val overrides = new OverrideConfigModelFactory(pubs)

  val configFiles = new ConfigFileServiceModelFactory(pubs)
  val endpoints = new CommEndCfgServiceModelFactory(pubs, cmds, configFiles, points, measProcConn, fepConn, fepPort)

  val alarms = new AlarmServiceModelFactory(pubs, summaries)
  val eventConfig = new EventConfigServiceModelFactory(pubs)
  val events = new EventServiceModelFactory(pubs, eventConfig, alarms)

  val authTokens = new AuthTokenServiceModelFactory(pubs, events.model.createFromProto)

  val procStatus = new ProcessStatusServiceModelFactory(pubs, measProcConn, fepConn)
  val appConfig = new ApplicationConfigServiceModelFactory(pubs, procStatus)

}