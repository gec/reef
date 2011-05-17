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

import org.totalgrid.reef.messaging.mock._
import com.google.protobuf.ByteString

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.totalgrid.reef.api._
import org.totalgrid.reef.api.scalaclient.Response
import org.totalgrid.reef.api.service.{ IServiceResponseCallback, AsyncToSyncServiceAdapter }

object TestDescriptors {
  def requestHeader() = new ITypeDescriptor[Envelope.RequestHeader] {
    def serialize(typ: Envelope.RequestHeader): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = Envelope.RequestHeader.parseFrom(bytes)
    def getKlass = classOf[Envelope.RequestHeader]
  }

  def serviceNotification() = new ITypeDescriptor[Envelope.ServiceNotification] {
    def serialize(typ: Envelope.ServiceNotification): Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = Envelope.ServiceNotification.parseFrom(bytes)
    def getKlass = classOf[Envelope.ServiceNotification]
  }
}

class ServiceNotificationServiceX3 extends AsyncToSyncServiceAdapter[Envelope.ServiceNotification] {

  val descriptor = TestDescriptors.serviceNotification

  def get(foo: Envelope.ServiceNotification, env: RequestEnv) = Response(Envelope.Status.OK, List(foo, foo, foo))
  def put(req: Envelope.ServiceNotification, env: RequestEnv) = noPut
  def delete(req: Envelope.ServiceNotification, env: RequestEnv) = noDelete
  def post(req: Envelope.ServiceNotification, env: RequestEnv) = noPost
}

class HeadersX2 extends AsyncToSyncServiceAdapter[Envelope.RequestHeader] {

  val descriptor = TestDescriptors.requestHeader

  def deserialize(bytes: Array[Byte]) = Envelope.RequestHeader.parseFrom(bytes)

  def get(foo: Envelope.RequestHeader, env: RequestEnv) = Response(Envelope.Status.OK, List(foo, foo))
  def put(req: Envelope.RequestHeader, env: RequestEnv) = noPut
  def delete(req: Envelope.RequestHeader, env: RequestEnv) = noDelete
  def post(req: Envelope.RequestHeader, env: RequestEnv) = noPost
}

@RunWith(classOf[JUnitRunner])
class ProtoClientTest extends FunSuite with ShouldMatchers {

  val exchangeA = "test.protoClient.A"
  val exchangeB = "test.protoClient.B"

  val serviceList = new ServiceListOnMap(Map(
    classOf[Envelope.ServiceNotification] -> ServiceInfo.get(exchangeA, TestDescriptors.serviceNotification),
    classOf[Envelope.RequestHeader] -> ServiceInfo.get(exchangeB, TestDescriptors.requestHeader)))

  def setupTest(addServices: Boolean)(test: (ProtoClient, AMQPProtoFactory) => Unit) {
    val connection = new MockBrokerInterface

    // TODO: fix setupTest to use all async and all sync

    AMQPFixture.run(connection, true) { amqp =>

      if (addServices) {
        amqp.bindService(exchangeA, (new ServiceNotificationServiceX3).respond, competing = true)
        amqp.bindService(exchangeB, (new HeadersX2).respond, competing = true)
      }

      AMQPFixture.sync(connection, true) { syncAmqp =>
        val client = new ProtoClient(syncAmqp, serviceList, 10000)

        test(client, amqp)
      }
    }
  }

  test("ProtoClient handles multiple proto types") {
    setupTest(true) { (client, amqp) =>

      val notificationRequest = Envelope.ServiceNotification.newBuilder.setEvent(Envelope.Event.ADDED).setPayload(ByteString.copyFromUtf8("hi")).build
      client.get(notificationRequest).await().expectMany().size should equal(3)

      val headerRequest = Envelope.RequestHeader.newBuilder.setKey("key").setValue("magic").build
      client.get(headerRequest).await().expectMany().size should equal(2)

      intercept[UnknownServiceException] {
        val responseRequest = Envelope.ServiceResponse.newBuilder.setId("").setStatus(Envelope.Status.BAD_REQUEST).build
        client.get(responseRequest).await().expectMany()
      }
    }
  }

  test("Client throws exception when closed") {
    setupTest(true) { (client, amqp) =>
      client.close

      intercept[ServiceIOException] {
        val headerRequest = Envelope.RequestHeader.newBuilder.setKey("key").setValue("magic").build
        client.get(headerRequest).await().expectMany()
      }
    }
  }
  test("Client fails quickly when closed") {
    setupTest(false) { (client, amqp) =>

      def respond(req: Envelope.ServiceRequest, env: RequestEnv, callback: IServiceResponseCallback) = {
        // kill the client rather than returning any data
        client.close
      }

      // TODO: make bindService synchronous callback
      amqp.bindService(exchangeB, respond, competing = true)

      Thread.sleep(500)

      intercept[ResponseTimeoutException] {
        val headerRequest = Envelope.RequestHeader.newBuilder.setKey("key").setValue("magic").build
        client.get(headerRequest).await().expectMany()
      }
    }
  }
}