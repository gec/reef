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
package org.totalgrid.reef.protoapi.client

import com.google.protobuf.GeneratedMessage

import org.totalgrid.reef.proto.Envelope
import org.totalgrid.reef.util.Logging
import org.totalgrid.reef.protoapi.RequestEnv
import org.totalgrid.reef.protoapi.ProtoServiceTypes._

import org.totalgrid.reef.protoapi.ProtoConversions._

trait AsyncOperations extends Logging {

  /** All other async functions can be reduced to this
   */
  def asyncRequest[A <: GeneratedMessage](verb: Envelope.Verb, payload: A, env: RequestEnv)(callback: MultiResult[A] => Unit)

  /* --- Thick Interface --- All function prevalidate the response code so the client doesn't have to check it */
  def asyncGet[A <: GeneratedMessage](payload: A, env: RequestEnv = new RequestEnv)(callback: MultiResult[A] => Unit): Unit = asyncRequest(Envelope.Verb.GET, payload, env)(callback)

  def asyncDelete[A <: GeneratedMessage](payload: A, env: RequestEnv = new RequestEnv)(callback: MultiResult[A] => Unit): Unit = asyncRequest(Envelope.Verb.DELETE, payload, env)(callback)

  def asyncPost[A <: GeneratedMessage](payload: A, env: RequestEnv = new RequestEnv)(callback: MultiResult[A] => Unit): Unit = asyncRequest(Envelope.Verb.POST, payload, env)(callback)

  def asyncPut[A <: GeneratedMessage](payload: A, env: RequestEnv = new RequestEnv)(callback: MultiResult[A] => Unit): Unit = asyncRequest(Envelope.Verb.PUT, payload, env)(callback)

  def asyncGetOne[A <: GeneratedMessage](payload: A, env: RequestEnv = new RequestEnv)(callback: SingleResult[A] => Unit): Unit = asyncGet(payload, env)(callback)

  def asyncDeleteOne[A <: GeneratedMessage](payload: A, env: RequestEnv = new RequestEnv)(callback: SingleResult[A] => Unit): Unit = asyncDelete(payload, env)(callback)

  def asyncPutOne[A <: GeneratedMessage](payload: A, env: RequestEnv = new RequestEnv)(callback: SingleResult[A] => Unit): Unit = asyncPut(payload, env)(callback)

  def asyncPostOne[A <: GeneratedMessage](payload: A, env: RequestEnv = new RequestEnv)(callback: SingleResult[A] => Unit): Unit = asyncPost(payload, env)(callback)

}