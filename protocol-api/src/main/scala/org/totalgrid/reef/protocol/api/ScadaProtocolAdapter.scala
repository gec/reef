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
package org.totalgrid.reef.protocol.api

import org.totalgrid.reef.client.service.proto.Measurements.MeasurementBatch
import org.totalgrid.reef.client.Client
import org.totalgrid.reef.client.service.proto.{ FEP, Model }
import org.totalgrid.reef.client.service.proto.FEP.{ CommChannel, EndpointConnection }
import org.totalgrid.reef.protocol.api.scada.{ Resources, ProtocolAdapter => ScadaProtocol }

import collection.JavaConversions._

object ScadaProtocolAdapter {

  def createResources(
    channel: Publisher[CommChannel.State],
    endpoint: Publisher[EndpointConnection.State],
    batch: Publisher[MeasurementBatch]): Resources =

    new Resources {

      def getEndpointStatePublisher(): Publisher[EndpointConnection.State] = endpoint

      def getMeasurementBatchPublisher(): Publisher[MeasurementBatch] = batch

      def getChannelStatePublisher(): Publisher[CommChannel.State] = channel
    }
}

class ScadaProtocolAdapter(scada: ScadaProtocol) extends Protocol {

  case class ChannelRecord(channel: FEP.CommChannel, publisher: Publisher[CommChannel.State])

  val channelMap = collection.mutable.Map.empty[String, ChannelRecord]
  val endpointMap = collection.mutable.Map.empty[String, String]

  def name: String = scada.name()

  def addEndpoint(endpoint: String, channelName: String, config: List[Model.ConfigFile], batchPublisher: Publisher[MeasurementBatch],
    endpointPublisher: Publisher[EndpointConnection.State], client: Client): CommandHandler = {

    channelMap.get(channelName) match {
      case Some(r) =>
        val cmdHandler = scada.addEndpoint(endpoint, r.channel, config, ScadaProtocolAdapter.createResources(r.publisher, endpointPublisher, batchPublisher))
        endpointMap += endpoint -> channelName
        cmdHandler
      case None =>
        throw new Exception("A channel by that name was not previously added to the adapter: " + channelName)
    }
  }

  def removeEndpoint(endpoint: String): Unit = endpointMap.get(endpoint) match {
    case Some(ep) =>
      scada.removeEndpoint(endpoint)
      endpointMap.remove(endpoint)
    case None =>
      throw new Exception("A endpoint by that name does not exist: " + endpoint)
  }

  def addChannel(channel: FEP.CommChannel, channelPublisher: Publisher[CommChannel.State], client: Client): Unit = channelMap.get(channel.getName) match {
    case Some(x) => throw new Exception("Channel already exists: " + channel.getName)
    case None => channelMap += channel.getName -> ChannelRecord(channel, channelPublisher)
  }

  def removeChannel(channel: String): Unit = channelMap.get(channel) match {
    case Some(ch) =>
      val toRemove = endpointMap.filter { case (ep, ch) => ch == channel }
      toRemove.foreach { case (ep, ch) => removeEndpoint(ep) }
    case None => throw new Exception("Channel by that name doesn't exist: " + channel)
  }

}
