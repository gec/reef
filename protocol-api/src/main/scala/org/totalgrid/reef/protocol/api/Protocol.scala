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
import org.totalgrid.reef.sapi.Promise

import Measurements.MeasurementBatch
import FEP.CommChannel

object Protocol {

  def find(files: List[Model.ConfigFile], mimetype: String): Model.ConfigFile = {
    files.find { _.getMimeType == mimetype }.getOrElse { throw new Exception("Missing file w/ mime-type: " + mimetype) }
  }
}

trait CommandHandler {
  def issue(cmd: Commands.CommandRequest, listener: Listener[Commands.CommandResponse])
}

trait Listener[A] {
  def onUpdate(value: A)
}

trait Publisher {
  def publish(batch : MeasurementBatch) : Promise[Boolean]
}

trait NullListener[A] extends Listener[A] {
  final override def onUpdate(value: A) = {}
}

case object NullPublisher extends NullListener[Measurements.MeasurementBatch]

case object NullEndpointListener extends NullListener[FEP.CommEndpointConnection.State]

case object NullChannelListener extends NullListener[CommChannel.State]

trait Protocol {

  import Protocol._

  /**
   * @return Unique name, i.e. 'dnp3'
   */
  def name: String

  /**
   * if true the protocol trait will verify that the each device is associated with a port, if false we dont care
   * if there is a port or not.
   */
  def requiresChannel: Boolean

  def addChannel(channel: FEP.CommChannel, channelListener: Listener[CommChannel.State]): Unit

  def removeChannel(channel: String): Listener[CommChannel.State]

  def addEndpoint(endpoint: String,
    channelName: String,
    config: List[Model.ConfigFile],
    publish: Listener[MeasurementBatch],
    epListener: Listener[FEP.CommEndpointConnection.State]): CommandHandler

  def removeEndpoint(endpoint: String): Listener[FEP.CommEndpointConnection.State]
}
