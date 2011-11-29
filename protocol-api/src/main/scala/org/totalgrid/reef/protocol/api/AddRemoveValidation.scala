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

import org.totalgrid.reef.proto.{ FEP, Model }
import scala.collection.immutable

import com.weiglewilczek.slf4s.Logging
import org.totalgrid.reef.clientapi.sapi.client.rest.Client

trait AddRemoveValidation extends Protocol with Logging {

  import Protocol._

  case class Endpoint(name: String, channel: Option[FEP.CommChannel], config: List[Model.ConfigFile], publisher: EndpointPublisher) /// The issue function and the channel
  case class Channel(config: FEP.CommChannel, publisher: ChannelPublisher)

  // only mutable state is current assignment of these variables
  private var endpoints = immutable.Map.empty[String, Endpoint] /// maps uids to a Endpoint
  private var channels = immutable.Map.empty[String, Channel] /// maps uids to a Port

  abstract override def addChannel(p: FEP.CommChannel, publisher: ChannelPublisher, client: Client): Unit = {
    channels.get(p.getName) match {
      case None =>
        channels = channels + (p.getName -> Channel(p, publisher))
        super.addChannel(p, publisher, client)
      case Some(x) =>
        logger.info("Ignoring duplicate channel " + p)
    }
  }

  abstract override def addEndpoint(endpoint: String, channelName: String, config: List[Model.ConfigFile], batchPublisher: BatchPublisher, endpointPublisher: EndpointPublisher, client: Client): CommandHandler = {

    endpoints.get(endpoint) match {
      case Some(x) => throw new IllegalArgumentException("Endpoint already exists: " + endpoint)
      case None =>
        channels.get(channelName) match {
          case Some(p) =>
            endpoints += endpoint -> Endpoint(endpoint, Some(p.config), config, endpointPublisher)
            super.addEndpoint(endpoint, channelName, config, batchPublisher, endpointPublisher, client)
          case None =>
            throw new IllegalArgumentException("Required channel not registered " + channelName)
        }
    }
  }

  abstract override def removeChannel(channel: String): Unit = {
    channels.get(channel) match {
      case Some(Channel(_, listener)) =>
        // if a channel is removed, check to see that all of the endpoints using the channel have been removed
        val noUsingEndpoints = endpoints.values.filter { e =>
          e.channel match {
            case Some(x) => x.getName == channel
            case None => false
          }
        }.isEmpty
        if (noUsingEndpoints) {
          channels -= channel
          super.removeChannel(channel)
        }
        listener
      case None =>
        throw new IllegalArgumentException("Cannot remove unknown channel " + channel)
    }
  }

  /// remove the device from the map and its channel's device list
  abstract override def removeEndpoint(endpoint: String): Unit = {
    endpoints.get(endpoint) match {
      case Some(Endpoint(_, _, _, listener)) =>
        endpoints -= endpoint
        super.removeEndpoint(endpoint)
        listener
      case None =>
        throw new IllegalArgumentException("Cannot remove unknown endpoint " + endpoint)
    }
  }

}
