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
package org.totalgrid.reef.sapi.service

import org.totalgrid.reef.japi.{ Envelope, TypeDescriptor }
import org.totalgrid.reef.sapi.RequestEnv

object AsyncService {
  type ServiceFunction = (Envelope.ServiceRequest, RequestEnv, ServiceResponseCallback) => Unit
}

/**
 * Defines how to complete a service call with a ServiceResponse
 */
trait AsyncService[A] extends ServiceDescriptor[A] {
  def respond(req: Envelope.ServiceRequest, env: RequestEnv, callback: ServiceResponseCallback): Unit
}

/**
 * A concrete example service that always responds immediately with Success and the correct Id
 */
class NoOpService extends AsyncService[Any] {

  import Envelope._

  /// noOpService that returns OK
  def respond(request: ServiceRequest, env: RequestEnv, callback: ServiceResponseCallback) =
    callback.onResponse(ServiceResponse.newBuilder.setStatus(Status.OK).setId(request.getId).build)

  override val descriptor = new TypeDescriptor[Any] {
    def serialize(typ: Any): Array[Byte] = throw new Exception("unimplemented")
    def deserialize(data: Array[Byte]): Any = throw new Exception("unimplemented")
    def getKlass: Class[Any] = throw new Exception("unimplemented")
    def id = "Any"
  }
}