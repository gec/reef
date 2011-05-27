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

import org.totalgrid.reef.proto.{ FEP, Model, Measurements }
import scala.collection.immutable

import FEP.CommChannel
import Measurements.MeasurementBatch

import org.totalgrid.reef.util.Logging

trait BaseProtocol extends IProtocol with Logging {

  case class Endpoint(name: String, channel: Option[FEP.CommChannel], config: List[Model.ConfigFile], listener: IListener[FEP.CommEndpointConnection.State]) /// The issue function and the channel
  case class Channel(config: FEP.CommChannel, listener: IListener[CommChannel.State])

  // only mutable state is current assignment of these variables
  private var endpoints = immutable.Map.empty[String, Endpoint] /// maps uids to a Endpoint
  private var channels = immutable.Map.empty[String, Channel] /// maps uids to a Port

  override def addChannel(p: FEP.CommChannel, listener: IListener[CommChannel.State]): Unit = {
    channels.get(p.getName) match {
      case None =>
        channels = channels + (p.getName -> Channel(p, listener))
        _addChannel(p, listener)
      case Some(x) =>
        logger.info("Ignoring duplicate channel " + p)
    }
  }

  override def addEndpoint(endpoint: String, channelName: String, config: List[Model.ConfigFile], publish: IListener[MeasurementBatch], listener: IListener[FEP.CommEndpointConnection.State]): ICommandHandler = {

    endpoints.get(endpoint) match {
      case Some(x) => throw new IllegalArgumentException("Endpoint already exists: " + endpoint)
      case None =>
        channels.get(channelName) match {
          case Some(p) =>
            endpoints += endpoint -> Endpoint(endpoint, Some(p.config), config, listener)
            _addEndpoint(endpoint, channelName, config, publish, listener)
          case None =>
            if (requiresChannel) throw new IllegalArgumentException("Port not registered " + channelName)
            endpoints += endpoint -> Endpoint(endpoint, None, config, listener)
            _addEndpoint(endpoint, channelName, config, publish, listener)
        }
    }
  }

  override def removeChannel(channel: String): IListener[CommChannel.State] = {
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
          _removeChannel(channel)
        }
        listener
      case None =>
        throw new IllegalArgumentException("Cannot remove unknown channel " + channel)
    }
  }

  /// remove the device from the map and its channel's device list
  override def removeEndpoint(endpoint: String): IListener[FEP.CommEndpointConnection.State] = {
    endpoints.get(endpoint) match {
      case Some(Endpoint(_, _, _, listener)) =>
        endpoints -= endpoint
        _removeEndpoint(endpoint)
        listener
      case None =>
        throw new IllegalArgumentException("Cannot remove unknown endpoint " + endpoint)
    }
  }

  /// These get implemented by the parent
  protected def _addChannel(p: FEP.CommChannel, listener: IListener[CommChannel.State])

  protected def _removeChannel(channel: String)

  protected def _addEndpoint(endpoint: String,
    channel: String,
    config: List[Model.ConfigFile],
    publish: IListener[MeasurementBatch],
    listener: IListener[FEP.CommEndpointConnection.State]): ICommandHandler

  protected def _removeEndpoint(endpoint: String)

}
