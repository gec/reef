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
import org.totalgrid.reef.api.ServiceTypes._

import ProtoConversions._
import org.totalgrid.reef.api.{ Envelope, RequestEnv, IDestination, AnyNode }

trait AsyncOperations {

  self: DefaultHeaders =>

  /** All other async functions can be reduced to this
   */
  def asyncRequest[A <: AnyRef](verb: Envelope.Verb, payload: A, env: RequestEnv = getDefaultHeaders, dest: IDestination = AnyNode)(callback: MultiResult[A] => Unit)

  /* --- Thick Interface --- All function prevalidate the response code so the client doesn't have to check it */
  def asyncGet[A <: AnyRef](payload: A, env: RequestEnv = getDefaultHeaders, dest: IDestination = AnyNode)(callback: MultiResult[A] => Unit): Unit = asyncRequest(Envelope.Verb.GET, payload, env, dest)(callback)

  def asyncDelete[A <: AnyRef](payload: A, env: RequestEnv = getDefaultHeaders, dest: IDestination = AnyNode)(callback: MultiResult[A] => Unit): Unit = asyncRequest(Envelope.Verb.DELETE, payload, env, dest)(callback)

  def asyncPost[A <: AnyRef](payload: A, env: RequestEnv = getDefaultHeaders, dest: IDestination = AnyNode)(callback: MultiResult[A] => Unit): Unit = asyncRequest(Envelope.Verb.POST, payload, env, dest)(callback)

  def asyncPut[A <: AnyRef](payload: A, env: RequestEnv = getDefaultHeaders, dest: IDestination = AnyNode)(callback: MultiResult[A] => Unit): Unit = asyncRequest(Envelope.Verb.PUT, payload, env, dest)(callback)

  def asyncGetOne[A <: AnyRef](payload: A, env: RequestEnv = getDefaultHeaders, dest: IDestination = AnyNode)(callback: SingleResult[A] => Unit): Unit = asyncGet(payload, env, dest)(callback)

  def asyncDeleteOne[A <: AnyRef](payload: A, env: RequestEnv = getDefaultHeaders, dest: IDestination = AnyNode)(callback: SingleResult[A] => Unit): Unit = asyncDelete(payload, env, dest)(callback)

  def asyncPutOne[A <: AnyRef](payload: A, env: RequestEnv = getDefaultHeaders, dest: IDestination = AnyNode)(callback: SingleResult[A] => Unit): Unit = asyncPut(payload, env, dest)(callback)

  def asyncPostOne[A <: AnyRef](payload: A, env: RequestEnv = getDefaultHeaders, dest: IDestination = AnyNode)(callback: SingleResult[A] => Unit): Unit = asyncPost(payload, env, dest)(callback)

}