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
package org.totalgrid.reef.messaging

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.totalgrid.reef.japi.Envelope
import org.totalgrid.reef.japi.Envelope.Verb
import org.totalgrid.reef.promise.Promise
import org.totalgrid.reef.sapi.client.{ RequestSpy, Response }

@RunWith(classOf[JUnitRunner])
class RequestSpyHookTest extends AmqpClientSessionTestBase {

  class CountingRequestSpy extends RequestSpy {
    var count = 0
    def onRequestReply[A](verb: Verb, request: A, response: Promise[Response[A]]) = {
      count += 1
    }
  }

  val request = Envelope.RequestHeader.newBuilder.setKey("key").setValue("magic").build

  test("RequestSpy called") {
    setupTest(true) { (client, amqp) =>

      val spy = new CountingRequestSpy
      client.addRequestSpy(spy)

      client.get(request).await().expectMany()
      spy.count should equal(1)

      client.get(request).await().expectMany()
      spy.count should equal(2)

      client.removeRequestSpy(spy)

      client.get(request).await().expectMany()
      spy.count should equal(2)
    }
  }

  case class Request[A](verb: Verb, req: A, response: Promise[Response[A]])

  test("Request Spy called with right verb") {
    setupTest(true) { (client, amqp) =>

      val spy = new RequestSpy {
        var results = List.empty[Request[_]]
        def onRequestReply[A](verb: Verb, req: A, response: Promise[Response[A]]) = {
          results ::= Request(verb, req, response)
        }
      }
      client.addRequestSpy(spy)

      val promise = client.get(request)
      spy.results.head should equal(Request(Verb.GET, request, promise))

      val promise2 = client.put(request)
      spy.results.head should equal(Request(Verb.PUT, request, promise2))
    }
  }

}