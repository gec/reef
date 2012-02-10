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
import org.totalgrid.reef.client.service.proto.Model.{ Point, PointType, Entity }
import org.totalgrid.reef.client.service.proto.Processing.{ MeasOverride, TriggerSet }
import org.totalgrid.reef.measurementstore.InMemoryMeasurementStore
import org.totalgrid.reef.services.ServiceDependencies
import org.totalgrid.reef.client.sapi.client.BasicRequestHeaders
import org.totalgrid.reef.client.service.proto.Measurements.Quality.Validity
import org.totalgrid.reef.client.proto.Envelope.SubscriptionEventType._

@RunWith(classOf[JUnitRunner])
class PointServiceTest extends DatabaseUsingTestBase {

  import SubscriptionTools._

  class Fixture {
    val fakeDatabase = new InMemoryMeasurementStore

    val contextSource = new MockContextSource(dbConnection)

    val modelFactories = new ModelFactories(new ServiceDependenciesDefaults(dbConnection, cm = fakeDatabase))
    val pointService = new SyncService(new PointService(modelFactories.points), contextSource)
    val triggerService = new SyncService(new TriggerSetService(modelFactories.triggerSets), contextSource)
    val overrideService = new SyncService(new OverrideConfigService(modelFactories.overrides), contextSource)

    val entityService = new SyncService(new EntityService(modelFactories.entities), contextSource)

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

    def getEntity(name: String = "point01") = {
      val e = Entity.newBuilder.setName(name).build
      entityService.get(e).expectOneOrNone()
    }

    def deletePoint(name: String = "point01") = {
      val p = Point.newBuilder.setName(name)
      pointService.delete(p.build)
    }

    def events = contextSource.sink.events
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
    f.getEntity() should equal(None)

    val point = f.addPoint()
    f.getPoints() should equal(point :: Nil)

    f.getMeasurement() should not equal (None)

    val trigger = f.addTriggerSet()
    f.getTriggers() should equal(trigger :: Nil)

    val overrid = f.addOverride()
    f.getOverrides() should equal(overrid :: Nil)

    f.getEntity().isEmpty should equal(false)

    f.deletePoint()

    f.getPoints() should equal(Nil)
    f.getTriggers() should equal(Nil)
    f.getOverrides() should equal(Nil)
    f.getMeasurement() should equal(None)
    f.getEntity() should equal(None)

    val eventList = List(
      (ADDED, classOf[Entity]),
      (ADDED, classOf[Point]),
      (ADDED, classOf[TriggerSet]),
      (ADDED, classOf[MeasOverride]),
      (REMOVED, classOf[Point]),
      (REMOVED, classOf[TriggerSet]),
      (REMOVED, classOf[MeasOverride]),
      (REMOVED, classOf[Entity]))

    f.events.map(s => (s.typ, s.value.getClass)) should equal(eventList)
  }

}