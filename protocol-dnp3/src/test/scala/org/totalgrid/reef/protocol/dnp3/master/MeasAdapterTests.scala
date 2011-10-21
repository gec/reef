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
package org.totalgrid.reef.protocol.dnp3.master

import org.totalgrid.reef.proto.{ Mapping, Measurements }
import org.totalgrid.reef.protocol.dnp3._

import scala.collection.JavaConversions._

import org.scalatest.Suite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class MeasAdapterTests extends Suite with ShouldMatchers {

  // build a map that we can use for all the tests
  val indexmap = {

    case class MapEntry(index: Int, name: String, typ: Mapping.DataType)

    val mappings = List(
      MapEntry(0, "binary0", Mapping.DataType.BINARY),
      MapEntry(0, "analog0", Mapping.DataType.ANALOG),
      MapEntry(0, "counter0", Mapping.DataType.COUNTER),
      MapEntry(0, "control0", Mapping.DataType.CONTROL_STATUS),
      MapEntry(0, "setpoint0", Mapping.DataType.SETPOINT_STATUS))

    val map = Mapping.IndexMapping.newBuilder.setDeviceUid("test") // device uid is irrelevant
    mappings.foreach { x =>
      map.addMeasmap(Mapping.MeasMap.newBuilder.setIndex(x.index).setPointName(x.name).setType(x.typ))
    }
    map.build
  }

  // Test routing when something not in the map is specified
  def testEmptyMap() {
    runATest { adapt =>
      adapt._Update(new Binary, 10)
    } { batch =>
      batch should equal(None)
    }
  }

  def testMappings {

    runATest { adapt =>
      adapt.Update(new Binary(true), 0)
      adapt.Update(new Analog(5), 0)
      adapt.Update(new Counter(7), 0)
      adapt.Update(new ControlStatus(true), 0)
      adapt.Update(new SetpointStatus(3), 0)
    } { batch =>

      val list = batch.get.getMeasList //assume it's Some
      list.size should equal(5)

      list(0).getName() should equal("binary0")
      list(0).getType() should equal(Measurements.Measurement.Type.BOOL)
      list(0).getBoolVal() should equal(true)

      list(1).getName() should equal("analog0")
      list(1).getType() should equal(Measurements.Measurement.Type.DOUBLE)
      list(1).getDoubleVal() should equal(5)

      list(2).getName() should equal("counter0")
      list(2).getType() should equal(Measurements.Measurement.Type.INT)
      list(2).getIntVal() should equal(7)

      list(3).getName() should equal("control0")
      list(3).getType() should equal(Measurements.Measurement.Type.BOOL)
      list(3).getBoolVal() should equal(true)

      list(4).getName() should equal("setpoint0")
      list(4).getType() should equal(Measurements.Measurement.Type.DOUBLE)
      list(4).getDoubleVal() should equal(3)
    }

  }

  def runATest(load: MeasAdapter => Unit)(test: Option[Measurements.MeasurementBatch] => Unit) = {

    var batch: Option[Measurements.MeasurementBatch] = None
    val adapt = new MeasAdapter(indexmap, x => batch = Some(x))

    // do the transaction
    adapt.Start
    load(adapt)
    adapt.End

    test(batch)
  }

}
