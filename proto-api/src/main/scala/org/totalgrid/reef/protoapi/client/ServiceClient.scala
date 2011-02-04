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

import org.totalgrid.reef.util.Logging
import org.totalgrid.reef.proto.Envelope
import com.google.protobuf.GeneratedMessage

/** Provides a thick interface full of helper functions via implement the single abstract request function
 */
trait ServiceClient extends SyncOperations with AsyncOperations with ScatterGatherOperations with Logging {

  /** The default request headers */
  var defaultEnv: Option[RequestEnv] = None

  /** Set the default request headers */
  def setDefaultEnv(env: RequestEnv) = defaultEnv = Some(env)

  /**
   *    Implements a synchronous request in terms of an asynchronous request
   */
  override def request[A <: GeneratedMessage](verb: Envelope.Verb, payload: A, env: RequestEnv): List[A] = {
    val future = makeCallbackIntoFuture { asyncRequest(verb, payload, env) }
    convert(future()) match {
      case MultiResponse(list) => list
      case x: Failure => throw x.toException
    }
  }

  /**
   *
   */
  private def makeCallbackIntoFuture[T](fun: (T => Unit) => Unit): () => T = {
    val mail = new scala.actors.Channel[T]
    def callback(response: T): Unit = mail ! response
    fun(callback)
    () => mail.receive { case x => x.asInstanceOf[T] }
  }
}