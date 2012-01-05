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

import org.totalgrid.reef.client.sapi.rpc.impl.util.ServiceClientSuite

@RunWith(classOf[JUnitRunner])
class MeasurementOverrideTest extends ServiceClientSuite {

  test("Demonstrate Overrides") {
    val point = PointRequestBuilders.getByName("StaticSubstation.Line02.Current")

    client.clearMeasurementOverridesOnPoint(point).await

    val originalMeas = client.getMeasurementByPoint(point).await

    val nis = client.setPointOutOfService(point).await

    val nised = client.getMeasurementByPoint(point).await

    val over = client.setPointOverride(point, originalMeas.toBuilder.setDoubleVal(100).setTime(System.currentTimeMillis).build).await

    // TODO: add id to measurement override - backlog-63
    //over.getId should equal(nis.getId)

    val overriden = client.getMeasurementByPoint(point).await

    client.deleteMeasurementOverride(over).await

    //    // TODO: fix overrides time ordering reef-23
    //    nised.getQuality.getOperatorBlocked should equal(true)
    //    nised.getQuality.getDetailQual.getOldData should equal(true)
    //
    //    overriden.getQuality.getSource should equal(Quality.Source.SUBSTITUTED)
    //    overriden.getQuality.getOperatorBlocked should equal(true)
  }

}