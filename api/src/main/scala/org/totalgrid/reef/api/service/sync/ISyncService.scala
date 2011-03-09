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
package org.totalgrid.reef.api.service.sync

import org.totalgrid.reef.api._
import org.totalgrid.reef.api.service.ServiceDescriptor
import org.totalgrid.reef.api.Envelope

object ISyncService {
  /**
   * type of ServiceRequestHandler.respond
   */
  type Respond = (Envelope.ServiceRequest, RequestEnv) => Envelope.ServiceResponse
}

/**
 * classes that are going to be handling service requests should inherit this interface
 * to provide a consistent interface so we can easily implement a type of "middleware" 
 * wrapper that layers functionality on a request 
 */
trait ISyncService[A] extends ServiceDescriptor[A] {
  def respond(req: Envelope.ServiceRequest, env: RequestEnv): Envelope.ServiceResponse
}

