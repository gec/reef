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
package org.totalgrid.reef.metrics

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class CurrentMetricsValueHolderTests extends FunSuite with ShouldMatchers {

  test("Counters") {
    val c = new CounterMetric
    c.value should equal(0)

    c.update(5)
    c.value should equal(5)

    c.reset
    c.value should equal(0)
  }

  test("Value") {
    val c = new ValueMetric
    c.value should equal(0)

    c.update(5)
    c.value should equal(5)

    c.reset
    c.value should equal(0)
  }

  test("Average") {
    val c = new AverageMetric(10)
    c.value should equal(0.0)

    c.update(5)
    c.value should equal(5.0)

    c.update(15)
    c.value should equal(10.0)

    c.reset
    c.value should equal(0.0)
  }

  test("Name Filtering") {
    val cv = new CurrentMetricsValueHolder("all")

    val allNames = List("cmp1.hook1", "cmp1.hook2", "cmp1.hook3", "cmp2.hook1", "cmp2.hook2", "cmp3.hook1")

    allNames.foreach(cv.getSinkFunction(_, MetricsHooks.Value))

    val allNamesWithApp = allNames.map("all." + _)

    cv.values(Nil).keys.toList.sorted should equal(allNamesWithApp)
    cv.values(List("#")).keys.toList.sorted should equal(allNamesWithApp)
    cv.values(List("all.#")).keys.toList.sorted should equal(allNamesWithApp)
    cv.values(List("all.none.#")).keys.toList.sorted should equal(Nil)
    cv.values(List("none.#")).keys.toList.sorted should equal(Nil)
    cv.values(List("all.cmp1.*")).keys.toList.sorted should equal(List("all.cmp1.hook1", "all.cmp1.hook2", "all.cmp1.hook3"))
    cv.values(List("all.*.hook2")).keys.toList.sorted should equal(List("all.cmp1.hook2", "all.cmp2.hook2"))
    cv.values(List("all.cmp3.hook1")).keys.toList.sorted should equal(List("all.cmp3.hook1"))
  }

  test("Multi Level Name Filtering") {
    val cv = new SimpleMetricsSink

    val hookNames = List("cmp1.hook1", "cmp1.hook2", "cmp1.hook3", "cmp2.hook1", "cmp2.hook2", "cmp3.hook1")

    hookNames.foreach(cv.getStore("app1").getSinkFunction(_, MetricsHooks.Value))
    hookNames.foreach(cv.getStore("app2").getSinkFunction(_, MetricsHooks.Value))

    val allNamesForApp1 = hookNames.map("app1." + _).toList
    val allNamesForApp2 = hookNames.map("app2." + _).toList
    val allNames = allNamesForApp1 ::: allNamesForApp2

    cv.values(Nil).keys.toList.sorted should equal(allNames)
    cv.values(List("#")).keys.toList.sorted should equal(allNames)
    cv.values(List("app1.#")).keys.toList.sorted should equal(allNamesForApp1)
    cv.values(List("app2.#")).keys.toList.sorted should equal(allNamesForApp2)

    cv.values(List("app2.*")).keys.toList.sorted should equal(Nil)
    cv.values(List("*.cmp3.*")).keys.toList.sorted should equal(List("app1.cmp3.hook1", "app2.cmp3.hook1"))
  }

  test("Calculated rates") {
    val cv = new CurrentMetricsValueHolder("all")

    val testFunc = cv.getSinkFunction("test", MetricsHooks.Value)

    val start = cv.values()

    testFunc(5)

    val end = cv.values()

    val results = MetricsMapHelpers.changePerSecond(start, end, 500)
    results.get("all.test.Rate") should equal(Some(10.0))

    val results2 = MetricsMapHelpers.changePerSecond(start, end, 5000)
    results2.get("all.test.Rate") should equal(Some(1.0))
  }

  test("Sum and Count") {
    val map = Map("node1.incs" -> 3, "node2.incs" -> 1, "node3.incs" -> 2)

    val counts = MetricsMapHelpers.sumAndCount(map, "*.incs")
    counts("*.incs.Sum") should equal(6.0)
    counts("*.incs.Count") should equal(3)

  }

}