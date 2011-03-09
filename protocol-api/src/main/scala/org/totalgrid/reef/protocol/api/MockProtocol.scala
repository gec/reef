/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  Green Energy Corp licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.protocol.api

import org.totalgrid.reef.proto.{ FEP, Model, Commands }
import scala.concurrent.MailBox

object MockProtocol {
  case class AddPort(p: FEP.Port)
  case class RemovePort(name: String)
  case class AddEndpoint(endpoint: String, port: String, config: List[Model.ConfigFile])
  case class RemoveEndpoint(endpoint: String)
}

class MockProtocol(val requiresPort: Boolean = true) extends BaseProtocol {

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

  override def _addPort(p: FEP.Port) = mail send AddPort(p)

  override def _removePort(port: String) = mail send RemovePort(port)

  override def _addEndpoint(endpoint: String, port: String, config: List[Model.ConfigFile], publish: IPublisher): ICommandHandler = {
    mail send AddEndpoint(endpoint, port, config)
    new ICommandHandler {
      def issue(request: Commands.CommandRequest, rspHandler: IResponseHandler) = mail send request
    }
  }

  override def _removeEndpoint(endpoint: String) = mail send RemoveEndpoint(endpoint)
}