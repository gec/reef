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

import org.totalgrid.reef.protocol.api.Protocol._
import org.totalgrid.reef.protocol.api._
import org.totalgrid.reef.client.service.proto.{ FEP, Model }
import org.totalgrid.reef.client.sapi.client.rest.Client

class NullProtocol(protocolName: String = "NullProtocol") extends Protocol {

  final override def name = protocolName

  override def requiresChannel = false

  override def addEndpoint(endpoint: String,
    channelName: String,
    config: List[Model.ConfigFile],
    batchPublisher: BatchPublisher,
    endpointPublisher: EndpointPublisher,
    client: Client): CommandHandler = NullCommandHandler

  override def removeEndpoint(endpoint: String) = {}

  override def addChannel(channel: FEP.CommChannel, channelPublisher: ChannelPublisher, client: Client) = {}

  override def removeChannel(channel: String) = {}

}