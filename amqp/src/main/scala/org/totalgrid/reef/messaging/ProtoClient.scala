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

import org.totalgrid.reef.util.Logging

import com.google.protobuf.GeneratedMessage

import org.totalgrid.reef.messaging.ProtoSerializer._
import _root_.scala.collection.JavaConversions._

import org.totalgrid.reef.api.ServiceTypes.{ MultiResult, Response }
import org.totalgrid.reef.api.scalaclient.ClientSession
import org.totalgrid.reef.api._

/**
 * a super client that switches on the passed in proto to automatically call the correct client so the app developer
 * doesn't have to manage the clients manually. NOT THREAD SAFE, needs to be used from a single thread at a time.
 *
 */
class ProtoClient(
    factory: ClientSessionFactory,
    lookup: ServiceList, timeoutms: Long) extends ClientSession with Logging {

  private val correlator = factory.getServiceResponseCorrelator(timeoutms)
  private var clients = Map.empty[Class[_], ClientSession]

  def asyncRequest[A <: AnyRef](verb: Envelope.Verb, payload: A, env: RequestEnv, dest: IDestination)(callback: MultiResult[A] => Unit) {

    val info = lookup.getServiceInfo(payload.getClass.asInstanceOf[Class[A]])
    val request = Envelope.ServiceRequest.newBuilder.setVerb(verb).setPayload(info.descriptor.serialize(payload))

    val sendEnv = mergeHeaders(env)
    sendEnv.asKeyValueList.foreach(kv => request.addHeaders(Envelope.RequestHeader.newBuilder.setKey(kv._1).setValue(kv._2).build))

    def handleResponse(resp: Option[Envelope.ServiceResponse]) {
      val result = resp match {
        case Some(x) =>
          try {
            val list = x.getPayloadList.map { x => info.descriptor.deserialize(x.toByteArray) }.toList
            val error = if (x.hasErrorMessage) x.getErrorMessage else ""
            Some(Response(x.getStatus, list, error))
          } catch {
            case ex: Exception =>
              warn("Error deserializing proto: ", ex)
              None
          }
        case None => None
      }

      import org.totalgrid.reef.api.scalaclient.ProtoConversions._
      callback(result)
    }

    correlator.send(request, info.descriptor.id, dest.key, handleResponse)
  }

  def addSubscription[A <: GeneratedMessage](klass: Class[_]): Subscription[A] = {

    // TODO: lookup by subscription klass instead of serviceKlass
    val info = lookup.getServiceInfo(klass)
    val deser = (info.subType.deserialize _).asInstanceOf[Array[Byte] => A]
    val subIsStreamType = info.subIsStreamType

    factory.prepareSubscription(deser, subIsStreamType)
  }

  def close() = correlator.close
}