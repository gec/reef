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
package org.totalgrid.reef.messaging.javabridge

import org.totalgrid.reef.proto.Envelope

import scala.collection.JavaConversions._
import org.totalgrid.reef.messaging._
import com.google.protobuf.GeneratedMessage

import org.totalgrid.reef.protoapi.{ ServiceHandlerHeaders, RequestEnv }

/**
 * wraps a ProtoClient with some java helpers to convert to and from java lists
 */
class JavaProtoClientWrapper(client: ProtoClient) extends IServiceClient {

  def request[T <: GeneratedMessage](verb: Envelope.Verb, payload: T, env: ServiceHandlerHeaders): java.util.List[T] = client.requestThrow(verb, payload, env.env)

  def get[T <: GeneratedMessage](payload: T, env: ServiceHandlerHeaders): java.util.List[T] = client.getThrow(payload, env.env)
  def delete[T <: GeneratedMessage](payload: T, env: ServiceHandlerHeaders): java.util.List[T] = client.deleteThrow(payload, env.env)
  def post[T <: GeneratedMessage](payload: T, env: ServiceHandlerHeaders): java.util.List[T] = client.postThrow(payload, env.env)
  def put[T <: GeneratedMessage](payload: T, env: ServiceHandlerHeaders): java.util.List[T] = client.putThrow(payload, env.env)

  def getOne[T <: GeneratedMessage](payload: T, env: ServiceHandlerHeaders): T = client.getOneThrow(payload, env.env)
  def deleteOne[T <: GeneratedMessage](payload: T, env: ServiceHandlerHeaders): T = client.deleteOneThrow(payload, env.env)
  def putOne[T <: GeneratedMessage](payload: T, env: ServiceHandlerHeaders): T = client.putOneThrow(payload, env.env)

  def get[T <: GeneratedMessage](payload: T, sub: Subscription): java.util.List[T] = client.getThrow(payload, getEnv(sub))
  def delete[T <: GeneratedMessage](payload: T, sub: Subscription): java.util.List[T] = client.deleteThrow(payload, getEnv(sub))
  def post[T <: GeneratedMessage](payload: T, sub: Subscription): java.util.List[T] = client.postThrow(payload, getEnv(sub))
  def put[T <: GeneratedMessage](payload: T, sub: Subscription): java.util.List[T] = client.putThrow(payload, getEnv(sub))

  def getOne[T <: GeneratedMessage](payload: T, sub: Subscription): T = client.getOneThrow(payload, getEnv(sub))
  def deleteOne[T <: GeneratedMessage](payload: T, sub: Subscription): T = client.deleteOneThrow(payload, getEnv(sub))
  def putOne[T <: GeneratedMessage](payload: T, sub: Subscription): T = client.putOneThrow(payload, getEnv(sub))

  def get[T <: GeneratedMessage](payload: T): java.util.List[T] = client.getThrow(payload)
  def delete[T <: GeneratedMessage](payload: T): java.util.List[T] = client.deleteThrow(payload)
  def post[T <: GeneratedMessage](payload: T): java.util.List[T] = client.postThrow(payload)
  def put[T <: GeneratedMessage](payload: T): java.util.List[T] = client.putThrow(payload)

  def getOne[T <: GeneratedMessage](payload: T): T = client.getOneThrow(payload)
  def deleteOne[T <: GeneratedMessage](payload: T): T = client.deleteOneThrow(payload)
  def putOne[T <: GeneratedMessage](payload: T): T = client.putOneThrow(payload)

  def addSubscription[T <: GeneratedMessage](pd: ProtoDescriptor[T], ea: EventAcceptor[T]): Subscription = {
    client.addSubscription(pd.getKlass, ea.onEvent)
  }

  // we create a defaultEnv here and pass it to the underlying ServiceClient so we keep a reference to a request
  // env that we control and can update, the underlying client will see any updates
  // TODO: make defaultEnv immutable
  private val defaultEnv = new ServiceHandlerHeaders(new RequestEnv)
  client.setDefaultEnv(defaultEnv.env)

  override def getDefaultEnv = defaultEnv

  def close() = client.close

  private def getEnv(sub: Subscription): RequestEnv = {
    val headers = new ServiceHandlerHeaders(new RequestEnv)
    sub.configure(headers)
    headers.env
  }
}

