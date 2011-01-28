/**
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

import org.totalgrid.reef.proto.{ FEP, Commands, Measurements, Model }
import scala.collection.immutable

object IProtocol {
  type Issue = Commands.CommandRequest => Unit
  type Respond = Commands.CommandResponse => Unit
  type Publish = Measurements.MeasurementBatch => Unit

  def find(files: List[Model.ConfigFile], mimetype: String): Model.ConfigFile = {
    files.find { _.getMimeType == mimetype }.getOrElse { throw new Exception("Missing file w/ mime-type: " + mimetype) }
  }
}

trait IProtocol {

  val name: String /// unique name, i.e. dnp3

  /**
   * if true the protocol trait will verify that the each device is associated with a port, if false we dont care
   * if there is a port or not.
   */
  val requiresPort: Boolean

  def addPort(p: FEP.Port): Unit
  def removePort(port: String): Unit

  def addEndpoint(endpoint: String, portName: String, config: List[Model.ConfigFile], publish: IProtocol.Publish, command: IProtocol.Respond): IProtocol.Issue
  def removeEndpoint(endpoint: String): Unit
}
