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

import org.totalgrid.reef.protoapi.{ ProtoServiceTypes, RequestEnv }
import ProtoServiceTypes._

import org.totalgrid.reef.proto.Envelope.Verb

import com.google.protobuf.GeneratedMessage

import org.totalgrid.reef.protoapi.ProtoConversions._ //implicits

trait AsyncScatterGatherOperations extends AsyncOperations {

  def requestAsyncScatterGather[T <: GeneratedMessage](verb: Verb, payloads: List[T], env: RequestEnv)(callback: List[MultiResult[T]] => Unit) {

    // the results we're collecting and a counter
    val map = new java.util.concurrent.ConcurrentHashMap[Int, MultiResult[T]]
    val latch = new java.util.concurrent.CountDownLatch(payloads.size)

    def gather(idx: Int)(rsp: MultiResult[T]) {
      map.put(idx, rsp)
      latch.countDown()
      if (latch.getCount == 0) callback(payloads.indices.map(i => map.get(i)).toList) //last callback orders and calls the callback
    }

    payloads.zipWithIndex.foreach { case (p, i) => asyncRequest(verb, p, env)(gather(i)) }
  }

  def asyncGetScatterGather[T <: GeneratedMessage](payloads: List[T], env: RequestEnv = new RequestEnv)(callback: List[MultiResult[T]] => Unit) =
    requestAsyncScatterGather(Verb.GET, payloads, env)(callback)

  def asyncPutScatterGather[T <: GeneratedMessage](payloads: List[T], env: RequestEnv = new RequestEnv)(callback: List[MultiResult[T]] => Unit) =
    requestAsyncScatterGather(Verb.PUT, payloads, env)(callback)

  def asyncPostScatterGather[T <: GeneratedMessage](payloads: List[T], env: RequestEnv = new RequestEnv)(callback: List[MultiResult[T]] => Unit) =
    requestAsyncScatterGather(Verb.POST, payloads, env)(callback)

  def asyncDeleteScatterGather[T <: GeneratedMessage](payloads: List[T], env: RequestEnv = new RequestEnv)(callback: List[MultiResult[T]] => Unit) =
    requestAsyncScatterGather(Verb.DELETE, payloads, env)(callback)

  def asyncGetOneScatterGather[T <: GeneratedMessage](payloads: List[T], env: RequestEnv = new RequestEnv)(callback: List[SingleResult[T]] => Unit) =
    asyncGetScatterGather(payloads, env)(callback)

  def asyncPutOneScatterGather[T <: GeneratedMessage](payloads: List[T], env: RequestEnv = new RequestEnv)(callback: List[SingleResult[T]] => Unit) =
    asyncPutScatterGather(payloads, env)(callback)

  def asyncPostOneScatterGather[T <: GeneratedMessage](payloads: List[T], env: RequestEnv = new RequestEnv)(callback: List[SingleResult[T]] => Unit) =
    asyncPostScatterGather(payloads, env)(callback)

  def asyncDeleteOneScatterGather[T <: GeneratedMessage](payloads: List[T], env: RequestEnv = new RequestEnv)(callback: List[SingleResult[T]] => Unit) =
    asyncDeleteScatterGather(payloads, env)(callback)

}

