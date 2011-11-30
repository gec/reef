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
package org.totalgrid.reef.client.sapi.rpc.impl

import org.totalgrid.reef.client.sapi.rpc._
import org.totalgrid.reef.clientapi.sapi.client.rest.{ RpcProvider, Client }
import org.totalgrid.reef.clientapi.sapi.client.rpc.framework.ApiBase

/**
 * "Super" implementation of all of the service interfaces
 */
trait AllScadaServiceImpl
  extends AllScadaService
  with EntityServiceImpl
  with ConfigFileServiceImpl
  with MeasurementServiceImpl
  with MeasurementOverrideServiceImpl
  with EventServiceImpl
  with EventPublishingServiceImpl
  with EventConfigServiceImpl
  with CommandServiceImpl
  with PointServiceImpl
  with AlarmServiceImpl
  with AgentServiceImpl
  with EndpointServiceImpl
  with ApplicationServiceImpl
  with CommunicationChannelServiceImpl

class AllScadaServiceWrapper(client: Client) extends ApiBase(client) with AllScadaServiceImpl

object AllScadaServiceImpl {
  val serviceInfo = RpcProvider(new AllScadaServiceWrapper(_),
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
      classOf[CommunicationChannelService]))
}