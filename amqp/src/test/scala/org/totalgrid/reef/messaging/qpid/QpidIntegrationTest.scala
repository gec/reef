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
package org.totalgrid.reef.messaging.qpid

import scala.collection.JavaConversions._

import org.totalgrid.reef.messaging.mock._
import org.totalgrid.reef.messaging.{ TestDescriptors, BrokerConnectionInfo, HeadersX2 }
import org.totalgrid.reef.util.Conversion.convertIntToTimes

import org.totalgrid.reef.api._
import org.totalgrid.reef.api.service.IServiceResponseCallback

import scala.concurrent.MailBox

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class QpidIntegrationTest extends FunSuite with ShouldMatchers {

  val payload = Envelope.RequestHeader.newBuilder.setKey("test").setValue("test").build
  val request = Envelope.ServiceRequest.newBuilder.setId("1").setVerb(Envelope.Verb.GET).setPayload(payload.toByteString).build
  val exchange = TestDescriptors.requestHeader.id
  val servicelist = new ServiceListOnMap(Map(classOf[Envelope.RequestHeader] -> ServiceInfo.get(TestDescriptors.requestHeader)))

  test("Timeout") {
    AMQPFixture.run(new BrokerConnectionInfo("127.0.0.1", 10000, "", "", ""), false) { amqp =>
      val client = amqp.getProtoClientSession(servicelist, 1000)
      intercept[ReefServiceException] {
        client.get(payload).await().expectMany()
      }
    }
  }

  // This is a functionally defined service that just echos the payload back 3x with an OK status
  def x3Service(request: Envelope.ServiceRequest, env: RequestEnv, callback: IServiceResponseCallback) = {
    val rsp = Envelope.ServiceResponse.newBuilder
    rsp.setStatus(Envelope.Status.OK)
    rsp.setId(request.getId)
    3.times { rsp.addPayload(request.getPayload) }
    callback.onResponse(rsp.build)
  }

  // same service except that it only responds to get, and only works with validated type Foo

  val serviceList = new ServiceListOnMap(Map(
    classOf[Envelope.RequestHeader] -> ServiceInfo.get(TestDescriptors.requestHeader)))

  test("SimpleServiceEchoSuccess") {
    AMQPFixture.run() { amqp =>

      amqp.bindService(exchange, x3Service) // listen for service requests with the echo service

      val serviceSend = amqp.getProtoClientSession(servicelist, 5000)

      // invoke the service future, and check that
      // response payload matches the request
      val payloads = serviceSend.get(payload).await().expectMany(3)
    }
  }

  test("ConvertingServiceEchoSuccess") {
    val request = Envelope.RequestHeader.newBuilder.setKey("test").setValue("test").build
    val service = new HeadersX2

    AMQPFixture.run() { amqp =>

      amqp.bindService(exchange, service.respond _) // this service just multplies the payload by 3	    	    	    
      val client = amqp.getProtoClientSession(servicelist, 10000)

      // invoke the service future, and check that
      // response payload matches the request
      val payloads = client.get(request).await().expectMany(2)
      payloads.foreach { _.getKey should equal(request.getKey) }
    }
  }

  test("ObservableSubscription") {

    AMQPFixture.run() { amqp =>

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

  def respondWithServiceName(serviceNum: Long)(request: Envelope.ServiceRequest, env: RequestEnv, callback: IServiceResponseCallback) {
    val rsp = Envelope.ServiceResponse.newBuilder
    rsp.setStatus(Envelope.Status.OK)
    rsp.setId(request.getId)
    rsp.addPayload(Envelope.RequestHeader.newBuilder.setKey("test").setValue(serviceNum.toString).build.toByteString)
    callback.onResponse(rsp.build)
  }

  test("Competing Consumers") {
    AMQPFixture.run() { amqp =>

      val services = 5
      val runs = 50
      var counts = scala.collection.mutable.Map.empty[Long, Int]

      for (i <- 1 to services) yield {
        amqp.bindService(exchange, respondWithServiceName(i), competing = true)
        counts(i) = 0
      }

      val serviceSend = amqp.getProtoClientSession(servicelist, 10000)

      for (i <- 1 to runs) yield {
        val payloads = serviceSend.get(payload).await().expectMany()
        payloads.size should equal(1)
        // collect the server names for later analysis
        counts(payloads.head.getValue.toInt) += 1
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

      val serviceSend = amqp.getProtoClientSession(exchangeName, 100, Example.Foo.parseFrom)
      intercept[Exception] {
        serviceSend.getOne(request)
      }

      amqp.bindService(exchangeName, x3Service _)

      serviceSend.get(request).size should equal(3)
    }
  }*/

}

