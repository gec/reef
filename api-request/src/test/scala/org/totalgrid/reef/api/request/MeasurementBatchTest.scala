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
package org.totalgrid.reef.api.request

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import scala.collection.JavaConversions._
import org.totalgrid.reef.proto.Model.{ Point, Entity, Relationship }
import org.totalgrid.reef.proto.Measurements.{ Measurement, MeasurementBatch, MeasurementSnapshot }

@RunWith(classOf[JUnitRunner])
class MeasurementBatchTest
    extends ServiceClientSuite("MeasurementBatch.xml", "MeasurementBatch",
      <div>
        <p>
          The MeasurementSnapshot service provides the current state of measurements. The request contains the
          list of measurement names, the response contains the requested measurement objects.
        </p>
      </div>)
    with ShouldMatchers {

  def putMeas(m: Measurement) = client.putOneOrThrow(MeasurementBatch.newBuilder.addMeas(m).setWallTime(System.currentTimeMillis).build)
  def putAll(m: List[Measurement]) = client.putOneOrThrow(MeasurementBatch.newBuilder.addAllMeas(m).setWallTime(System.currentTimeMillis).build)

  test("Simple puts") {
    val pointName = "StaticSubstation.Line02.Current"
    val original = client.getOneOrThrow(MeasurementSnapshot.newBuilder.addPointNames("StaticSubstation.Line02.Current").build).getMeasurementsList.head

    val updated = original.toBuilder.setDoubleVal(original.getDoubleVal * 2).setTime(System.currentTimeMillis).build
    val req = MeasurementBatch.newBuilder.addMeas(updated).setWallTime(System.currentTimeMillis).build
    val resp = client.putOneOrThrow(req)

    doc.addCase("Put measurement", "Put", "Put a single new measurement.", req, resp)

    putMeas(original.toBuilder.setTime(System.currentTimeMillis).build)
  }

  test("Multi put") {
    val points = List("StaticSubstation.Line02.Current", "StaticSubstation.Breaker02.Bkr", "StaticSubstation.Breaker02.Tripped")
    val originals = client.getOneOrThrow(MeasurementSnapshot.newBuilder.addAllPointNames(points).build).getMeasurementsList.toList

    val updateds = originals.map { m =>
      if (m.getType == Measurement.Type.DOUBLE)
        m.toBuilder.setDoubleVal(m.getDoubleVal * 2).setTime(System.currentTimeMillis).build
      else if (m.getType == Measurement.Type.BOOL)
        m.toBuilder.setBoolVal(!m.getBoolVal).setTime(System.currentTimeMillis).build
      else m.toBuilder.setTime(System.currentTimeMillis).build
    }

    val req = MeasurementBatch.newBuilder.addAllMeas(updateds).setWallTime(System.currentTimeMillis).build
    val resp = client.putOneOrThrow(req)

    doc.addCase("Put multiple measurements", "Put", "Put multiple new measurements in a single MeasurementBatch.", req, resp)

    val reverteds = originals.map { m => m.toBuilder.setTime(System.currentTimeMillis).build }
    putAll(reverteds)
  }
}