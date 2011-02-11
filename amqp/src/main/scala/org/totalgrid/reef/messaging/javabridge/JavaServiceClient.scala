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

import com.google.protobuf.GeneratedMessage
import org.totalgrid.reef.protoapi.{ ServiceHandlerHeaders, RequestEnv, TypeDescriptor }
import org.totalgrid.reef.messaging.ProtoClient

import scala.collection.JavaConversions._

/**
 * wraps a ProtoClient with some java helpers to convert to and from java lists
 */
class JavaProtoClientWrapper(client: ProtoClient) extends IServiceClient {

  def request[T <: GeneratedMessage](verb: Envelope.Verb, payload: T, env: ServiceHandlerHeaders): java.util.List[T] = client.requestThrow(verb, payload, env.env)

  def get[T <: GeneratedMessage](payload: T, env: ServiceHandlerHeaders): java.util.List[T] = client.getOrThrow(payload, env.env)
  def delete[T <: GeneratedMessage](payload: T, env: ServiceHandlerHeaders): java.util.List[T] = client.deleteOrThrow(payload, env.env)
  def post[T <: GeneratedMessage](payload: T, env: ServiceHandlerHeaders): java.util.List[T] = client.postOrThrow(payload, env.env)
  def put[T <: GeneratedMessage](payload: T, env: ServiceHandlerHeaders): java.util.List[T] = client.putOrThrow(payload, env.env)

  def getOne[T <: GeneratedMessage](payload: T, env: ServiceHandlerHeaders): T = client.getOneOrThrow(payload, env.env)
  def deleteOne[T <: GeneratedMessage](payload: T, env: ServiceHandlerHeaders): T = client.deleteOneOrThrow(payload, env.env)
  def putOne[T <: GeneratedMessage](payload: T, env: ServiceHandlerHeaders): T = client.putOneOrThrow(payload, env.env)

  def get[T <: GeneratedMessage](payload: T, sub: Subscription): java.util.List[T] = client.getOrThrow(payload, getEnv(sub))
  def delete[T <: GeneratedMessage](payload: T, sub: Subscription): java.util.List[T] = client.deleteOrThrow(payload, getEnv(sub))
  def post[T <: GeneratedMessage](payload: T, sub: Subscription): java.util.List[T] = client.postOrThrow(payload, getEnv(sub))
  def put[T <: GeneratedMessage](payload: T, sub: Subscription): java.util.List[T] = client.putOrThrow(payload, getEnv(sub))

  def getOne[T <: GeneratedMessage](payload: T, sub: Subscription): T = client.getOneOrThrow(payload, getEnv(sub))
  def deleteOne[T <: GeneratedMessage](payload: T, sub: Subscription): T = client.deleteOneOrThrow(payload, getEnv(sub))
  def putOne[T <: GeneratedMessage](payload: T, sub: Subscription): T = client.putOneOrThrow(payload, getEnv(sub))

  def get[T <: GeneratedMessage](payload: T): java.util.List[T] = client.getOrThrow(payload)
  def delete[T <: GeneratedMessage](payload: T): java.util.List[T] = client.deleteOrThrow(payload)
  def post[T <: GeneratedMessage](payload: T): java.util.List[T] = client.postOrThrow(payload)
  def put[T <: GeneratedMessage](payload: T): java.util.List[T] = client.putOrThrow(payload)

  def getOne[T <: GeneratedMessage](payload: T): T = client.getOneOrThrow(payload)
  def deleteOne[T <: GeneratedMessage](payload: T): T = client.deleteOneOrThrow(payload)
  def putOne[T <: GeneratedMessage](payload: T): T = client.putOneOrThrow(payload)

  def addSubscription[T <: GeneratedMessage](pd: TypeDescriptor[T], ea: EventAcceptor[T]): Subscription = {
    client.addSubscription(pd.getKlass, ea.onEvent)
  }

  // we create a defaultEnv here and pass it to the underlying ServiceClient so we keep a reference to a request
  // env that we control and can update, the underlying client will see any updates
  // TODO: make defaultEnv immutable
  private val defaultEnv = new ServiceHandlerHeaders(new RequestEnv)
  client.setDefaultHeaders(defaultEnv.env)

  override def getDefaultEnv = defaultEnv

  def close() = client.close

  private def getEnv(sub: Subscription): RequestEnv = {
    val headers = new ServiceHandlerHeaders(new RequestEnv)
    sub.configure(headers)
    headers.env
  }
}

