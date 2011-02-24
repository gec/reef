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
package org.totalgrid.reef.metrics

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class CurrentMetricsValueHolderTests extends FunSuite with ShouldMatchers {

  class LastValueSink extends NonOperationalDataSink {

    var lastValue: Option[Any] = None

    def nonOp(name: String, value: String): Unit = lastValue = Some(value)
    def nonOp(name: String, value: Int): Unit = lastValue = Some(value)
    def nonOp(name: String, value: Double): Unit = lastValue = Some(value)

  }

  test("Counters") {

    val data = new LastValueSink

    val c = new CounterMetric

    c.publish("test", data)

    data.lastValue should equal(Some(0))

    c.update(5)

    c.publish("test", data)
    data.lastValue should equal(Some(5))

    c.reset

    c.publish("test", data)
    data.lastValue should equal(Some(0))
  }

  test("Value") {

    val data = new LastValueSink

    val c = new ValueMetric

    c.publish("test", data)

    data.lastValue should equal(Some(0))

    c.update(5)

    c.publish("test", data)
    data.lastValue should equal(Some(5))

    c.reset

    c.publish("test", data)
    data.lastValue should equal(Some(0))
  }

  test("Average") {

    val data = new LastValueSink

    val c = new AverageMetric(10)

    c.publish("test", data)

    data.lastValue should equal(Some(0.0))

    c.update(5)

    c.publish("test", data)
    data.lastValue should equal(Some(5.0))

    c.update(15)

    c.publish("test", data)
    data.lastValue should equal(Some(10.0))

    c.reset

    c.publish("test", data)
    data.lastValue should equal(Some(0.0))
  }
}