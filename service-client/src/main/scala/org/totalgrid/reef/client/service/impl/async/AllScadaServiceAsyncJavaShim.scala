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
package org.totalgrid.reef.client.service.impl.async

import org.totalgrid.reef.client.SubscriptionCreationListener

import org.totalgrid.reef.client.service.async._
import org.totalgrid.reef.client.service.async.impl._
import org.totalgrid.reef.client.sapi.rpc.{ AllScadaService => ScalaAllScadaService }
import org.totalgrid.reef.client.sapi.rpc.impl.AllScadaServiceWrapper
import org.totalgrid.reef.client.sapi.client.rest.{ RpcProvider }
import org.totalgrid.reef.client.Client

trait AllScadaServiceAsyncJavaShim
    extends AllScadaServiceAsync
    with EntityServiceAsyncJavaShim
    with ConfigFileServiceAsyncJavaShim
    with MeasurementServiceAsyncJavaShim
    with MeasurementOverrideServiceAsyncJavaShim
    with EventServiceAsyncJavaShim
    with EventPublishingServiceAsyncJavaShim
    with EventConfigServiceAsyncJavaShim
    with CommandServiceAsyncJavaShim
    with PointServiceAsyncJavaShim
    with AlarmServiceAsyncJavaShim
    with AgentServiceAsyncJavaShim
    with EndpointServiceAsyncJavaShim
    with ApplicationServiceAsyncJavaShim
    with CommunicationChannelServiceAsyncJavaShim
    with CalculationServiceAsyncJavaShim {

  def service: ScalaAllScadaService
}

final class AllScadaServiceAsyncJavaShimWrapper(client: Client) extends AllScadaServiceAsyncJavaShim {

  private val srv = new AllScadaServiceWrapper(client)

  override def service = srv

  override def addSubscriptionCreationListener(listener: SubscriptionCreationListener) = client.addSubscriptionCreationListener(listener)
  override def removeSubscriptionCreationListener(listener: SubscriptionCreationListener) = client.removeSubscriptionCreationListener(listener)
}

object AllScadaServiceAsyncJavaShimServiceList {
  def getServiceInfo = RpcProvider(new AllScadaServiceAsyncJavaShimWrapper(_),
    List(
      classOf[AllScadaServiceAsync],
      classOf[EntityServiceAsync],
      classOf[ConfigFileServiceAsync],
      classOf[MeasurementServiceAsync],
      classOf[MeasurementOverrideServiceAsync],
      classOf[EventServiceAsync],
      classOf[EventPublishingServiceAsync],
      classOf[EventConfigServiceAsync],
      classOf[CommandServiceAsync],
      classOf[PointServiceAsync],
      classOf[AlarmServiceAsync],
      classOf[AgentServiceAsync],
      classOf[EndpointServiceAsync],
      classOf[ApplicationServiceAsync],
      classOf[CommunicationChannelServiceAsync]))

}