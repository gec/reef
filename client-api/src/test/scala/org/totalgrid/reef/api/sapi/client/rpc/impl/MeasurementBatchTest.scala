package org.totalgrid.reef.api.sapi.client.rpc.impl

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
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import scala.collection.JavaConversions._
import org.totalgrid.reef.api.proto.Model.{ Point, Entity, Relationship }
import org.totalgrid.reef.api.proto.Measurements.{ Measurement, MeasurementBatch, MeasurementSnapshot }

@RunWith(classOf[JUnitRunner])
class MeasurementBatchTest
    extends ClientSessionSuite("MeasurementBatch.xml", "MeasurementBatch",
      <div>
        <p>
          The MeasurementSnapshot service provides the current state of measurements. The request contains the
          list of measurement names, the response contains the requested measurement objects.
        </p>
      </div>)
    with ShouldMatchers {

  def putMeas(m: Measurement) = client.publishMeasurements(m :: Nil).await
  def putAll(m: List[Measurement]) = client.publishMeasurements(m).await

  test("Simple puts") {
    val pointName = "StaticSubstation.Line02.Current"
    // read the current value so we can edit it
    val original = client.getMeasurementByName(pointName).await

    recorder.addExplanation("Put measurement", "Put a single new measurement.")

    // double the value and post it
    val updated = original.toBuilder.setDoubleVal(original.getDoubleVal * 2).setTime(System.currentTimeMillis).build
    putMeas(updated)

    putMeas(original.toBuilder.setTime(System.currentTimeMillis).build)
  }

  test("Multi put") {
    val names = List("StaticSubstation.Line02.Current", "StaticSubstation.Breaker02.Bkr", "StaticSubstation.Breaker02.Tripped")
    val originals = client.getMeasurementsByNames(names).await

    val updated = originals.map { m =>
      if (m.getType == Measurement.Type.DOUBLE)
        m.toBuilder.setDoubleVal(m.getDoubleVal * 2).setTime(System.currentTimeMillis).build
      else if (m.getType == Measurement.Type.BOOL)
        m.toBuilder.setBoolVal(!m.getBoolVal).setTime(System.currentTimeMillis).build
      else m.toBuilder.setTime(System.currentTimeMillis).build
    }.toList

    recorder.addExplanation("Put multiple measurements", "Put multiple new measurements in a single MeasurementBatch.")
    putAll(updated)

    val reverted = originals.map { m => m.toBuilder.setTime(System.currentTimeMillis).build }.toList
    putAll(reverted)
  }
}