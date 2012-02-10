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
package org.totalgrid.reef.client.service.impl

import org.totalgrid.reef.client.SubscriptionCreationListener

import org.totalgrid.reef.client.service._
import org.totalgrid.reef.client.sapi.rpc.{ AllScadaService => ScalaAllScadaService }
import org.totalgrid.reef.client.sapi.rpc.impl.AllScadaServiceWrapper
import org.totalgrid.reef.client.sapi.client.rest.{ RpcProvider, Client }

trait AllScadaServiceJavaShim
    extends AllScadaService
    with EntityServiceJavaShim
    with ConfigFileServiceJavaShim
    with MeasurementServiceJavaShim
    with MeasurementOverrideServiceJavaShim
    with EventServiceJavaShim
    with EventPublishingServiceJavaShim
    with EventConfigServiceJavaShim
    with CommandServiceJavaShim
    with PointServiceJavaShim
    with AlarmServiceJavaShim
    with AgentServiceJavaShim
    with EndpointServiceJavaShim
    with ApplicationServiceJavaShim
    with CommunicationChannelServiceJavaShim {

  def service: ScalaAllScadaService
}

final class AllScadaServiceJavaShimWrapper(client: Client) extends AllScadaServiceJavaShim {

  private val srv = new AllScadaServiceWrapper(client)

  override def service = srv

  override def addSubscriptionCreationListener(listener: SubscriptionCreationListener) = service.addSubscriptionCreationListener(listener)
  override def removeSubscriptionCreationListener(listener: SubscriptionCreationListener) = service.removeSubscriptionCreationListener(listener)
}

object AllScadaServiceJavaShimServiceList {
  def getServiceInfo = RpcProvider(new AllScadaServiceJavaShimWrapper(_),
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