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

import org.totalgrid.reef.proto.Envelope.Verb
import com.google.protobuf.GeneratedMessage

import org.totalgrid.reef.protoapi.{ ProtoServiceTypes, RequestEnv }
import ProtoServiceTypes._
import org.totalgrid.reef.protoapi.ProtoConversions._

trait SyncScatterGatherOperations extends FutureOperations {

  def requestScatterGather[T <: GeneratedMessage](verb: Verb, payloads: List[T], env: RequestEnv): List[MultiResult[T]] =
    payloads.map { p => requestFuture(verb, p, env) }.map { future => future() }

  def getScatterGather[T <: GeneratedMessage](payloads: List[T], env: RequestEnv = new RequestEnv): List[MultiResult[T]] = requestScatterGather(Verb.GET, payloads, env)
  def deleteScatterGather[T <: GeneratedMessage](payloads: List[T], env: RequestEnv = new RequestEnv): List[MultiResult[T]] = requestScatterGather(Verb.DELETE, payloads, env)
  def putScatterGather[T <: GeneratedMessage](payloads: List[T], env: RequestEnv = new RequestEnv): List[MultiResult[T]] = requestScatterGather(Verb.PUT, payloads, env)
  def postScatterGather[T <: GeneratedMessage](payloads: List[T], env: RequestEnv = new RequestEnv): List[MultiResult[T]] = requestScatterGather(Verb.POST, payloads, env)

  def getOneScatterGather[T <: GeneratedMessage](payloads: List[T], env: RequestEnv = new RequestEnv): List[SingleResult[T]] = getScatterGather(payloads, env)
  def deleteOneScatterGather[T <: GeneratedMessage](payloads: List[T], env: RequestEnv = new RequestEnv): List[SingleResult[T]] = deleteScatterGather(payloads, env)
  def putOneScatterGather[T <: GeneratedMessage](payloads: List[T], env: RequestEnv = new RequestEnv): List[SingleResult[T]] = putScatterGather(payloads, env)
  def postOneScatterGather[T <: GeneratedMessage](payloads: List[T], env: RequestEnv = new RequestEnv): List[SingleResult[T]] = postScatterGather(payloads, env)

}

