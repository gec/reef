package org.totalgrid.reef.client.javaimpl.fixture

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

import org.totalgrid.reef.client.sapi.service.SyncServiceBase

import org.totalgrid.reef.client.operations.Response

import org.totalgrid.reef.client.proto.Envelope
import org.totalgrid.reef.client.registration.EventPublisher

import org.totalgrid.reef.client.operations.scl.ScalaRequestHeaders._
import org.totalgrid.reef.client.RequestHeaders
import org.totalgrid.reef.client.operations.scl.ScalaResponse

class SomeIntegerIncrementService(handler: EventPublisher) extends SyncServiceBase[SomeInteger] {
  val descriptor = SomeIntegerTypeDescriptor

  final override def put(req: SomeInteger, headers: RequestHeaders): Response[ServiceType] = {
    val rsp = req.increment
    headers.subQueue.foreach(q => handler.bindQueueByClass(q, "#", req.getClass))
    handler.publishEvent(Envelope.SubscriptionEventType.MODIFIED, req.increment, "all")
    ScalaResponse.success(Envelope.Status.OK, rsp)
  }
}