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

import org.totalgrid.reef.proto.{ FEP, Commands, Measurements, Model }
import Measurements.MeasurementBatch
import FEP.CommChannel
import org.totalgrid.reef.proto.Model.ConfigFile
import com.weiglewilczek.slf4s.Logging
import org.totalgrid.reef.clientapi.sapi.client.rest.Client

trait Publisher[A] {
  /**
   * @param value Value that will be updated
   */
  def publish(value: A): Unit
}

object Protocol {

  def find(files: List[Model.ConfigFile], mimetype: String): Model.ConfigFile =
    {
      files.find {
        _.getMimeType == mimetype
      }.getOrElse {
        throw new Exception("Missing file w/ mime-type: " + mimetype)
      }
    }

  type BatchPublisher = Publisher[MeasurementBatch]
  type EndpointPublisher = Publisher[FEP.EndpointConnection.State]
  type ChannelPublisher = Publisher[CommChannel.State]
  type ResponsePublisher = Publisher[Commands.CommandStatus]
}

trait CommandHandler {
  def issue(cmd: Commands.CommandRequest, publisher: Protocol.ResponsePublisher)
}

trait NullPublisher[A] extends Publisher[A] {
  def publish(value: A) = {}
}

case object NullBatchPublisher extends NullPublisher[MeasurementBatch]

case object NullEndpointPublisher extends NullPublisher[FEP.EndpointConnection.State]

case object NullChannelPublisher extends NullPublisher[CommChannel.State]

case object NullCommandHandler extends CommandHandler {
  def issue(cmd: Commands.CommandRequest, publisher: Protocol.ResponsePublisher) =
    {}
}

import Protocol._

trait Protocol {

  /**
   * @return Unique name, i.e. 'dnp3'
   */
  def name: String

  def requiresChannel: Boolean

  def addEndpoint(endpoint: String, channelName: String, config: List[Model.ConfigFile], batchPublisher: BatchPublisher,
    endpointPublisher: EndpointPublisher, client: Client): CommandHandler

  def removeEndpoint(endpoint: String): Unit

  def addChannel(channel: FEP.CommChannel, channelPublisher: ChannelPublisher, client: Client): Unit

  def removeChannel(channel: String): Unit

}

trait ChannelIgnoringProtocol extends Protocol {
  def addChannel(channel: FEP.CommChannel, channelPublisher: ChannelPublisher, client: Client): Unit =
    {}

  def removeChannel(channel: String): Unit =
    {}
}

trait LoggingProtocol extends Protocol with Logging {

  abstract override def addChannel(channel: CommChannel, channelPublisher: Protocol.ChannelPublisher, client: Client) {
    logger.info("protocol: " + name + ": adding channel: " + channel + ", channelPublisher: " + channelPublisher)
    super.addChannel(channel, channelPublisher, client)
  }

  abstract override def removeChannel(channel: String) {
    logger.info("protocol: " + name + ": remove channel: " + channel)
    super.removeChannel(channel)
  }

  abstract override def addEndpoint(endpointName: String, channelName: String, config: List[ConfigFile], batchPublisher: Protocol.BatchPublisher,
    endpointPublisher: Protocol.EndpointPublisher, client: Client) = {
    logger.info("protocol: " + name + ": adding endpoint: " + endpointName + ", channelName: " + channelName);
    super.addEndpoint(endpointName, channelName, config, batchPublisher, endpointPublisher, client)
  }

  abstract override def removeEndpoint(endpoint: String) {
    logger.info("protocol: " + name + ": remove endpoint: " + endpoint)
    super.removeEndpoint(endpoint)
  }

}