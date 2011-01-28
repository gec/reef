/**
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

import org.totalgrid.reef.messaging.sync._
import org.totalgrid.reef.proto.Envelope

import scala.collection.JavaConversions._
import org.totalgrid.reef.messaging._
import com.google.protobuf.GeneratedMessage

/**
 * wraps a ProtoClient with some java helpers to convert to and from java lists
 */
class JavaProtoClientWrapper(client: ProtoClient) extends IServiceClient {

  def request[T <: GeneratedMessage](verb: Envelope.Verb, payload: T, env: ServiceHandlerHeaders) = client.verb(verb, payload, env.env)

  def get[T <: GeneratedMessage](payload: T, env: ServiceHandlerHeaders): java.util.List[T] = client.get(payload, env.env)
  def delete[T <: GeneratedMessage](payload: T, env: ServiceHandlerHeaders): java.util.List[T] = client.delete(payload, env.env)
  def post[T <: GeneratedMessage](payload: T, env: ServiceHandlerHeaders): java.util.List[T] = client.post(payload, env.env)
  def put[T <: GeneratedMessage](payload: T, env: ServiceHandlerHeaders): java.util.List[T] = client.put(payload, env.env)

  def get_one[T <: GeneratedMessage](payload: T, env: ServiceHandlerHeaders): T = client.get_one(payload, env.env)
  def delete_one[T <: GeneratedMessage](payload: T, env: ServiceHandlerHeaders): T = client.delete_one(payload, env.env)
  def put_one[T <: GeneratedMessage](payload: T, env: ServiceHandlerHeaders): T = client.put_one(payload, env.env)

  def get[T <: GeneratedMessage](payload: T, sub: Subscription): java.util.List[T] = client.get(payload, getEnv(sub))
  def delete[T <: GeneratedMessage](payload: T, sub: Subscription): java.util.List[T] = client.delete(payload, getEnv(sub))
  def post[T <: GeneratedMessage](payload: T, sub: Subscription): java.util.List[T] = client.post(payload, getEnv(sub))
  def put[T <: GeneratedMessage](payload: T, sub: Subscription): java.util.List[T] = client.put(payload, getEnv(sub))

  def get_one[T <: GeneratedMessage](payload: T, sub: Subscription): T = client.get_one(payload, getEnv(sub))
  def delete_one[T <: GeneratedMessage](payload: T, sub: Subscription): T = client.delete_one(payload, getEnv(sub))
  def put_one[T <: GeneratedMessage](payload: T, sub: Subscription): T = client.put_one(payload, getEnv(sub))

  def get[T <: GeneratedMessage](payload: T): java.util.List[T] = client.get(payload)
  def delete[T <: GeneratedMessage](payload: T): java.util.List[T] = client.delete(payload)
  def post[T <: GeneratedMessage](payload: T): java.util.List[T] = client.post(payload)
  def put[T <: GeneratedMessage](payload: T): java.util.List[T] = client.put(payload)

  def get_one[T <: GeneratedMessage](payload: T): T = client.get_one(payload)
  def delete_one[T <: GeneratedMessage](payload: T): T = client.delete_one(payload)
  def put_one[T <: GeneratedMessage](payload: T): T = client.put_one(payload)

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

