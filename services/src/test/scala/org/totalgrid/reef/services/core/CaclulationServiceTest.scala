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

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import scala.collection.JavaConversions._

import org.totalgrid.reef.client.proto.Envelope.SubscriptionEventType._
import org.totalgrid.reef.client.service.proto.Measurements.Measurement

import org.totalgrid.reef.client.service.proto.Calculations._
import org.totalgrid.reef.client.service.proto.Model.{ ReefUUID, Point, Entity, EntityEdge }
import org.totalgrid.reef.client.exception.BadRequestException

@RunWith(classOf[JUnitRunner])
class CaclulationServiceTest extends PointServiceTestBase {

  class CalcFixture extends Fixture {
    val calculationService = new SyncService(new CalculationConfigService(modelFactories.calculations), contextSource)
    val edgeService = new SyncService(new EntityEdgeService(modelFactories.edges), contextSource)

    def addCalc(point: String, inputs: List[String], uuid: Option[ReefUUID] = None) = {
      val b = Calculation.newBuilder.setOutputPoint(Point.newBuilder.setName(point))
      uuid.foreach(b.setUuid(_))
      inputs.foreach { i => b.addCalcInputs(CalculationInput.newBuilder.setPoint(Point.newBuilder.setName(i)).setVariableName(i)) }

      val calc = calculationService.put(b.build).expectOne
      checkInputPoints(calc)
      calc
    }

    def deleteCalc(uuid: ReefUUID) {
      calculationService.delete(Calculation.newBuilder.setUuid(uuid).build).expectOne
    }

    def checkInputPoints(calculation: Calculation) = {
      val expectedNames = calculation.getCalcInputsList.toList.map { _.getPoint.getUuid.toString }.sorted

      val edgeQuery = EntityEdge.newBuilder.setChild(Entity.newBuilder.setUuid(calculation.getUuid)).setRelationship("calcs")
      val children = edgeService.get(edgeQuery.build).expectMany()

      children.map { _.getParent.getUuid.toString }.sorted should equal(expectedNames)
    }

    def pointIsCalculated(name: String, isCalced: Boolean) = {
      val foundType = getEntity(name).get.getTypesList.toList.find(_ == "CalculatedPoint")
      if (isCalced) foundType should not equal (None)
      else foundType should equal(None)
    }

  }

  test("Creating Calculation fills out point info and sets up edges") {
    val f = new CalcFixture

    f.addPoint("input1", "v")
    f.addPoint("input2", "a")
    f.addPoint("calcPoint", "w")

    val calc = f.addCalc("calcPoint", List("input1", "input2"))

    calc.getOutputPoint.getName should equal("calcPoint")
    calc.getOutputPoint.getUnit should equal("w")

    calc.getCalcInputsList.get(0).getPoint.getName should equal("input1")
    calc.getCalcInputsList.get(1).getPoint.getName should equal("input2")

    calc.getCalcInputsList.get(0).getPoint.getUnit should equal("v")
    calc.getCalcInputsList.get(1).getPoint.getUnit should equal("a")

    f.pointIsCalculated("calcPoint", true)

    f.deleteCalc(calc.getUuid)

    f.pointIsCalculated("calcPoint", false)

    val eventList = List(
      (ADDED, classOf[Entity]), (ADDED, classOf[Point]), (ADDED, classOf[Measurement]),
      (ADDED, classOf[Entity]), (ADDED, classOf[Point]), (ADDED, classOf[Measurement]),
      (ADDED, classOf[Entity]), (ADDED, classOf[Point]), (ADDED, classOf[Measurement]),
      (ADDED, classOf[Entity]), (ADDED, classOf[Calculation]),
      // 3 calcs and 1 source edge
      (ADDED, classOf[EntityEdge]), (ADDED, classOf[EntityEdge]), (ADDED, classOf[EntityEdge]), (ADDED, classOf[EntityEdge]),
      // two derived edges
      (ADDED, classOf[EntityEdge]), (ADDED, classOf[EntityEdge]),
      // adding CalculatedPoint type
      (MODIFIED, classOf[Entity]),
      // removing CalculatedPoint type
      (MODIFIED, classOf[Entity]),
      // removed entity underlying calculation
      (REMOVED, classOf[Entity]),
      // 3 calcs and 1 source edge
      (REMOVED, classOf[EntityEdge]), (REMOVED, classOf[EntityEdge]), (REMOVED, classOf[EntityEdge]), (REMOVED, classOf[EntityEdge]),
      // two derived edges
      (REMOVED, classOf[EntityEdge]), (REMOVED, classOf[EntityEdge]),
      (REMOVED, classOf[Calculation]))

    f.events.map(s => (s.typ, s.value.getClass)) should equal(eventList)
  }

  test("Calculations can be updated to different output point") {
    val f = new CalcFixture

    f.addPoint("input1", "v")
    f.addPoint("calcPoint1", "w")
    f.addPoint("calcPoint2", "w")

    val calc = f.addCalc("calcPoint1", List("input1"))

    f.pointIsCalculated("calcPoint1", true)
    f.pointIsCalculated("calcPoint2", false)

    f.addCalc("calcPoint2", List("input1"), Some(calc.getUuid))

    f.pointIsCalculated("calcPoint1", false)
    f.pointIsCalculated("calcPoint2", true)
  }

  test("Calculations can be updated to different input points") {
    val f = new CalcFixture

    f.addPoint("input1", "v")
    f.addPoint("input2", "a")
    f.addPoint("calcPoint", "w")

    val calc1 = f.addCalc("calcPoint", List("input1"))
    calc1.getCalcInputsList.toList.map { _.getPoint.getName } should equal(List("input1"))

    val calc2 = f.addCalc("calcPoint", List("input1", "input2"))
    calc2.getCalcInputsList.toList.map { _.getPoint.getName } should equal(List("input1", "input2"))

    calc2.getUuid should equal(calc1.getUuid)
  }

  test("Calculations with unknown points are rejected") {
    val f = new CalcFixture

    intercept[BadRequestException] {
      f.addCalc("calcPoint", List("input1"))
    }.getMessage should include("Unknown OutputPoint")

    f.addPoint("calcPoint", "w")

    val msg = intercept[BadRequestException] {
      f.addCalc("calcPoint", List("input"))
    }.getMessage
    msg should include("unknown InputPoint")
    msg should include("input")
  }

  test("Removing point removes calculation") {
    val f = new CalcFixture

    f.addPoint("input1", "v")
    f.addPoint("input2", "a")
    f.addPoint("calcPoint", "w")

    val calc = f.addCalc("calcPoint", List("input1"))

    f.deletePoint("calcPoint")

    f.getEntity(calc.getUuid) should equal(None)
  }
}
