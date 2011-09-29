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
package org.totalgrid.reef.messaging

import org.totalgrid.reef.util.Logging

import org.totalgrid.reef.messaging.ProtoSerializer._
import scala.collection.JavaConversions._

import org.totalgrid.reef.sapi.client._
import org.totalgrid.reef.sapi._
import org.totalgrid.reef.japi.Envelope

/**
 * Messaging based implementation of ClientSession. Looks up information on the requested operation using the class of the request object.
 *
 */
class AmqpClientSession(
    factory: ClientSessionFactory,
    lookup: ServiceList,
    timeoutms: Long) extends ClientSession with AsyncRestAdapter with RequestSpyHook with Logging {

  private val correlator = factory.getServiceResponseCorrelator(timeoutms)

  final override def isOpen = correlator.isOpen
  override def close() = correlator.close()

  final override def asyncRequest[A](verb: Envelope.Verb, request: A, env: RequestEnv, dest: Routable)(callback: Response[A] => Unit) {

    val info: ServiceInfo[A, _] = lookup.getServiceInfo(ClassLookup[A](request))
    val requestBuilder = Envelope.ServiceRequest.newBuilder.setVerb(verb).setPayload(info.descriptor.serialize(request))

    val sendEnv: RequestEnv = mergeHeaders(env)
    sendEnv.asKeyValueList.foreach(kv => requestBuilder.addHeaders(Envelope.RequestHeader.newBuilder.setKey(kv._1).setValue(kv._2).build))

    def handleResponse(resp: Option[Envelope.ServiceResponse]) {
      val response: Option[Response[A]] = resp match {
        case Some(x) =>
          try {
            val list = x.getPayloadList.map { x => info.descriptor.deserialize(x.toByteArray) }.toList
            val error = if (x.hasErrorMessage) x.getErrorMessage else ""
            Some(Response(x.getStatus, list, error))
          } catch {
            case ex: Exception =>
              logger.warn("Error deserializing proto: ", ex)
              None
          }
        case None => None
      }

      callback(Response.convert(response))
    }

    correlator.send(requestBuilder, info.descriptor.id, dest.key, handleResponse)
  }

  final override def addSubscription[A](klass: Class[_]): Subscription[A] = {

    val info = lookup.getServiceInfo(klass)
    val deser = (info.subType.deserialize _).asInstanceOf[Array[Byte] => A]

    factory.prepareSubscription(deser)
  }

}