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

import org.totalgrid.reef.proto.{ Model, FEP }

trait EndpointAlwaysOnline extends IProtocol {

  abstract override def addEndpoint(endpoint: String, channel: String, config: List[Model.ConfigFile], publish: IPublisher, listener: IEndpointListener): ICommandHandler = {
    val ret = super.addEndpoint(endpoint, channel, config, publish, listener)
    listener.onStateChange(FEP.CommEndpointConnection.State.COMMS_UP)
    ret
  }

  abstract override def removeEndpoint(endpoint: String): IEndpointListener = {
    val ret = super.removeEndpoint(endpoint)
    ret.onStateChange(FEP.CommEndpointConnection.State.COMMS_DOWN)
    ret
  }

}
