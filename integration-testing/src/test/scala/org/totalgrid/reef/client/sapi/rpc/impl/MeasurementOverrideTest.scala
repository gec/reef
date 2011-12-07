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
package org.totalgrid.reef.client.sapi.rpc.impl

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.totalgrid.reef.client.sapi.rpc.impl.builders.PointRequestBuilders

import org.totalgrid.reef.client.sapi.rpc.impl.util.ClientSessionSuite

@RunWith(classOf[JUnitRunner])
class MeasurementOverrideTest
    extends ClientSessionSuite("MeasurementOverride.xml", "MeasurementOverride",
      <div>
        <p>
          The MeasurementOverride service.
        </p>
      </div>)
    with ShouldMatchers {

  test("Demonstrate Overrides") {
    val point = PointRequestBuilders.getByName("StaticSubstation.Line02.Current")

    recorder.addExplanation("Clear old overrides", "Removed any leftover overrides from previous tests.")
    client.clearMeasurementOverridesOnPoint(point).await

    recorder.addExplanation("Read Original Value", "Get current value for the point")
    val originalMeas = client.getMeasurementByPoint(point).await

    recorder.addExplanation("Mark Point NIS", "Creating an override with no overriding measurement attached implies NIS.")
    val nis = client.setPointOutOfService(point).await

    recorder.addExplanation("Read NIS value", "Check that value is marked appropriately.")
    val nised = client.getMeasurementByPoint(point).await

    recorder.addExplanation("Override point", "Override the value to 100. Notice the id is the same as for the NIS.")
    val over = client.setPointOverride(point, originalMeas.toBuilder.setDoubleVal(100).setTime(System.currentTimeMillis).build).await

    // TODO: add id to measurement override - backlog-63
    //over.getId should equal(nis.getId)

    recorder.addExplanation("Read Overriden Value", "Check that value is marked appropriately.")
    val overriden = client.getMeasurementByPoint(point).await

    recorder.addExplanation("Delete Override", "Clear the override we set (could have cleared)")
    client.deleteMeasurementOverride(over).await

    //    // TODO: fix overrides time ordering reef-23
    //    nised.getQuality.getOperatorBlocked should equal(true)
    //    nised.getQuality.getDetailQual.getOldData should equal(true)
    //
    //    overriden.getQuality.getSource should equal(Quality.Source.SUBSTITUTED)
    //    overriden.getQuality.getOperatorBlocked should equal(true)
  }

}