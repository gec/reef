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
package org.totalgrid.reef.messaging.mock

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import org.totalgrid.reef.util.Timer

import org.totalgrid.reef.api._
import org.totalgrid.reef.api.scalaclient._
import ServiceHandlerHeaders.convertRequestEnvToServiceHeaders

@RunWith(classOf[JUnitRunner])
class MockProtoRegistryTest extends FunSuite with ShouldMatchers {

  test("SubscribeExceptionNoTimeout") {
    val reg = new MockProtoSubscriberRegistry {}
    intercept[NoSuchElementException] {
      reg.getAcceptor(classOf[Envelope.RequestHeader], 0)
    }
  }

  test("SubscribeExceptionWithTimeout") {
    val reg = new MockProtoSubscriberRegistry {}
    intercept[MatchError] {
      reg.getAcceptor(classOf[Envelope.RequestHeader], 1) //wait for a millisecond
    }
  }

  test("Subscriber") {
    val reg = new MockProtoSubscriberRegistry {}
    var list: List[Envelope.RequestHeader] = Nil
    reg.subscribe(Envelope.RequestHeader.parseFrom, "#") { x => list = x :: list }
    reg.getAcceptor(classOf[Envelope.RequestHeader])(Envelope.RequestHeader.newBuilder.setKey("").setValue("").build) //call the acceptor
    list.size should equal(1)
  }

  test("DelayedSubscribe") {
    val reg = new MockProtoSubscriberRegistry {}
    var list: List[Envelope.RequestHeader] = Nil
    Timer.delay(10) { // do this 10 ms from now on an unknown actor thread
      reg.subscribe(Envelope.RequestHeader.parseFrom, "#") { x => list = x :: list }
    }
    reg.getAcceptor(classOf[Envelope.RequestHeader])(Envelope.RequestHeader.newBuilder.setKey("").setValue("").build) //call the acceptor
    list.size should equal(1)

  }

  test("GetPublisherException") {
    val reg = new MockProtoPublisherRegistry {}
    intercept[NoSuchElementException] {
      reg.getMailbox(classOf[Envelope.RequestHeader])
    }
  }

  test("GetPublisher") {
    val reg = new MockProtoPublisherRegistry {}
    val pub = reg.publish((_: Envelope.RequestHeader) => "key")
    val mail = reg.getMailbox(classOf[Envelope.RequestHeader])
    pub(Envelope.RequestHeader.newBuilder.setKey("").setValue("").build)
    mail.receiveWithin(0) {
      case x: Envelope.RequestHeader =>
      case _ => assert(false)
    }

  }

  test("GetEventException") {
    val reg = new MockConnection {}
    intercept[MatchError] {
      reg.getEvent(classOf[Envelope.RequestHeader])
    }
  }

  test("DefineEventQueue") {
    val reg = new MockConnection {}
    reg.defineEventQueue(Envelope.RequestHeader.parseFrom, (x: Any) => x)
    reg.getEvent(classOf[Envelope.RequestHeader])
  }

  test("testMockProtoConsumerRespondTimeout") {
    val reg = new MockClientSession
    intercept[MatchError] { reg.respondWithTimeout[Int](1) { x => None } } //when we timeout, we should get a match error
  }

  test("MockProtoConsumerRequestTimeout") {
    val reg = new MockClientSession(1)
    val rsp = reg.put(Envelope.RequestHeader.newBuilder.setKey("").setValue("").build).await
    intercept[ResponseTimeoutException] { rsp.expectMany() }
    rsp.status should equal(Envelope.Status.RESPONSE_TIMEOUT)
  }

  // Do a full request/respond
  test("ConsumerRequestResponse") {
    val reg = new MockClientSession

    //fire off a read on an actor
    Timer.now {
      reg.put(Envelope.RequestHeader.newBuilder.setKey("").setValue("4").build)
    }

    reg.respond[Envelope.RequestHeader] { request: Request[Envelope.RequestHeader] =>
      request.verb should equal(Envelope.Verb.PUT)
      request.env.subQueue should equal(None)
      request.payload.getValue should equal("4")
      Some(Success(Envelope.Status.OK, List(request.payload)))
    }
  }

  // mock objects are not available until they are created
  test("MockServiceRegistryException") {
    val reg = new MockConnection {}
    intercept[NoSuchElementException] {
      reg.getMockClient
    }
  }

  // tests that once the consumer is requested, the mock will be available
  test("MockServiceRegistryLookupDefaultstoREQUEST") {
    val reg = new MockConnection {}
    reg.getClientSession()
    reg.getMockClient
  }

}

