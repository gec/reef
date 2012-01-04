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
package org.totalgrid.reef.protocol.api.mock

import org.totalgrid.reef.client.service.proto.{ FEP, Model, Commands }

import org.totalgrid.reef.client.sapi.client.rest.Client
import org.totalgrid.reef.client.service.proto.Measurements.MeasurementBatch
import org.totalgrid.reef.protocol.api.{ Publisher, Protocol, CommandHandler }
import org.totalgrid.reef.client.service.proto.FEP.{ CommChannel, EndpointConnection }

object RecordingProtocol {

  trait Action
  case class AddChannel(name: String) extends Action
  case class RemoveChannel(name: String) extends Action
  case class AddEndpoint(endpoint: String, port: String, config: List[Model.ConfigFile]) extends Action
  case class RemoveEndpoint(endpoint: String) extends Action
  case class Command(request: Commands.CommandRequest, rsp: Publisher[Commands.CommandStatus]) extends Action

}

trait RecordingProtocol extends Protocol {

  import RecordingProtocol._

  val queue = new scala.collection.mutable.Queue[Action]

  def next(): Option[Action] = if (queue.isEmpty) None else Some(queue.dequeue())

  abstract override def addChannel(channel: FEP.CommChannel, publisher: Publisher[CommChannel.State], client: Client): Unit = {
    queue.enqueue(AddChannel(channel.getName))
    super.addChannel(channel, publisher, client)
  }

  abstract override def removeChannel(channel: String): Unit = {
    queue.enqueue(RemoveChannel(channel))
    super.removeChannel(channel)
  }

  abstract override def addEndpoint(
    endpoint: String,
    channel: String,
    config: List[Model.ConfigFile],
    batch: Publisher[MeasurementBatch],
    epPublisher: Publisher[EndpointConnection.State],
    client: Client): CommandHandler = {
    queue.enqueue(AddEndpoint(endpoint, channel, config))
    val handler = super.addEndpoint(endpoint, channel, config, batch, epPublisher, client)
    new CommandHandler {
      def issue(request: Commands.CommandRequest, rspPublisher: Publisher[Commands.CommandStatus]) = {
        queue.enqueue(Command(request, rspPublisher))
        handler.issue(request, rspPublisher)
      }
    }
  }

  abstract override def removeEndpoint(endpoint: String): Unit = {
    queue.enqueue(RemoveEndpoint(endpoint))
    super.removeEndpoint(endpoint)
  }
}