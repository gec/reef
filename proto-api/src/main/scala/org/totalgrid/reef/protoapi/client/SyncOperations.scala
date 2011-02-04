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

import org.totalgrid.reef.protoapi.{ ProtoServiceException, RequestEnv }
import org.totalgrid.reef.proto.Envelope

trait SyncOperations {

  // overridable
  protected def getRequestEnv: RequestEnv = new RequestEnv

  /**
   * Implement this function to widen the interface
   */
  def request[A <: GeneratedMessage](verb: Envelope.Verb, payload: A, env: RequestEnv = getRequestEnv): List[A]

  def get[A <: GeneratedMessage](payload: A, env: RequestEnv = getRequestEnv): List[A] = request(Envelope.Verb.GET, payload, env)
  def delete[A <: GeneratedMessage](payload: A, env: RequestEnv = getRequestEnv): List[A] = request(Envelope.Verb.DELETE, payload, env)
  def post[A <: GeneratedMessage](payload: A, env: RequestEnv = getRequestEnv): List[A] = request(Envelope.Verb.POST, payload, env)
  def put[A <: GeneratedMessage](payload: A, env: RequestEnv = getRequestEnv): List[A] = request(Envelope.Verb.PUT, payload, env)

  def getOne[A <: GeneratedMessage](payload: A, env: RequestEnv = getRequestEnv): A = oneResult(get(payload, env))
  def deleteOne[A <: GeneratedMessage](payload: A, env: RequestEnv = getRequestEnv): A = oneResult(delete(payload, env))
  def postOne[A <: GeneratedMessage](payload: A, env: RequestEnv = getRequestEnv): A = oneResult(post(payload, env))
  def putOne[A <: GeneratedMessage](payload: A, env: RequestEnv = getRequestEnv): A = oneResult(put(payload, env))

  private def oneResult[A <: GeneratedMessage](list: List[A]): A = list match {
    case List(x) => x
    case List(_, _) =>
      throw new ProtoServiceException("Expected one result but got + " + list.length, Envelope.Status.LOCAL_ERROR)
    case Nil =>
      throw new ProtoServiceException("Expected one result but got empty response", Envelope.Status.LOCAL_ERROR)
  }

}