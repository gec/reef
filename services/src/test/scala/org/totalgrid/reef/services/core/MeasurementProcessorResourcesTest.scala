/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the GNU Affero General Public License
 * Version 3.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.services.core

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import org.squeryl.PrimitiveTypeMode._

import org.totalgrid.reef.client.service.proto.Processing._
import org.totalgrid.reef.models.DatabaseUsingTestBase
import org.totalgrid.reef.models.EntityQuery
import org.totalgrid.reef.client.sapi.client.BasicRequestHeaders
import org.totalgrid.reef.client.service.proto.Model.{ PointType, Point, Entity }

import org.totalgrid.reef.services.{ SilentRequestContext, ServiceDependencies, ServiceResponseTestingHelpers }

@RunWith(classOf[JUnitRunner])
class MeasurementProcessorResourcesTest extends DatabaseUsingTestBase with SyncServicesTestHelpers {
  import org.totalgrid.reef.measproc.ProtoHelper._
  import ServiceResponseTestingHelpers._

  val edgeModel = new EntityEdgeServiceModel
  val context = new SilentRequestContext

  private def addPoint(pointName: String, devName: String): Entity = {
    val modelFac = new ModelFactories(new ServiceDependenciesDefaults(dbConnection))
    val service = sync(new PointService(modelFac.points))

    // val logicalNode = Entity.newBuilder.setName(devName).addTypes("LogicalNode").build

    val context = new SilentRequestContext

    val device = modelFac.entities.findOrCreate(context, devName, "LogicalNode" :: Nil, None)
    val pp = Point.newBuilder.setName(pointName).setUnit("raw").setType(PointType.ANALOG).build

    val point = service.put(pp).expectOne()

    // TODO: add edge service
    val point_ent = EntityQuery.findEntity(point.getEntity).get
    edgeModel.addEdge(context, device, point_ent, "source")

    EntityQuery.entityToProto(device).build
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
    gotten.getPoint.getNode.getUuid should equal(node.getUuid)

    many(2, s.get(Trigger.newBuilder.setTriggerName("trigname1").build))

    none(s.get(Trigger.newBuilder.setPoint(makePoint("meas03")).build))
    none(s.get(Trigger.newBuilder.setTriggerName("trigname1").setPoint(makePoint("meas03")).build))

    many(3, s.get(Trigger.newBuilder.setPoint(makePoint("meas01")).build))
    many(3, s.get(Trigger.newBuilder.setPoint(makePointByNodeId(node.getUuid)).build))
    many(3, s.get(Trigger.newBuilder.setPoint(makePointByNodeName(node.getName)).build))

    many(4, s.get(Trigger.newBuilder.setPoint(makePoint("*")).build))
    many(4, s.get(Trigger.newBuilder.setPoint(makePointByNodeId("*")).build))
    many(4, s.get(Trigger.newBuilder.setPoint(makePointByNodeName("*")).build))
  }*/

  test("Exercise Overrides Service") {
    val node = addPoint("meas01", "dev1")
    addPoint("meas02", "dev2")

    val s = sync(new OverrideConfigService(new OverrideConfigServiceModel))

    val headers = BasicRequestHeaders.empty.setUserName("user")

    // first NIS the points
    s.put(MeasOverride.newBuilder.setPoint(makePoint("meas01")).build, headers).expectOne
    s.put(MeasOverride.newBuilder.setPoint(makePoint("meas02")).build, headers).expectOne

    // then override it a few times
    val put1 = s.put(MeasOverride.newBuilder.setPoint(makePoint("meas01")).setMeas(makeInt("meas01", 100)).build, headers).expectOne()
    val put2 = s.put(MeasOverride.newBuilder.setPoint(makePoint("meas01")).setMeas(makeInt("meas01", 999)).build, headers).expectOne()
    s.put(MeasOverride.newBuilder.setPoint(makePoint("meas02")).setMeas(makeInt("meas02", 888)).build, headers).expectOne()

    val gotten = s.get(MeasOverride.newBuilder.setPoint(makePoint("meas01")).build).expectOne()
    // make sure the object has had all of the point and node fields filled out
    gotten.getPoint.getName should equal("meas01")
    //gotten.getPoint.getLogicalNode.getUuid should equal(node.getUuid)
    gotten.getMeas.getIntVal should equal(999)

    s.get(MeasOverride.newBuilder.setPoint(makePoint("meas01")).build).expectOne()
    s.get(MeasOverride.newBuilder.setPoint(makePointByNodeId(node.getUuid)).build).expectOne()
    s.get(MeasOverride.newBuilder.setPoint(makePointByNodeName(node.getName)).build).expectOne()

    s.get(MeasOverride.newBuilder.setPoint(makePoint("*")).build).expectMany(2)
    s.get(MeasOverride.newBuilder.setPoint(makePointByNodeId("*")).build).expectMany(2)
    s.get(MeasOverride.newBuilder.setPoint(makePointByNodeName("*")).build).expectMany(2)
  }
}
