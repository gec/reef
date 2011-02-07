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

import org.totalgrid.reef.protoapi.{ ProtoServiceTypes, RequestEnv }
import ProtoServiceTypes._

import org.totalgrid.reef.proto.Envelope
import Envelope.Verb._

trait SyncOperations {

  // overridable
  protected def getRequestEnv: RequestEnv = new RequestEnv

  /**
   * Implement this function to widen the interface
   */
  def request[A <: GeneratedMessage](verb: Envelope.Verb, payload: A, env: RequestEnv = getRequestEnv): MultiResult[A]

  // helpers
  def requestOne[A <: GeneratedMessage](verb: Envelope.Verb, payload: A, env: RequestEnv = getRequestEnv): SingleResult[A] = expectsOne(request(verb, payload, env))

  def requestThrow[A <: GeneratedMessage](verb: Envelope.Verb, payload: A, env: RequestEnv = getRequestEnv): List[A] = throwOnFailure(request(verb, payload, env))

  def requestOneOrThrow[A <: GeneratedMessage](verb: Envelope.Verb, payload: A, env: RequestEnv = getRequestEnv): A = throwOnFailure(requestOne(verb, payload, env))

  def get[A <: GeneratedMessage](payload: A, env: RequestEnv = getRequestEnv): MultiResult[A] = request(GET, payload, env)

  def delete[A <: GeneratedMessage](payload: A, env: RequestEnv = getRequestEnv): MultiResult[A] = request(DELETE, payload, env)

  def post[A <: GeneratedMessage](payload: A, env: RequestEnv = getRequestEnv): MultiResult[A] = request(POST, payload, env)

  def put[A <: GeneratedMessage](payload: A, env: RequestEnv = getRequestEnv): MultiResult[A] = request(PUT, payload, env)

  def getOne[A <: GeneratedMessage](payload: A, env: RequestEnv = getRequestEnv): SingleResult[A] = expectsOne(get(payload, env))

  def deleteOne[A <: GeneratedMessage](payload: A, env: RequestEnv = getRequestEnv): SingleResult[A] = expectsOne(delete(payload, env))

  def postOne[A <: GeneratedMessage](payload: A, env: RequestEnv = getRequestEnv): SingleResult[A] = expectsOne(post(payload, env))

  def putOne[A <: GeneratedMessage](payload: A, env: RequestEnv = getRequestEnv): SingleResult[A] = expectsOne(put(payload, env))

  def getOrThrow[A <: GeneratedMessage](payload: A, env: RequestEnv = getRequestEnv): List[A] = throwOnFailure(get(payload, env))

  def deleteOrThrow[A <: GeneratedMessage](payload: A, env: RequestEnv = getRequestEnv): List[A] = throwOnFailure(delete(payload, env))

  def postOrThrow[A <: GeneratedMessage](payload: A, env: RequestEnv = getRequestEnv): List[A] = throwOnFailure(post(payload, env))

  def putOrThrow[A <: GeneratedMessage](payload: A, env: RequestEnv = getRequestEnv): List[A] = throwOnFailure(put(payload, env))

  def getOneOrThrow[A <: GeneratedMessage](payload: A, env: RequestEnv = getRequestEnv): A = throwOnFailure(getOne(payload, env))

  def deleteOneOrThrow[A <: GeneratedMessage](payload: A, env: RequestEnv = getRequestEnv): A = throwOnFailure(deleteOne(payload, env))

  def postOneOrThrow[A <: GeneratedMessage](payload: A, env: RequestEnv = getRequestEnv): A = throwOnFailure(postOne(payload, env))

  def putOneOrThrow[A <: GeneratedMessage](payload: A, env: RequestEnv = getRequestEnv): A = throwOnFailure(putOne(payload, env))

  protected def throwOnFailure[A](response: MultiResult[A]): List[A] = response match {
    case MultiSuccess(list) => list
    case x: Failure => throw x.toException
  }

  protected def throwOnFailure[A](response: SingleResult[A]): A = response match {
    case SingleSuccess(x) => x
    case x: Failure => throw x.toException
  }

}