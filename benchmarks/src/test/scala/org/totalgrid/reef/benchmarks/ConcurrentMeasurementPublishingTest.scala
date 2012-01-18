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
import org.totalgrid.reef.benchmarks.measurements.ConcurrentMeasurementPublishingBenchmark
import org.totalgrid.reef.benchmarks.system.EndpointLoaderBenchmark

@RunWith(classOf[JUnitRunner])
class ConcurrentMeasurementPublishingTest extends BenchmarkTestBase {

  var setupReadings = List.empty[BenchmarkReading]
  var measReadings = List.empty[BenchmarkReading]

  override protected def afterAll() {
    writeFiles("measPublishing", measReadings)
    writeFiles("measSetup", setupReadings)
  }

  val endpoints = 15
  val pointsPerEndpoint = 30
  //val parallelisms = List(1, 5, 10, 15)
  val parallelisms = List(5, 10)
  val batchSizes = List(25)
  val totalMeasurements = 50000
  val measurementsImported = totalMeasurements * parallelisms.size * batchSizes.size
  val endpointNames = (1 to endpoints).map { i => "TestEndpoint" + i }.toList

  test("Setup " + endpointNames.size + " endpoints with " + pointsPerEndpoint + " points") {
    setupReadings :::= runBenchmark(new EndpointLoaderBenchmark(endpointNames, pointsPerEndpoint, 5, 50, true, false))
  }

  parallelisms.foreach { threads =>
    batchSizes.foreach { batchSize =>
      test("Publishing " + totalMeasurements + " measurements in batches of: " + batchSize + "  with " + threads + " threads") {
        measReadings :::= runBenchmark(new ConcurrentMeasurementPublishingBenchmark(endpointNames, totalMeasurements, threads, batchSize))
      }
    }
  }

  test("Deleting " + endpointNames.size + " endpoints with " + pointsPerEndpoint + " points and " + measurementsImported + " measurements") {
    setupReadings :::= runBenchmark(new EndpointLoaderBenchmark(endpointNames, pointsPerEndpoint, 5, 50, false, true))
  }

}