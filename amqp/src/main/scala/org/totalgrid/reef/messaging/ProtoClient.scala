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
package org.totalgrid.reef.messaging

import javabridge.Subscription

import org.totalgrid.reef.protoapi.{ ProtoServiceTypes, RequestEnv }
import ProtoServiceTypes.{ Event, Response }
import org.totalgrid.reef.protoapi.client.ServiceClient

import org.totalgrid.reef.proto.Envelope
import com.google.protobuf.GeneratedMessage

/**
 * a super client that switches on the passed in proto to automatically call the correct client so the app developer
 * doesn't have to manage the clients manually. NOT THREAD SAFE, needs to be used from a single thread at a time.
 */
class ProtoClient(
    factory: ServiceClientFactory,
    timeoutms: Long,
    serviceInfo: Class[_] => ServiceInfo) extends ServiceClient {

  private val correlator = factory.getServiceResponseCorrelator(timeoutms)
  private var clients = Map.empty[Class[_], ServiceClient]

  private def getClient[T <: GeneratedMessage](klass: Class[_]): ServiceClient = {
    clients.get(klass) match {
      case Some(client) => client
      case None =>
        val info = serviceInfo(klass)
        val deser = (info.descriptor.deserializeBytes _).asInstanceOf[Array[Byte] => T]
        val client = factory.addProtoServiceClient[T](info.exchange, "request", deser, correlator)
        clients = clients + (klass -> client)
        defaultEnv.foreach(client.setDefaultEnv(_))
        client
    }
  }

  def request[A <: GeneratedMessage](verb: Envelope.Verb, payload: A, env: RequestEnv, callback: Option[Response[A]] => Unit) {
    val client = getClient[A](payload.getClass())

    client.request(verb, payload, env, callback)
  }

  def addSubscription[T <: GeneratedMessage](ea: (Envelope.Event, T) => Unit): Subscription = {

    val applyMethod = ea.getClass.getDeclaredMethods.find { x => x.getName.equals("apply") }.get
    val klass = applyMethod.getParameterTypes.apply(1).asInstanceOf[Class[T]]

    val proxy = { (evt: Event[T]) => ea(evt.event, evt.result) }

    addSubscription(klass, proxy)
  }

  def addSubscription[T <: GeneratedMessage](klass: Class[_], ea: Event[T] => Unit): Subscription = {

    // TODO: lookup by subscription klass instead of serviceKlass
    val info = serviceInfo(klass)
    val deser = (info.subType.deserializeBytes _).asInstanceOf[Array[Byte] => T]
    val subIsStreamType = info.subIsStreamType

    factory.prepareSubscription(deser, subIsStreamType, ea)
  }

  def close() = correlator.close
}