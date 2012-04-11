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
package org.totalgrid.reef.protocol.api.impl

import org.totalgrid.reef.protocol.api.ProtocolResources
import org.totalgrid.reef.client.service.proto.FEP.{ CommChannel, EndpointConnection }
import java.util.List
import org.totalgrid.reef.client.service.proto.Measurements.Measurement
import org.totalgrid.reef.client.service.proto.Model.ConfigFile

import scala.collection.JavaConversions._
import org.totalgrid.reef.client.{ AddressableDestination, Client }
import org.totalgrid.reef.client.service.{ CommunicationChannelService, EndpointService, MeasurementService }

class ProtocolEndpointResources(client: Client, endpointConnection: EndpointConnection) extends ProtocolResources {

  private val mimeMap = endpointConnection.getEndpoint.getConfigFilesList.map(cfg => (cfg.getMimeType -> cfg)).toMap

  private val routingKey = endpointConnection.getRouting.getServiceRoutingKey;

  private val channel: Option[CommChannel] = if (endpointConnection.getEndpoint.hasChannel) Some(endpointConnection.getEndpoint.getChannel) else None

  def getClient: Client = client

  def getEndpointConnection: EndpointConnection = endpointConnection

  def getEndpointName: String = endpointConnection.getEndpoint.getName

  def getConfigFile(mimeType: String): ConfigFile = mimeMap.get(mimeType) getOrElse null

  def publishMeasurements(measurementList: List[Measurement]) {
    val service = client.getService(classOf[MeasurementService])
    service.publishMeasurements(measurementList, new AddressableDestination(routingKey))
  }

  def setCommsState(state: EndpointConnection.State) {
    val service = client.getService(classOf[EndpointService])
    service.alterEndpointConnectionState(endpointConnection.getId, state)
  }

  def hasCommChannel: Boolean = channel.isDefined

  def getCommChannel: CommChannel = channel.getOrElse(null)

  def setChannelState(state: CommChannel.State) {
    channel.foreach { ch =>
      val service = client.getService(classOf[CommunicationChannelService])
      service.alterCommunicationChannelState(ch.getUuid, state)
    }
  }
}
