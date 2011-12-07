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
import org.totalgrid.reef.models.DatabaseUsingTestBase
import org.totalgrid.reef.client.service.proto.Model.{ Point, PointType }
import org.totalgrid.reef.client.service.proto.Processing.{ MeasOverride, TriggerSet }
import org.totalgrid.reef.measurementstore.InMemoryMeasurementStore
import org.totalgrid.reef.services.ServiceDependencies
import org.totalgrid.reef.client.sapi.client.BasicRequestHeaders
import org.totalgrid.reef.client.service.proto.Measurements.Quality.Validity

import org.totalgrid.reef.services.core.SyncServiceShims._

@RunWith(classOf[JUnitRunner])
class PointServiceTest extends DatabaseUsingTestBase {

  class Fixture {
    val fakeDatabase = new InMemoryMeasurementStore

    val modelFactories = new ModelFactories(new ServiceDependenciesDefaults(cm = fakeDatabase))
    val pointService = new PointService(modelFactories.points)
    val triggerService = new TriggerSetService(modelFactories.triggerSets)
    val overrideService = new OverrideConfigService(modelFactories.overrides)

    def addPoint(name: String = "point01", unit: String = "amps", typ: PointType = PointType.ANALOG) = {
      val p = Point.newBuilder.setName(name).setUnit(unit).setType(typ)
      pointService.put(p.build).expectOne
    }
    def addTriggerSet(name: String = "point01") = {
      val t = TriggerSet.newBuilder.setPoint(Point.newBuilder.setName(name))
      triggerService.put(t.build).expectOne
    }
    def addOverride(name: String = "point01") = {
      val o = MeasOverride.newBuilder.setPoint(Point.newBuilder.setName(name))
      overrideService.put(o.build).expectOne
    }

    def getMeasurement(name: String = "point01") = {
      fakeDatabase.get(name)
    }

    def getPoints(name: String = "*") = {
      val p = Point.newBuilder.setName(name)
      pointService.get(p.build).expectMany()
    }

    def getTriggers(name: String = "*") = {
      val t = TriggerSet.newBuilder.setPoint(Point.newBuilder.setName(name))
      triggerService.get(t.build).expectMany()
    }

    def getOverrides(name: String = "*") = {
      val o = MeasOverride.newBuilder.setPoint(Point.newBuilder.setName(name))
      overrideService.get(o.build).expectMany()
    }

    def deletePoint(name: String = "point01") = {
      val p = Point.newBuilder.setName(name)
      pointService.delete(p.build).expectOne
    }
  }

  test("Creating point creates offline measurement") {
    val f = new Fixture

    f.addPoint()

    val measOption = f.getMeasurement()
    measOption should not equal (None)
    val m = measOption.get

    m.getQuality.getValidity should equal(Validity.QUESTIONABLE)
    m.getTime should not equal (0)
  }

  test("Deleting point deletes dependent resources") {
    val f = new Fixture

    f.getPoints() should equal(Nil)
    f.getTriggers() should equal(Nil)
    f.getOverrides() should equal(Nil)
    f.getMeasurement() should equal(None)

    val point = f.addPoint()
    f.getPoints() should equal(point :: Nil)

    f.getMeasurement() should not equal (None)

    val trigger = f.addTriggerSet()
    f.getTriggers() should equal(trigger :: Nil)

    val overrid = f.addOverride()
    f.getOverrides() should equal(overrid :: Nil)

    f.deletePoint()

    f.getPoints() should equal(Nil)
    f.getTriggers() should equal(Nil)
    f.getOverrides() should equal(Nil)
    f.getMeasurement() should equal(None)
  }

}