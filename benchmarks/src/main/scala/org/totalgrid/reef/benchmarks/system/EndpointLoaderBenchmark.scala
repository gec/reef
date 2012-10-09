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
package org.totalgrid.reef.benchmarks.system

import org.totalgrid.reef.benchmarks.{ BenchmarkTest, BenchmarkReading }
import java.io.PrintStream
import org.totalgrid.reef.util.Timing.Stopwatch
import org.totalgrid.reef.benchmarks.endpoints.EndpointStateTransitionTimer
import org.totalgrid.reef.client.service.proto.FEP.EndpointConnection.State
import org.totalgrid.reef.client.sapi.sync.AllScadaService
import org.totalgrid.reef.client.{ Promise, Client }

case class EndpointLoadingReading(request: String, endpoints: Int, pointsPerEndpoint: Int,
    time: Long, parallelism: Int, batchSize: Int) extends BenchmarkReading {
  def csvName = "endpointLoading"

  def testParameterNames = List("request", "endpoints", "pointsPerEndpoint", "numConcurrent", "batchSize")
  def testParameters = List(request, endpoints, pointsPerEndpoint, parallelism, batchSize)

  def testOutputNames = List("time")
  def testOutputs = List(time)
}

class EndpointLoaderBenchmark(endpointNames: List[String], pointsPerEndpoint: Int, parallelism: Int, batchSize: Int, add: Boolean = true, delete: Boolean = true) extends BenchmarkTest {

  val endpoints = endpointNames.size

  def runTest(client: Client, stream: Option[PrintStream]) = {

    var readings = List.empty[BenchmarkReading]

    def addReadings[A](operation: String, ops: Seq[() => Promise[A]]) {
      val stopwatch = Stopwatch.start
      val results = ModelCreationUtilities.parallelExecutor(client.getInternal.getExecutor, parallelism, ops)
      val overallTime = stopwatch.elapsed

      readings ::= new EndpointLoadingReading("overall" + operation, endpoints, pointsPerEndpoint, overallTime, parallelism, batchSize)

      results.foreach {
        case (time, _) =>
          readings ::= new EndpointLoadingReading(operation, endpoints, pointsPerEndpoint, time, parallelism, batchSize)
      }
    }

    if (add) {
      val preparedLoaders = endpointNames.map { n => ModelCreationUtilities.addEndpoint(client.spawn(), n, pointsPerEndpoint, batchSize) }
      stream.foreach { _.println("Adding " + endpointNames.size + " endpoints with " + pointsPerEndpoint + " points using " + parallelism + " writers.") }
      addReadings("addEndpoint", preparedLoaders)
    }

    if (delete) {
      val services = client.getService(classOf[AllScadaService])
      val endpointUuids = services.getEndpointsByNames(endpointNames).map { _.getUuid }
      endpointUuids.foreach { services.disableEndpointConnection(_) }
      val map = new EndpointStateTransitionTimer(services.subscribeToEndpointConnections(), endpointUuids)
      map.checkAllState(false, State.COMMS_DOWN)

      val preparedDeleters = endpointNames.map { n => ModelCreationUtilities.deleteEndpoint(client.spawn(), n, pointsPerEndpoint, batchSize) }
      stream.foreach { _.println("Deleting " + endpointNames.size + " endpoints with " + pointsPerEndpoint + " points using " + parallelism + " writers.") }
      addReadings("deleteEndpoint", preparedDeleters)
    }

    readings
  }

}