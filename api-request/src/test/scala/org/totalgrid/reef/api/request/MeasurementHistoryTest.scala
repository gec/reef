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

import builders.PointRequestBuilders
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import scala.collection.JavaConversions._
import org.totalgrid.reef.util.BlockingQueue
import org.totalgrid.reef.proto.Measurements.{ MeasurementHistory, Measurement }
import org.totalgrid.reef.api.BadRequestException
import org.totalgrid.reef.api.ServiceTypes.Event

@RunWith(classOf[JUnitRunner])
class MeasurementHistoryTest
    extends ServiceClientSuite("MeasurementHistory.xml", "MeasurementHistory",
      <div>
        <p>
          The MeasurementHistory service provides the historical state of measurements.
        </p>
      </div>)
    with ShouldMatchers {

  test("Get History") {
    val point = PointRequestBuilders.getByName("StaticSubstation.Line02.Current")

    val original = client.getMeasurementByPoint(point)

    val now = System.currentTimeMillis
    val startValue = original.getDoubleVal
    val history = for (i <- 1 to 10) yield original.toBuilder.setDoubleVal(startValue + i).setTime(now + i).build

    client.addExplanation("Create Short History", "Create a 10 point fake history, 1 measurement per millisecond")
    client.publishMeasurements(history.toList)

    client.addExplanation("Get last 3 points of history", "Get the most recent 3 measurements")
    val last3 = client.getMeasurementHistory(point, 3)
    last3.map { _.getDoubleVal } should equal(List(startValue + 8, startValue + 9, startValue + 10))

    val queue = new BlockingQueue[Measurement]
    val sub = client.addSubscription(classOf[MeasurementHistory], (ea: Event[Measurement]) => queue.push(ea.result))

    client.addExplanation("Get measurements since time", "Should be only two measurements since we limited since to the last 2 fake entries we made.")
    val last2 = client.getMeasurementHistory(point, now + 9, 100, sub)
    last2.map { _.getDoubleVal } should equal(List(startValue + 9, startValue + 10))

    client.addExplanation("Get measurements in range", "We can ask for a specific time range of measurements, this implies not getting live data.")
    val middle = client.getMeasurementHistory(point, now + 3, now + 5, 100)
    middle.map { _.getDoubleVal } should equal(List(startValue + 3, startValue + 4, startValue + 5))

    queue.size should equal(0)

    val newMeasurements = for (i <- 11 to 15) yield original.toBuilder.setDoubleVal(startValue + i).setTime(now + i).build
    client.publishMeasurements(newMeasurements.toList)

    queue.pop(1000).getDoubleVal should equal(startValue + 11)
    queue.pop(0).getDoubleVal should equal(startValue + 12)
    queue.pop(0).getDoubleVal should equal(startValue + 13)
    queue.pop(0).getDoubleVal should equal(startValue + 14)
    queue.pop(0).getDoubleVal should equal(startValue + 15)
    queue.size should equal(0)
  }

  test("Bad Requests") {

    val point = PointRequestBuilders.getByName("StaticSubstation.Line02.Current")

    intercept[BadRequestException] {
      client.addExplanation("Bad Request: Over Limit", "Try to request a billion measurements.")
      client.getMeasurementHistory(point, 1000000000)
    }
  }
}