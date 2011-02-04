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
package org.totalgrid.reef.services

import org.scalatest.{ FunSuite, BeforeAndAfterAll, BeforeAndAfterEach }
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

//import org.totalgrid.reef.messaging.mock.AMQPFixture

//import org.squeryl.{ Schema, Table, KeyedEntity }
import org.squeryl.PrimitiveTypeMode._

//import org.totalgrid.reef.protoapi.ProtoServiceTypes._

import org.totalgrid.reef.models.ApplicationSchema
import org.totalgrid.reef.persistence.squeryl.{ DbConnector, DbInfo }
import org.totalgrid.reef.models.RunTestsInsideTransaction

import org.totalgrid.reef.proto.Processing._
import org.totalgrid.reef.proto.Model.{ Point, Entity }

import org.totalgrid.reef.services.core.EQ

@RunWith(classOf[JUnitRunner])
class MeasurementProcessorResourcesTest extends FunSuite with ShouldMatchers with BeforeAndAfterAll with BeforeAndAfterEach with RunTestsInsideTransaction {
  import org.totalgrid.reef.measproc.ProtoHelper._
  import ServiceResponseTestingHelpers._

  override def beforeAll() = DbConnector.connect(DbInfo.loadInfo("test"))

  override def beforeEach() = transaction { ApplicationSchema.reset }

  private def addPoint(pointName: String, devName: String): Entity = {
    val modelFac = new core.ModelFactories(new SilentEventPublishers, new core.SilentSummaryPoints)
    val service = new core.PointService(modelFac.points)

    // val logicalNode = Entity.newBuilder.setName(devName).addTypes("LogicalNode").build
    val device = transaction {
      EQ.findOrCreateEntity(devName, "LogicalNode")
    }
    val pp = Point.newBuilder.setName(pointName).build

    val point = one(service.put(pp))

    transaction {
      // TODO: add edge service
      val point_ent = EQ.findEntity(point.getEntity).get
      EQ.addEdge(device, point_ent, "source")

      EQ.entityToProto(device).build
    }
  }

  // TODO: reenable trigger tests
  /*test("Exercise Trigger Service") {
    val publisher = new SilentEventPublishers

    val node = addPoint("meas01", "dev1")
    addPoint("meas02", "dev2")

    val fac = new core.TriggerSetServiceModelFactory(publisher)
    val s = new core.TriggerSetService(fac)
    one(s.put(makeTrigger("meas01", "trigname1")))
    one(s.put(makeTrigger("meas01", "trigname2")))
    one(s.put(makeTrigger("meas01", "trigname3")))
    one(s.put(makeTrigger("meas02", "trigname1")))

    val gotten = one(s.get(Trigger.newBuilder.setTriggerName("trigname1").setPoint(makePoint("meas01")).build))
    // make sure the object has had all of the point and node fields filled out
    gotten.getPoint.getName should equal("meas01")
    gotten.getPoint.getNode.getUid should equal(node.getUid)

    many(2, s.get(Trigger.newBuilder.setTriggerName("trigname1").build))

    none(s.get(Trigger.newBuilder.setPoint(makePoint("meas03")).build))
    none(s.get(Trigger.newBuilder.setTriggerName("trigname1").setPoint(makePoint("meas03")).build))

    many(3, s.get(Trigger.newBuilder.setPoint(makePoint("meas01")).build))
    many(3, s.get(Trigger.newBuilder.setPoint(makePointByNodeUid(node.getUid)).build))
    many(3, s.get(Trigger.newBuilder.setPoint(makePointByNodeName(node.getName)).build))

    many(4, s.get(Trigger.newBuilder.setPoint(makePoint("*")).build))
    many(4, s.get(Trigger.newBuilder.setPoint(makePointByNodeUid("*")).build))
    many(4, s.get(Trigger.newBuilder.setPoint(makePointByNodeName("*")).build))
  }*/

  test("Exercise Overrides Service") {
    val publisher = new SilentEventPublishers

    val node = addPoint("meas01", "dev1")
    addPoint("meas02", "dev2")
    val fac = new core.OverrideConfigModelFactory(publisher)
    val s = new core.OverrideConfigService(fac)
    val put1 = one(s.put(MeasOverride.newBuilder.setPoint(makePoint("meas01")).setMeas(makeInt("meas01", 100)).build))
    val put2 = one(s.put(MeasOverride.newBuilder.setPoint(makePoint("meas01")).setMeas(makeInt("meas01", 999)).build))
    one(s.put(MeasOverride.newBuilder.setPoint(makePoint("meas02")).setMeas(makeInt("meas02", 888)).build))

    val gotten = one(s.get(MeasOverride.newBuilder.setPoint(makePoint("meas01")).build))
    // make sure the object has had all of the point and node fields filled out
    gotten.getPoint.getName should equal("meas01")
    //gotten.getPoint.getLogicalNode.getUid should equal(node.getUid)
    gotten.getMeas.getIntVal should equal(999)

    one(s.get(MeasOverride.newBuilder.setPoint(makePoint("meas01")).build))
    one(s.get(MeasOverride.newBuilder.setPoint(makePointByNodeUid(node.getUid)).build))
    one(s.get(MeasOverride.newBuilder.setPoint(makePointByNodeName(node.getName)).build))

    many(2, s.get(MeasOverride.newBuilder.setPoint(makePoint("*")).build))
    many(2, s.get(MeasOverride.newBuilder.setPoint(makePointByNodeUid("*")).build))
    many(2, s.get(MeasOverride.newBuilder.setPoint(makePointByNodeName("*")).build))
  }
}
