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
package org.totalgrid.reef.client.sapi.service

import org.totalgrid.reef.client.proto.Envelope
import com.google.protobuf.ByteString
import org.totalgrid.reef.client.sapi.client.Response
import org.totalgrid.reef.client.exception.ReefServiceException
import org.totalgrid.reef.client.types.TypeDescriptor
import com.weiglewilczek.slf4s.Logging

object ServiceHelpers extends Logging {
  def getResponse[A](id: String, rsp: Response[A], descriptor: TypeDescriptor[A]): Envelope.ServiceResponse = {
    val ret = Envelope.ServiceResponse.newBuilder.setId(id)
    ret.setStatus(rsp.status).setErrorMessage(rsp.error)
    rsp.list.foreach { x: A => ret.addPayload(ByteString.copyFrom(descriptor.serialize(x))) }
    ret.build
  }

  def getFailure(id: String, status: Envelope.Status, errorMsg: String) = {
    Envelope.ServiceResponse.newBuilder.setId(id).setStatus(status).setErrorMessage(errorMsg).build
  }

  def catchErrors(req: Envelope.ServiceRequest, callback: ServiceResponseCallback)(fun: => Unit) = {
    try {
      fun
    } catch {
      case px: ReefServiceException =>
        logger.error(px.getMessage, px)
        callback.onResponse(getFailure(req.getId, px.getStatus, px.getMessage))
      case x: Exception =>
        logger.error(x.getMessage, x)
        val msg = x.getMessage + "\n" + x.getStackTraceString
        callback.onResponse(getFailure(req.getId, Envelope.Status.INTERNAL_ERROR, msg))
    }
  }
}

trait ServiceHelpers[A] {
  self: ServiceDescriptor[A] =>

  import ServiceHelpers._

  def getResponse(id: String, rsp: Response[A]): Envelope.ServiceResponse =
    ServiceHelpers.getResponse(id, rsp, descriptor)

}