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

import com.google.protobuf.GeneratedMessage
import scala.collection.JavaConversions._

import org.totalgrid.reef.util.Logging
import org.totalgrid.reef.proto.Envelope
import ProtoSerializer.convertProtoToByteString //implicit

import org.totalgrid.reef.protoapi.{ RequestEnv, ProtoServiceTypes }
import org.totalgrid.reef.protoapi.client.ServiceClient

import ProtoServiceTypes.{ TypedResponseCallback, Response }

/** Wraps a regular AMQPProtoServiceClient, layering on machinery for serializing/deserializing
 *	the payload of the service envelope. 
 * 
 *	@param 	exchange 		AMQP exchange name
 *	@param	timeoutms		The reply timeout in milliseconds
 *	@param 	deserialize 	Function that deserializes the proto (typically parseFrom) 
 */
class ProtoServiceClient[T <: GeneratedMessage](
  deserialize: Array[Byte] => T,
  exchange: String,
  key: String,
  correlator: ServiceResponseCorrelator)
    extends ServiceClient with Logging {

  def close(): Unit = correlator.close()
  /** Sends a service request of type T with optional subscribe queue.
   *	@param	verb			Type of request being made
   *	@param	payload			Data associated with the request
   * 	@param 	subscribe_queue	Subscribe queue name (onyl valid for verb=Subscribe)
   *	@return					Blocking function for getting the response (a future)
   */
  def request[A <: GeneratedMessage](verb: Envelope.Verb, payloadA: A, env: RequestEnv, callbackA: TypedResponseCallback[A]) {
    // TODO: get rid of these casts
    val payload = payloadA.asInstanceOf[T] // will explode if type is wrong
    val callback = callbackA.asInstanceOf[(Option[Response[T]]) => Unit]

    val request = Envelope.ServiceRequest.newBuilder.setVerb(verb).setPayload(payload)

    val sendEnv = if (defaultEnv.isDefined) env.merge(defaultEnv.get) else env
    sendEnv.asKeyValueList.foreach(kv => request.addHeaders(Envelope.RequestHeader.newBuilder.setKey(kv._1).setValue(kv._2).build))

    send(request, callback)
  }

  private def send(request: Envelope.ServiceRequest.Builder, callback: TypedResponseCallback[T]) = {

    def handleResponse(resp: Option[Envelope.ServiceResponse]) {
      val result = resp match {
        case Some(x) =>
          try {
            val list = x.getPayloadList.map { x => deserialize(x.toByteArray) }.toList
            val error = if (x.hasErrorMessage) x.getErrorMessage else ""
            Some(Response(x.getStatus, error, list))
          } catch {
            case ex: Exception =>
              warn("Error deserializing proto: ", ex)
              None
          }
        case None => None
      }
      callback(result)
    }

    correlator.send(request, exchange, key, handleResponse)

  }

}

