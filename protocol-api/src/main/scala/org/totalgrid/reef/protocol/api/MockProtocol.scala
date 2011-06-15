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

import org.totalgrid.reef.proto.{ FEP, Model, Commands, Measurements }

import FEP.CommChannel
import Measurements.MeasurementBatch

import scala.concurrent.MailBox

object MockProtocol {
  case class AddPort(p: FEP.CommChannel)
  case class RemovePort(name: String)
  case class AddEndpoint(endpoint: String, port: String, config: List[Model.ConfigFile])
  case class RemoveEndpoint(endpoint: String)
}

class MockProtocol(needsChannel: Boolean = true) extends BaseProtocol {

  def requiresChannel = needsChannel

  case object NOTHING

  import MockProtocol._
  val mail = new MailBox

  // only good for single threaded test
  def checkForNothing = {
    mail send NOTHING
    mail.receiveWithin(1) {
      case NOTHING =>
    }
  }

  def checkFor[A](x: PartialFunction[Any, A]): A = checkForTimeout(5000)(x)

  def checkForTimeout[A](timeout: Int)(x: PartialFunction[Any, A]): A = {
    assert(timeout > 0)
    mail.receiveWithin(timeout)(x)
  }

  val name: String = "mock"

  override def _addChannel(channel: FEP.CommChannel, listener: Listener[CommChannel.State]) = mail send AddPort(channel)

  override def _removeChannel(channel: String) = mail send RemovePort(channel)

  override def _addEndpoint(endpoint: String, channel: String, config: List[Model.ConfigFile], publish: Listener[MeasurementBatch], listener: Listener[FEP.CommEndpointConnection.State]): CommandHandler = {
    mail send AddEndpoint(endpoint, channel, config)
    new CommandHandler {
      def issue(request: Commands.CommandRequest, rspHandler: Listener[Commands.CommandResponse]) = mail send request
    }
  }

  override def _removeEndpoint(endpoint: String) = mail send RemoveEndpoint(endpoint)
}