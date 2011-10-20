package org.totalgrid.reef.api.sapi.client.rest.impl

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

import org.totalgrid.reef.api.sapi.client.{ BasicRequestHeaders, Response }
import org.totalgrid.reef.api.japi.{ Envelope, TypeDescriptor }
import org.totalgrid.reef.api.japi.Envelope.{ ServiceResponse, ServiceRequest, ServiceNotification, Event }
import collection.JavaConversions._
import com.google.protobuf.{ GeneratedMessage, ByteString }

object RestHelpers {

  def getEvent(typ: Event, value: GeneratedMessage): ServiceNotification =
    Envelope.ServiceNotification.newBuilder.setEvent(typ).setPayload(value.toByteString).build

  def getEvent[A](typ: Event, value: A, desc: TypeDescriptor[A]): ServiceNotification =
    Envelope.ServiceNotification.newBuilder.setEvent(typ).setPayload(ByteString.copyFrom(desc.serialize(value))).build

  def buildServiceRequest[A](verb: Envelope.Verb, request: A, desc: TypeDescriptor[A], uuid: String, env: BasicRequestHeaders): ServiceRequest = {
    val builder = Envelope.ServiceRequest.newBuilder.setVerb(verb).setId(uuid).setPayload(ByteString.copyFrom(desc.serialize(request)))
    env.toEnvelopeRequestHeaders.foreach(builder.addHeaders)
    builder.build()
  }

  def readServiceResponse[A](desc: TypeDescriptor[A], rsp: ServiceResponse): Response[A] = {
    val list = rsp.getPayloadList.map(bs => desc.deserialize(bs.toByteArray())).toList
    Response.apply[A](rsp.getStatus, list, rsp.getErrorMessage)
  }

}