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
package org.totalgrid.reef.benchmarks

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.totalgrid.reef.standalone.InMemoryNode
import org.totalgrid.reef.benchmarks.endpoints._
import org.totalgrid.reef.benchmarks.system._
import org.totalgrid.reef.benchmarks.measurements._
import org.totalgrid.reef.client.sapi.rpc.AllScadaService

@RunWith(classOf[JUnitRunner])
class AllBenchmarksTest extends BenchmarkTestBase {

  var readings = List.empty[BenchmarkReading]

  val attempts = 5
  var endpointNames: List[String] = null
  var pointNames: List[String] = null

  override def afterAll() {
    AllBenchmarksEntryPoint.outputResults(readings)
  }

  test("Load Model") {
    InMemoryNode.system.loadModel("../assemblies/assembly-common/filtered-resources/samples/integration/config.xml")

    val c = client.getRpcInterface(classOf[AllScadaService])
    endpointNames = c.getEndpoints().await.map { _.getName }
    pointNames = c.getPoints().await.map { _.getName }
  }

  test("System State") {
    readings :::= runBenchmark(new SystemStateBenchmark(attempts))
  }
  test("Measurement Roundtrip Timing 1000") {
    readings :::= runBenchmark(new MeasurementPublishingBenchmark(endpointNames, 1000, 5, false))
  }
  test("Measurement Roundtrip Timing 10 direct") {
    readings :::= runBenchmark(new MeasurementPublishingBenchmark(endpointNames, 10, 5, true))
  }
  test("Measurement Roundtrip Timing 10 proxied") {
    readings :::= runBenchmark(new MeasurementPublishingBenchmark(endpointNames, 10, 5, false))
  }
  test("Measurement Statistics") {
    readings :::= runBenchmark(new MeasurementStatBenchmark(pointNames))
  }
  test("Measurement History") {
    readings :::= runBenchmark(new MeasurementHistoryBenchmark(pointNames, List(1, 10, 100, 1000), true))
  }
  test("Endpoint Mangement") {
    readings :::= runBenchmark(new EndpointManagementBenchmark(endpointNames, 5))
  }
}