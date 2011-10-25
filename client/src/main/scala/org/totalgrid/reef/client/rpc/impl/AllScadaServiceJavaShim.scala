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
package org.totalgrid.reef.client.rpc.impl

import org.totalgrid.reef.api.japi.client.SubscriptionCreationListener
import org.totalgrid.reef.client.rpc._
import org.totalgrid.reef.client.sapi.rpc.{ AllScadaService => ScalaAllScadaService }
import org.totalgrid.reef.client.sapi.rpc.impl.AllScadaServiceWrapper
import org.totalgrid.reef.api.sapi.client.rest.{ RpcProviderInfo, Client }

trait AllScadaServiceJavaShim
    extends AllScadaService
    with AuthTokenServiceJavaShim
    with EntityServiceJavaShim
    with ConfigFileServiceJavaShim
    with MeasurementServiceJavaShim
    with MeasurementOverrideServiceJavaShim
    with EventServiceJavaShim
    with EventCreationServiceJavaShim
    with EventConfigServiceJavaShim
    with CommandServiceJavaShim
    with PointServiceJavaShim
    with AlarmServiceJavaShim
    with AgentServiceJavaShim
    with EndpointManagementServiceJavaShim
    with ApplicationServiceJavaShim
    with CommunicationChannelServiceJavaShim {

  def service: ScalaAllScadaService
}

final class AllScadaServiceJavaShimWrapper(client: Client) extends AllScadaServiceJavaShim {

  private val srv = new AllScadaServiceWrapper(client)

  override def service = srv

  override def addSubscriptionCreationListener(listener: SubscriptionCreationListener) = service.addSubscriptionCreationListener(listener)
}

object AllScadaServiceJavaShim {
  val serviceInfo = new RpcProviderInfo({ c: Client => new AllScadaServiceJavaShimWrapper(c) },
    List(
      classOf[AllScadaService],
      classOf[AuthTokenService],
      classOf[EntityService],
      classOf[ConfigFileService],
      classOf[MeasurementService],
      classOf[MeasurementOverrideService],
      classOf[EventService],
      classOf[EventCreationService],
      classOf[EventConfigService],
      classOf[CommandService],
      classOf[PointService],
      classOf[AlarmService],
      classOf[AgentService],
      classOf[EndpointManagementService],
      classOf[ApplicationService],
      classOf[CommunicationChannelService]))

}