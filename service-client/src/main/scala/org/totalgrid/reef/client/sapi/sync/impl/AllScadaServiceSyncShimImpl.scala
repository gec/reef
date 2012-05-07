/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.client.sapi.sync.impl

import org.totalgrid.reef.client.sapi.rpc.util.RpcProvider
import org.totalgrid.reef.client.sapi.sync._
import org.totalgrid.reef.client.sapi.rpc.{ AllScadaService => AsyncAllScadaService }
import org.totalgrid.reef.client.sapi.rpc.impl.AllScadaServiceWrapper
import org.totalgrid.reef.client.Client
import org.totalgrid.reef.client.operations.scl.ServiceOperationsProvider

trait AllScadaServiceSyncImpl
    extends AllScadaService
    with EntityServiceSyncShim
    with ConfigFileServiceSyncShim
    with MeasurementServiceSyncShim
    with MeasurementOverrideServiceSyncShim
    with EventServiceSyncShim
    with EventPublishingServiceSyncShim
    with EventConfigServiceSyncShim
    with CommandServiceSyncShim
    with PointServiceSyncShim
    with AlarmServiceSyncShim
    with AgentServiceSyncShim
    with EndpointServiceSyncShim
    with ApplicationServiceSyncShim
    with CommunicationChannelServiceSyncShim
    with CalculationServiceSyncShim
    with LoginServiceSyncShim {

  def service: AsyncAllScadaService
}

class AllScadaServiceSyncWrapper(client: Client) extends ServiceOperationsProvider(client) with AllScadaServiceSyncImpl {
  private val srv = new AllScadaServiceWrapper(client)

  override def service = srv
}

object AllScadaServiceSyncServiceList {
  def getServiceInfo = RpcProvider(new AllScadaServiceSyncWrapper(_),
    List(
      classOf[AllScadaService],
      classOf[EntityService],
      classOf[ConfigFileService],
      classOf[MeasurementService],
      classOf[MeasurementOverrideService],
      classOf[EventService],
      classOf[EventPublishingService],
      classOf[EventConfigService],
      classOf[CommandService],
      classOf[PointService],
      classOf[AlarmService],
      classOf[AgentService],
      classOf[EndpointService],
      classOf[ApplicationService],
      classOf[CommunicationChannelService],
      classOf[CalculationService],
      classOf[LoginService]))
}