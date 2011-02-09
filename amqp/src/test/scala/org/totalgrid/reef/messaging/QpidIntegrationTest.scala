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
package org.totalgrid.reef.messaging

import javabridge.Deserializers
import org.totalgrid.reef.proto.{ Envelope, Example }

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import scala.collection.JavaConversions._

import scala.concurrent.MailBox

import org.totalgrid.reef.protoapi.{ ProtoServiceException, RequestEnv, ProtoServiceTypes }
import ProtoServiceTypes.Response
import org.totalgrid.reef.messaging.mock._

import org.totalgrid.reef.util.Conversion.convertIntToTimes

@RunWith(classOf[JUnitRunner])
class QpidIntegrationTest extends FunSuite with ShouldMatchers {

  val payload = Example.Foo.newBuilder.build
  val request = Envelope.ServiceRequest.newBuilder.setId("1").setVerb(Envelope.Verb.GET).setPayload(payload.toByteString).build
  val exchange = "integration.test"
  val servicelist = new ServiceListOnMap(Map(classOf[Example.Foo] -> ServiceInfo.get(exchange, Deserializers.foo)))

  test("Timeout") {
    AMQPFixture.run(new BrokerConnectionInfo("127.0.0.1", 10000, "", "", ""), false) { amqp =>
      val client = amqp.getProtoServiceClient(servicelist, 1000)
      intercept[ProtoServiceException] {
        client.getOrThrow(payload)
      }
    }
  }

  // This is a functionally defined service that just echos the payload back 3x with an OK status
  def x3Service(request: Envelope.ServiceRequest, env: RequestEnv): Envelope.ServiceResponse = {
    val rsp = Envelope.ServiceResponse.newBuilder
    rsp.setStatus(Envelope.Status.OK)
    rsp.setId(request.getId)
    3.times { rsp.addPayload(request.getPayload) }
    rsp.build
  }

  // same service except that it only responds to get, and only works with validated type Foo
  class FooServiceX3 extends ProtoServiceable[Example.Foo] {
    override def deserialize(bytes: Array[Byte]) = Example.Foo.parseFrom(bytes)
    override def get(foo: Example.Foo, env: RequestEnv) = Response(Envelope.Status.OK, "", List(foo, foo, foo))
    def put(req: Example.Foo, env: RequestEnv) = noVerb("put")
    def delete(req: Example.Foo, env: RequestEnv) = noVerb("delete")
    def post(req: Example.Foo, env: RequestEnv) = noVerb("post")
  }

  val serviceList = new ServiceListOnMap(Map(
    classOf[Example.Foo] -> ServiceInfo.get(exchange, Deserializers.foo)))

  test("SimpleServiceEchoSuccess") {
    AMQPFixture.run { amqp =>

      amqp.bindService(exchange, x3Service _) // listen for service requests with the echo service

      val serviceSend = amqp.getProtoServiceClient(servicelist, 1000)

      // invoke the service future, and check that
      // response payload matches the request
      val payloads = serviceSend.getOrThrow(payload)
      payloads.size should equal(3)
    }
  }

  test("ConvertingServiceEchoSuccess") {
    val request = Example.Foo.newBuilder.setNum(42).build
    val service = new FooServiceX3

    AMQPFixture.run { amqp =>

      amqp.bindService(exchange, service.respond _) // this service just multplies the payload by 3	    	    	    
      val client = amqp.getProtoServiceClient(servicelist, 10000)

      // invoke the service future, and check that
      // response payload matches the request
      val payloads = client.getOrThrow(request)
      payloads.size should equal(3)
      payloads.foreach { _.getNum should equal(request.getNum) }
    }
  }

  test("ObservableSubscription") {

    AMQPFixture.run { amqp =>

      val mail = new MailBox

      val cnt = 100 //stress the broker by creating 100 sessions

      cnt.times {
        amqp.subscribe(exchange, Envelope.ServiceRequest.parseFrom(_: Array[Byte]), (x: Envelope.ServiceRequest) => {}).resubscribe {
          mail send _
        }
      }

      cnt.times {
        mail.receiveWithin(5000) {
          case x: String =>
          case _ => assert(false)
        }
      }

    }
  }

  def respondWithServiceName(serviceNum: Long, request: Envelope.ServiceRequest, env: RequestEnv): Envelope.ServiceResponse = {
    val rsp = Envelope.ServiceResponse.newBuilder
    rsp.setStatus(Envelope.Status.OK)
    rsp.setId(request.getId)
    rsp.addPayload(Example.Foo.newBuilder.setNum(serviceNum).build.toByteString)
    rsp.build
  }

  test("Competing Consumers") {
    AMQPFixture.run { amqp =>

      val services = 5
      val runs = 50
      var counts = scala.collection.mutable.Map.empty[Long, Int]

      for (i <- 1 to services) yield {
        amqp.bindService(exchange, respondWithServiceName(i, _, _), true)
        counts(i) = 0
      }

      val serviceSend = amqp.getProtoServiceClient(servicelist, 10000)

      for (i <- 1 to runs) yield {
        val payloads = serviceSend.getOrThrow(payload)
        payloads.size should equal(1)
        // collect the server names for later analysis
        counts(payloads.head.getNum) += 1
      }

      // println(counts)
      // make sure all of the services were used
      counts.size should equal(services)
      // make sure the services were all similarly used (round robinish)
      assert(counts.forall(p => p._2 >= runs / services - services && p._2 <= runs / services + services))
    }
  }

  /*test("Exchange declaration after failure is noticed") {
    // TODO: have exchange declaration test point at Qpid when session exception bug fixed
    AMQPFixture.mock(true) { amqp =>
      val request = Example.Foo.newBuilder.setNum(42).build

      val exchangeName = java.util.UUID.randomUUID.toString

      val serviceSend = amqp.getProtoServiceClient(exchangeName, 100, Example.Foo.parseFrom)
      intercept[Exception] {
        serviceSend.getOne(request)
      }

      amqp.bindService(exchangeName, x3Service _)

      serviceSend.get(request).size should equal(3)
    }
  }*/

}

