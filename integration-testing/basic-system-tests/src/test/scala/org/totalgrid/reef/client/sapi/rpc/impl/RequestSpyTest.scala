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
package org.totalgrid.reef.client.sapi.rpc.impl

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.totalgrid.reef.client.sapi.rpc.impl.util.ServiceClientSuite
import org.totalgrid.reef.client.proto.Envelope.Verb
import org.totalgrid.reef.client.service.entity.EntityRelation
import org.totalgrid.reef.client.Promise
import org.totalgrid.reef.client.operations.{ Response, RequestListener }

@RunWith(classOf[JUnitRunner])
class RequestSpyTest extends ServiceClientSuite {

  class CountingRequestListener extends RequestListener {
    var count = 0
    def onRequest[A](verb: Verb, request: A, response: Promise[Response[A]]) {
      println("Request: " + verb + " " + request)
      count += 1
    }
    def reset() {
      count = 0
    }
  }

  test("CountingRequestSpy") {
    val spy = new CountingRequestListener
    session.getRequestListenerManager.addRequestListener(spy)

    val relations = List(new EntityRelation("feedback", "Point", false))

    val fromRoots = client.getEntityRelationsFromTypeRoots("Command", relations)

    spy.count should equal(1)
    spy.reset()

    val fromParents = client.getEntityRelationsForParents(fromRoots.map { _.getUuid }, relations)

    fromParents should equal(fromRoots)

    // N queries, one for each command and another for the batch
    spy.count should equal(fromRoots.size + 1)
  }
}