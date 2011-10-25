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
import org.totalgrid.reef.proto.Measurements.Measurement
import org.totalgrid.reef.client.sapi.rpc.impl.builders.PointRequestBuilders
import org.totalgrid.reef.api.japi.BadRequestException
import org.totalgrid.reef.api.japi.client.SubscriptionEvent
import org.totalgrid.reef.client.sapi.rpc.impl.util.{ SubscriptionEventAcceptorShim, ClientSessionSuite }
import net.agileautomata.commons.testing.SynchronizedList

@RunWith(classOf[JUnitRunner])
class MeasurementHistoryTest
    extends ClientSessionSuite("MeasurementHistory.xml", "MeasurementHistory",
      <div>
        <p>
          The MeasurementHistory service provides the historical state of measurements.
        </p>
      </div>)
    with ShouldMatchers {

  test("Get History") {
    val point = PointRequestBuilders.getByName("StaticSubstation.Line02.Current")

    // make sure a left over override doesn't stop our published values
    client.clearMeasurementOverridesOnPoint(point).await

    val original = client.getMeasurementByPoint(point).await

    val now = System.currentTimeMillis
    val startValue = original.getDoubleVal
    val history = for (i <- 1 to 10) yield original.toBuilder.setDoubleVal(startValue + i).setTime(now + i).build

    recorder.addExplanation("Create Short History", "Create a 10 point fake history, 1 measurement per millisecond")
    client.publishMeasurements(history.toList).await

    recorder.addExplanation("Get last 3 points of history", "Get the most recent 3 measurements")
    val last3 = client.getMeasurementHistory(point, 3).await
    last3.map { _.getDoubleVal } should equal(List(startValue + 8, startValue + 9, startValue + 10))

    val list = new SynchronizedList[Double]

    recorder.addExplanation("Get measurements since time", "Should be only two measurements since we limited since to the last 2 fake entries we made.")
    val last2 = client.subscribeToMeasurementHistory(point, now + 9, 100).await
    last2.getResult.map { _.getDoubleVal } should equal(List(startValue + 9, startValue + 10))

    last2.getSubscription.start(new SubscriptionEventAcceptorShim[Measurement]({ ea: SubscriptionEvent[Measurement] => list.append(ea.getValue.getDoubleVal) }))

    recorder.addExplanation("Get measurements in range", "We can ask for a specific time range of measurements, this implies not getting live data.")
    val middle = client.getMeasurementHistory(point, now + 3, now + 5, true, 100).await
    middle.map { _.getDoubleVal } should equal(List(startValue + 3, startValue + 4, startValue + 5))

    list.get.isEmpty should equal(true)

    val range = 11 to 100

    val newMeasurements = for (i <- range) yield original.toBuilder.setDoubleVal(startValue + i).setTime(now + i).build
    val published = client.publishMeasurements(newMeasurements.toList).await

    published should equal(true)

    val expected = newMeasurements.map(_.getDoubleVal).toList

    list shouldBecome expected within 5000
    list shouldRemain expected during 500
  }

  test("Bad Requests") {

    val point = PointRequestBuilders.getByName("StaticSubstation.Line02.Current")

    intercept[BadRequestException] {
      recorder.addExplanation("Bad Request: Over Limit", "Try to request a billion measurements.")
      client.getMeasurementHistory(point, 1000000000).await
    }
  }
}