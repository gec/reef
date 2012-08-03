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
package org.totalgrid.reef.jmx

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import management.ManagementFactory

@RunWith(classOf[JUnitRunner])
class TestMetricsManager extends FunSuite with ShouldMatchers {

  test("Publishes To JMX") {
    val manager = MetricsManager("org.totalgrid.reef.jmx.test")

    val metrics = manager.metrics("ReefTestMetrics")

    val count1 = metrics.counter("FirstCounter")
    val count2 = metrics.counter("SecondCounter")

    manager.register()

    count1(3)
    count2(8)

    val objName = MBeanUtils.objectName("org.totalgrid.reef.jmx.test", Nil, "ReefTestMetrics")

    val server = ManagementFactory.getPlatformMBeanServer
    server.isRegistered(objName) should equal(true)

    server.getAttribute(objName, "FirstCounter") should equal(3)
    server.getAttribute(objName, "SecondCounter") should equal(8)
  }

  def fullRegister(count: Int) {
    val manager = MetricsManager("org.totalgrid.reef.jmx.test")
    val metrics = manager.metrics("ReefTestMetrics")
    val count1 = metrics.counter("FirstCounter")
    count1(count)
    manager.register()
  }

  test("Replace on second register") {
    val server = ManagementFactory.getPlatformMBeanServer
    val objName = MBeanUtils.objectName("org.totalgrid.reef.jmx.test", Nil, "ReefTestMetrics")

    fullRegister(4)

    server.isRegistered(objName) should equal(true)
    server.getAttribute(objName, "FirstCounter") should equal(4)

    fullRegister(10)

    server.isRegistered(objName) should equal(true)
    server.getAttribute(objName, "FirstCounter") should equal(10)
  }
}