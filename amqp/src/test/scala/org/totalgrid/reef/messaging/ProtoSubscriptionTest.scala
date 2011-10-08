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

import mock.AMQPFixture
import org.totalgrid.reef.broker.embedded.EmbeddedBrokerConnection
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import serviceprovider.ServiceSubscriptionHandler
import org.totalgrid.reef.executor.mock.InstantExecutor
import org.totalgrid.reef.japi.Envelope
import org.totalgrid.reef.sapi._
import client.{ Event, Subscription, Response }
import org.totalgrid.reef.messaging.sync.SyncSubscriptionHandler
import org.totalgrid.reef.sapi.service.SyncServiceBase
import net.agileautomata.commons.testing.SynchronizedList

@RunWith(classOf[JUnitRunner])
class MockProtoSubscriptionTest extends ProtoSubscriptionTestBase {
  def setupTest(test: AmqpClientSession => Unit) {

    val mock = new EmbeddedBrokerConnection

    AMQPFixture.sync(mock, true) { syncAmqp =>
      val client = new AmqpClientSession(syncAmqp, servicelist, 10000)

      val pub = new SyncSubscriptionHandler(syncAmqp.getChannel, exchange + "_events")

      val executor = new InstantExecutor

      syncAmqp.bindService(exchange, (new DemoSubscribeService(pub)).respond, executor, competing = true)

      test(client)
    }
  }
}

abstract class ProtoSubscriptionTestBase extends FunSuite with ShouldMatchers {

  val exchange = TestDescriptors.requestHeader.id
  val exchangeMap: ServiceList.ServiceMap = Map(
    classOf[Envelope.RequestHeader] -> ServiceInfo.get(TestDescriptors.requestHeader))
  val servicelist = new ServiceListOnMap(exchangeMap)

  def setupTest(test: AmqpClientSession => Unit)

  class DemoSubscribeService(subHandler: ServiceSubscriptionHandler) extends SyncServiceBase[Envelope.RequestHeader] {

    // list of entries
    private var entries = List.empty[Envelope.RequestHeader]

    private def handleSub(req: Envelope.RequestHeader, env: BasicRequestHeaders) =
      env.subQueue.foreach(subHandler.bind(_, req.getKey, req))

    private def publish(evt: Envelope.Event, changes: List[Envelope.RequestHeader]) =
      changes.foreach(msg => subHandler.publish(evt, msg, msg.getKey))

    val descriptor = TestDescriptors.requestHeader

    override def get(req: Envelope.RequestHeader, env: BasicRequestHeaders) = {
      handleSub(req, env)

      val response = req.getKey match {
        case "*" => entries
        case s => entries.find(_.getKey == s).toList
      }

      Response(Envelope.Status.OK, response)
    }

    override def post(req: Envelope.RequestHeader, env: BasicRequestHeaders) = put(req, env)

    override def put(req: Envelope.RequestHeader, env: BasicRequestHeaders) = {
      handleSub(req, env)

      val status = entries.find(_.getKey == req.getKey) match {
        case None =>
          publish(Envelope.Event.ADDED, List(req))
          Envelope.Status.CREATED
        case Some(existing) =>
          publish(Envelope.Event.MODIFIED, List(req))
          Envelope.Status.UPDATED
      }
      entries = entries.filterNot(_.getKey == req.getKey) ::: List(req)

      Response(status, List(req))
    }

    override def delete(req: Envelope.RequestHeader, env: BasicRequestHeaders) = {
      handleSub(req, env)
      val _entries = entries.partition(_.getKey == req.getKey)
      entries = _entries._2
      publish(Envelope.Event.REMOVED, _entries._1)

      Response(Envelope.Status.DELETED, _entries._1)
    }
  }

  test("ProtoClient ISubscriptions") {
    setupTest { client =>

      def header(key: String, value: String) = Envelope.RequestHeader.newBuilder.setKey(key).setValue(value).build()

      val updates = new SynchronizedList[Event[Envelope.RequestHeader]]

      def prepend(event: Event[Envelope.RequestHeader]) = updates.append(event)
      val sub = client.addSubscription(TestDescriptors.requestHeader.getKlass).start(prepend _)

      import Subscription.convertSubscriptionToRequestEnv
      val integrity = client.get(header("*", "*"), sub).await().expectMany()

      val created = client.put(header("magic", "abra")).await().expectOne
      val modified = client.put(header("magic", "cadabra")).await().expectOne
      val deleted = client.delete(header("magic", "cadabra")).await().expectOne

      val expected = List(
        Event(Envelope.Event.REMOVED, deleted),
        Event(Envelope.Event.MODIFIED, modified),
        Event(Envelope.Event.ADDED, created))

      updates shouldEqual (expected) within (5000)
    }
  }

}
