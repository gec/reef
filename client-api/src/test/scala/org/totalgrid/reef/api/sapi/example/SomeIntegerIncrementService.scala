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
package org.totalgrid.reef.api.sapi.example

import org.totalgrid.reef.api.sapi.client.rest.SubscriptionHandler
import org.totalgrid.reef.api.sapi.service.SyncServiceBase

import org.totalgrid.reef.api.sapi.client.{ Response, BasicRequestHeaders }
import org.totalgrid.reef.api.japi.Envelope
import org.totalgrid.reef.api.japi.Envelope.Event

class SomeIntegerIncrementService(handler: SubscriptionHandler) extends SyncServiceBase[SomeInteger] {
  val descriptor = SomeIntegerTypeDescriptor

  final override def put(req: SomeInteger, headers: BasicRequestHeaders): Response[ServiceType] = {
    val rsp = req.increment
    headers.subQueue.foreach(q => handler.bindQueueByClass(q, "#", req.getClass))
    handler.publishEvent(Event.MODIFIED, req.increment, "all")
    Response(Envelope.Status.OK, rsp)
  }
}