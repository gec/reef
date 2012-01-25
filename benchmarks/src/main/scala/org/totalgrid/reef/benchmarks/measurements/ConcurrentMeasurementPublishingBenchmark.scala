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
package org.totalgrid.reef.benchmarks.measurements

import org.totalgrid.reef.client.sapi.rpc.AllScadaService
import java.io.PrintStream
import org.totalgrid.reef.benchmarks._
import org.totalgrid.reef.benchmarks.system.ModelCreationUtilities
import org.totalgrid.reef.client.sapi.client.rest.Client

import scala.collection.JavaConversions._
import scala.collection.mutable
import org.totalgrid.reef.client.AddressableDestination
import org.totalgrid.reef.client.service.proto.FEP.EndpointConnection

/**
 * this benchmark tests how long it takes to publish a total number of measurements to the server.
 *
 * Like any benchmark, the data is artificial and doesn't necessarily reflect real world usage.
 * Caveats:
 * - Evenly distributed measurement publishing across streams, real world streams are likely to be very different with
 *   differing usage patterns depending on usage, time of day, polling rates, etc.
 * - Measurements are all different, no filtering is done, real world streams often have lots of redundant information
 */
class ConcurrentMeasurementPublishingBenchmark(endpointNames: List[String], totalMeas: Long, concurrency: Int, batchSize: Int) extends BenchmarkTest {

  case class ConcurrentMeasurementReading(concurrency: Int, batchSize: Int, time: Long, firstMessage: Long, lastMessage: Long) extends BenchmarkReading {
    def csvName = "concurrentPublishing"

    def testParameterNames = List("concurrency", "batchSize", "totalMeas")
    def testParameters = List(concurrency, batchSize, totalMeas)

    def testOutputNames = List("time", "firstMessage", "lastMessage")
    def testOutputs = List(time, firstMessage, lastMessage)
  }

  case class OverallConcurrentMeasurementReading(concurrency: Int, batchSize: Int, time: Long) extends BenchmarkReading {
    def csvName = "concurrentPublishOverall"

    def testParameterNames = List("concurrency", "batchSize", "totalMeas")
    def testParameters = List(concurrency, batchSize, totalMeas)

    def testOutputNames = List("time")
    def testOutputs = List(time)
  }

  def runTest(client: Client, stream: Option[PrintStream]) = {

    val services = client.getRpcInterface(classOf[AllScadaService])

    // load the endpoint connections and point names for each endpoint
    val endpoints = endpointNames.map { services.getEndpointByName(_).await }
    val pointsForEndpoints = endpoints.map { e =>
      var connection = services.getEndpointConnectionByUuid(e.getUuid).await
      while (connection.getRouting.getServiceRoutingKey == "") {
        // wait for measurement processor to come online
        Thread.sleep(100)
        connection = services.getEndpointConnectionByUuid(e.getUuid).await
      }
      val points = e.getOwnerships.getPointsList.toList
      e.getName -> (connection, points)
    }.toMap

    val allPointNames = pointsForEndpoints.values.map { _._2 }.flatten.toList
    val sub = services.subscribeToMeasurementsByNames(allPointNames).await
    val roundtripTimer = new ConcurrentRoundtripTimer(client, sub)

    stream.foreach { _.println("Publishing: " + totalMeas + " measurements using batchSize: " + batchSize + " concurrency: " + concurrency) }
    val results = publishMeasurements(client, pointsForEndpoints, roundtripTimer)
    roundtripTimer.cancel()
    results
  }

  private def publishMeasurements(client: Client, pointsForEndpoints: Map[String, (EndpointConnection, List[String])], roundtripTimer: ConcurrentRoundtripTimer) = {

    val batches = (totalMeas / batchSize).toInt
    val endpointNames = Stream.continually(pointsForEndpoints.keys).flatten.take(batches)

    // prepare the batch publishers (lazy definition because of Stream.continually)
    val batchPublishers = endpointNames.map { endpointName =>
      val publishingClient = client.getRpcInterface(classOf[AllScadaService])

      val (connection, pointsOnEndpoint) = pointsForEndpoints(endpointName)
      val measurementProcessorDestination = new AddressableDestination(connection.getRouting.getServiceRoutingKey)

      val measurements = MeasurementUtility.makeMeasurements(pointsOnEndpoint, batchSize)

      () => {
        val roundtripPromise = roundtripTimer.timeRoundtrip(measurements.toList)
        publishingClient.publishMeasurements(measurements.toList, measurementProcessorDestination)
        roundtripPromise
      }
    }

    val start = System.nanoTime()
    val results = ModelCreationUtilities.parallelExecutor(client, concurrency, batchPublishers)
    val overallTime = (System.nanoTime() - start) / 1000000

    val readings = mutable.Queue.empty[BenchmarkReading]
    readings.enqueue(new OverallConcurrentMeasurementReading(concurrency, batchSize, overallTime))

    results.foreach {
      case (time, timerResults) =>
        readings.enqueue(new ConcurrentMeasurementReading(concurrency, batchSize, time, timerResults.firstMessage, timerResults.lastMessage))
    }
    readings.toList
  }

}