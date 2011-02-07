/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Green Energy Corp licenses this file
 * to you under the GNU Affero General Public License Version 3.0
 * (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.services.core

import org.totalgrid.reef.util.SyncVar
import org.totalgrid.reef.reactor.mock.InstantReactor

import org.totalgrid.reef.messaging.mock.AMQPFixture
import org.totalgrid.reef.messaging.{ AMQPProtoFactory, AMQPProtoRegistry, ServicesList }

import org.totalgrid.reef.proto.Measurements._
import org.totalgrid.reef.proto.Model.{ Point => PointProto, Entity => EntityProto }
import org.totalgrid.reef.util.BlockingQueue

import org.totalgrid.reef.protoapi.{ RequestEnv, ServiceHandlerHeaders, ProtoServiceTypes }
import ProtoServiceTypes.Event

//implicits
import ServiceHandlerHeaders.convertRequestEnvToServiceHeaders
import org.squeryl.PrimitiveTypeMode._

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class PointServiceIntegrationTest extends EndpointRelatedTestBase {

  class PointFixture(amqp: AMQPProtoFactory) extends CoordinatorFixture(amqp, true) {

    val registry = new AMQPProtoRegistry(amqp, 5000, ServicesList.getServiceInfo)

    val pointClient = registry.getServiceClient(PointProto.parseFrom _)

    val entityClient = registry.getServiceClient(EntityProto.parseFrom _)
    val parentEntity = entityClient.putOneOrThrow(EntityProto.newBuilder.setName("test").addTypes("LogicalNode").build)

    val measPublish = amqp.send("measurement")

    // add a summary device and make sure it has an fep/measproc
    addDevice("summary", "abnormals")
    addFep("fep")
    addMeasProc("meas")

    val syncs = listenForMeasurements("meas")

    val abnormalThunker = new PointAbnormalsThunker(modelFac.points, new SummaryPointPublisher(amqp))
    abnormalThunker.addAMQPConsumers(amqp, new InstantReactor {})

    def addPoint(proto: PointProto) = {
      pointClient.putOneOrThrow(proto.toBuilder.setLogicalNode(parentEntity).build)
    }
    val changedPoints = new BlockingQueue[PointProto]

    def namedPoint(name: String) = {
      PointProto.newBuilder.setName(name).build
    }
    def abnormalPoint(abnormal: Boolean) = {
      PointProto.newBuilder.setAbnormal(abnormal).build
    }

    def subscribePoints(req: PointProto) = {
      val eventQueueName = new SyncVar("")
      val pointSource = amqp.getEventQueue(PointProto.parseFrom, { evt: Event[PointProto] => changedPoints.push(evt.result) }, { q => eventQueueName.update(q) })

      // wait for the queue name to get populated (actor startup delay)
      eventQueueName.waitWhile("")

      val env = new RequestEnv
      env.setSubscribeQueue(eventQueueName.current)
      pointClient.getOrThrow(req, env)
    }

    def nextNotification(timeout: Int = 5000) = {
      changedPoints.pop(timeout)
    }
    def assertNoNotifications() {
      changedPoints.waitUntil(1, 50) should equal(false)
    }

    def publishMeasurement(name: String, abnormal: Boolean) {
      import org.totalgrid.reef.measproc.ProtoHelper._
      val m = if (abnormal) {
        updateQuality(makeAnalog(name, 999), makeAbnormalQuality)
      } else {
        makeAnalog(name, 50)
      }
      measPublish(m, "test_point")
    }
    def waitForValue(name: String, value: Long) {
      syncs.waitFor({ l: List[(String, MeasurementBatch)] => getValue(name) == value }, 5000)
    }
  }

  test("Abnormal updated on general subscription") {
    AMQPFixture.mock(true) { amqp =>
      val fix = new PointFixture(amqp)

      fix.waitForValue("summary.abnormals", 0)

      // populate a test point (by default its not abnormal)
      fix.addPoint(fix.namedPoint("test_point"))

      // subscribe to test_point updates
      val points = fix.subscribePoints(fix.namedPoint("test_point"))
      points.size should equal(1)

      // check that we havent seen any events
      fix.assertNoNotifications()

      // simulate a measurement that is not abnormal
      fix.publishMeasurement("test_point", false)

      // still havent seen an event
      fix.assertNoNotifications()

      // simulate an abnormal measurement 
      fix.publishMeasurement("test_point", true)

      // we should have gotten an event for that point with abnormal set
      fix.nextNotification().getAbnormal should equal(true)
      fix.waitForValue("summary.abnormals", 1)

      // send another measurement with the abnormal still set, shouldn't generate a new event
      fix.publishMeasurement("test_point", true)
      fix.assertNoNotifications()

      // simulate a measurement with abnormal set to false
      fix.publishMeasurement("test_point", false)

      // check that we get an event with the abnormal set to false
      fix.nextNotification().getAbnormal should equal(false)
      fix.waitForValue("summary.abnormals", 0)
    }
  }

  test("Abnormal updated on abnormal subscription") {
    AMQPFixture.mock(true) { amqp =>
      val fix = new PointFixture(amqp)

      fix.waitForValue("summary.abnormals", 0)

      // start with one ok point, one abnormal point
      fix.addPoint(fix.namedPoint("abnormal_point"))
      fix.publishMeasurement("abnormal_point", true)
      fix.addPoint(fix.namedPoint("regular_point"))

      fix.waitForValue("summary.abnormals", 1)

      // subscribe to abnormal points
      val points = fix.subscribePoints(fix.abnormalPoint(true))
      // should only get the abnormal point
      points.size should equal(1)
      points.head.getName should equal("abnormal_point")
      fix.assertNoNotifications()

      // simulate measurement with same abnormalness, no events
      fix.publishMeasurement("regular_point", false)
      fix.assertNoNotifications()

      // simulate regular => abnormal
      fix.publishMeasurement("regular_point", true)

      // show we get the abnormal point
      val regNowAbnormal = fix.nextNotification()
      regNowAbnormal.getName should equal("regular_point")
      regNowAbnormal.getAbnormal should equal(true)

      fix.waitForValue("summary.abnormals", 2)

      // simulate abnormal => regular
      fix.publishMeasurement("regular_point", false)

      // show we get the non-abnormal message
      val regBackToNormal = fix.nextNotification()
      regBackToNormal.getName should equal("regular_point")
      regBackToNormal.getAbnormal should equal(false)

      fix.waitForValue("summary.abnormals", 1)

      // simulate abnormal => regular on point that started abnormally
      fix.publishMeasurement("abnormal_point", false)

      val abBackToNormal = fix.nextNotification()
      abBackToNormal.getName should equal("abnormal_point")
      abBackToNormal.getAbnormal should equal(false)

      fix.waitForValue("summary.abnormals", 0)

      // check that a subscription done for abnormals when there are no abnormals give an empty initial set
      val allOk = fix.subscribePoints(fix.abnormalPoint(true))
      allOk.size should equal(0)
    }
  }
}