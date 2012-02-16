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
package org.totalgrid.reef.client.javaimpl

import org.totalgrid.reef.client.registration.{ Service, ServiceResponseCallback => JSRC }
import org.totalgrid.reef.client.types.TypeDescriptor
import org.totalgrid.reef.client.sapi.client.BasicRequestHeaders
import org.totalgrid.reef.client.sapi.service.{ ServiceResponseCallback, AsyncService }
import scala.collection.JavaConversions._
import org.totalgrid.reef.client.proto.Envelope.{ ServiceResponse, ServiceRequest }

class ServiceResponseCallbackWrapper(scallback: ServiceResponseCallback) extends JSRC {
  def onResponse(rsp: ServiceResponse) = {
    scallback.onResponse(rsp)
  }
}

class ServiceWrapper[A](service: Service, val descriptor: TypeDescriptor[A]) extends AsyncService[A] {

  def respond(req: ServiceRequest, env: BasicRequestHeaders, callback: ServiceResponseCallback) {

    val headers: java.util.Map[String, java.util.List[String]] = mapAsJavaMap(env.headers.map(tup => (tup._1, seqAsJavaList(tup._2))))
    service.respond(req, headers, new ServiceResponseCallbackWrapper(callback))
  }
}