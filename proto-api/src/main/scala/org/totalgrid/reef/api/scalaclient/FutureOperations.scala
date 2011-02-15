package org.totalgrid.reef.api.scalaclient

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
import com.google.protobuf.GeneratedMessage
import org.totalgrid.reef.api.RequestEnv
import org.totalgrid.reef.api.Envelope.Verb
import org.totalgrid.reef.api.ServiceTypes.MultiResult

trait FutureOperations {

  self: AsyncOperations with DefaultHeaders =>

  def requestFuture[A <: AnyRef](verb: Verb, payload: A, env: RequestEnv = getDefaultHeaders): () => MultiResult[A] = makeCallbackIntoFuture {
    asyncRequest(verb, payload, env)
  }

  def getWithFuture[A <: AnyRef](payload: A, env: RequestEnv = getDefaultHeaders) = requestFuture(Verb.GET, payload, env)
  def deleteWithFuture[A <: AnyRef](payload: A, env: RequestEnv = getDefaultHeaders) = requestFuture(Verb.DELETE, payload, env)
  def putWithFuture[A <: AnyRef](payload: A, env: RequestEnv = getDefaultHeaders) = requestFuture(Verb.PUT, payload, env)
  def postWithFuture[A <: AnyRef](payload: A, env: RequestEnv = getDefaultHeaders) = requestFuture(Verb.POST, payload, env)

  protected def makeCallbackIntoFuture[A <: AnyRef](fun: (A => Unit) => Unit): () => A = {
    val mail = new scala.actors.Channel[A]
    def callback(response: A): Unit = mail ! response
    fun(callback)
    () => mail.receive { case x => x.asInstanceOf[A] }
  }

}