/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.client.impl

import org.totalgrid.reef.client.proto.Envelope
import net.agileautomata.executor4s.Executor
import org.totalgrid.reef.client.{ Promise, RequestHeaders }
import org.totalgrid.reef.client.operations.Response
import org.totalgrid.reef.client.proto.Envelope.Verb
import org.totalgrid.reef.client.operations.impl.FuturePromise
import org.totalgrid.reef.client.operations.scl.ScalaResponse

trait RequestSender {
  def request[A](verb: Envelope.Verb, payload: A, headers: RequestHeaders, requestExecutor: Executor): Promise[Response[A]]
}

abstract class RequestSenderImpl(manager: RequestManager, registry: ServiceRegistryLookup) extends RequestSender {
  def request[A](verb: Verb, payload: A, headers: RequestHeaders, requestExecutor: Executor): Promise[Response[A]] = {

    ClassLookup(payload).flatMap(registry.getServiceOption) match {
      case Some(info) => manager.request(verb, payload, headers, info, requestExecutor)
      case None =>
        // TODO: one-line this in FuturePromise/ScalaPromise
        val promise = FuturePromise.open[Response[A]](requestExecutor)
        promise.setSuccess(ScalaResponse.failure[A](Envelope.Status.BAD_REQUEST, "Message type unrecognized: " + payload))
        promise
    }
  }
}
