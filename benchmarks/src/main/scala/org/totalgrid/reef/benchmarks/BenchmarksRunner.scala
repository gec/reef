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

import org.totalgrid.reef.client.sapi.rpc.AllScadaService
import org.totalgrid.reef.benchmarks.measurements._
import org.totalgrid.reef.benchmarks.system._
import org.totalgrid.reef.benchmarks.endpoints.EndpointManagementBenchmark
import org.totalgrid.reef.benchmarks.output.DelimitedFileOutput
import org.totalgrid.reef.client.sapi.client.rest.Client
import org.totalgrid.reef.loader.commons.LoaderServicesList

import MeasurementCurrentValueBenchmark._
import java.util.Properties
import org.totalgrid.reef.metrics.client.{ MetricsMapHelpers, MetricsService, MetricsServiceList }

import BenchmarkUtilities._

object BenchmarksRunner {

  def runAllTests(client: Client, properties: Properties) {
    client.addServicesList(new LoaderServicesList())
    client.addServicesList(new MetricsServiceList())
    client.setHeaders(client.getHeaders.setTimeout(60000))
    client.setHeaders(client.getHeaders.setResultLimit(10000))
    val services = client.getRpcInterface(classOf[AllScadaService])

    val stream = Some(Console.out)

    var tests = List.empty[BenchmarkTest]

    val options = new SimpleOptionsHandler(properties, "org.totalgrid.reef.benchmarks")

    if (options.getBool("live.enabled")) {
      val c = options.subOptions("live")
      val allPoints = services.getPoints().await.map { _.getName }

      tests ::= new SystemStateBenchmark(c.getInt("requestAttempts"))
      tests ::= new MeasurementStatBenchmark(takeRandom(c.getInt("measStatPoints"), allPoints))
      tests ::= new MeasurementHistoryBenchmark(takeRandom(c.getInt("measStatPoints"), allPoints), c.getIntList("measHistorySizes"), true)
      tests ::= new MeasurementCurrentValueBenchmark(allPoints, testSizes(allPoints.size), c.getInt("measCurrentValueAttempts"))

      if (c.getBool("endpointManagementEnabled")) {
        val protocols = c.getStringList("endpointManagementProtocols")
        val endpointNames = protocols.map { p => services.getEndpoints().await.filter(_.getProtocol == p).map { _.getName } }.flatten

        if (!endpointNames.isEmpty) {
          tests ::= new EndpointManagementBenchmark(endpointNames, c.getInt("endpointManagementCycles"))
        }
      }
    }

    if (options.getBool("measthroughput.enabled")) {
      val c = options.subOptions("measthroughput")

      val concurrentEndpointNames = (1 to c.getInt("numEndpoints")).map { i => "Endpoint" + i }.toList
      val pointsPerEndpoint = c.getInt("numPointsPerEndpoint")
      val pointNames = concurrentEndpointNames.map { ModelCreationUtilities.getPointNames(_, pointsPerEndpoint) }.flatten

      val endpointLoadingWriters = c.getInt("endpointWriters")
      val endpointLoadingBatchSize = c.getInt("endpointBatchSize")

      val totalMeasurements = c.getIntList("publishMeasTotal")
      val publishingWriters = c.getIntList("publishMeasWriters")
      val publishingBatchSizes = c.getIntList("publishMeasBatchSizes")
      val subscribers = c.getIntList("subscribers")

      if (c.getBool("addEndpoints")) {
        tests ::= new EndpointLoaderBenchmark(concurrentEndpointNames, pointsPerEndpoint,
          endpointLoadingWriters, endpointLoadingBatchSize, true, false)
      }
      totalMeasurements.foreach { measurements =>
        publishingWriters.foreach { writers =>
          publishingBatchSizes.foreach { batchSize =>
            subscribers.foreach { subs =>
              tests ::= new ConcurrentMeasurementPublishingBenchmark(concurrentEndpointNames, measurements, writers, batchSize, subs)
            }
          }
        }
      }
      if (c.getBool("measTestReads")) {
        tests ::= new MeasurementStatBenchmark(takeRandom(c.getInt("measStatPoints"), pointNames))
        tests ::= new MeasurementHistoryBenchmark(takeRandom(c.getInt("measHistoryPoints"), pointNames), c.getIntList("measHistorySizes"), false)
        tests ::= new MeasurementCurrentValueBenchmark(pointNames, testSizes(pointNames.size), c.getInt("measCurrentValueAttempts"))
      }
      if (c.getBool("removeEndpoints")) {
        tests ::= new EndpointLoaderBenchmark(concurrentEndpointNames, pointsPerEndpoint,
          endpointLoadingWriters, endpointLoadingBatchSize, false, true)
      }
    }

    val baseName = options.getString("outputFileBaseName")
    val metricsClient = client.getRpcInterface(classOf[MetricsService])

    def runAllTests() = tests.reverse.map(_.runTest(client, stream)).flatten

    val allResults = if (options.getBool("collectServerSideMetrics")) {
      captureServerSideMetrics(metricsClient, baseName + "serverSideMetrics.csv") {
        runAllTests()
      }
    } else {
      runAllTests()
    }
    outputResults(allResults, baseName)
  }

  def captureServerSideMetrics[A](metricsClient: MetricsService, fileName: String)(fun: => A) = {
    val initialSnapshot = MetricsMapHelpers.fromProto(metricsClient.getMetrics())
    try {
      fun
    } finally {
      val postTestSnapshot = MetricsMapHelpers.fromProto(metricsClient.getMetrics())
      val differences = MetricsMapHelpers.difference(initialSnapshot, postTestSnapshot)

      val output = new DelimitedFileOutput(fileName, false)
      output.addRow(List("name", "value"))
      stableForeach(postTestSnapshot) { (name, value) =>
        output.addRow(List(name, value.toString))
      }
      stableForeach(differences) { (name, value) =>
        output.addRow(List(name + "-diff", value.toString))
      }
      output.close()
    }
  }

  def outputResults(allResults: List[BenchmarkReading], baseName: String) {

    val resultsByFileName = allResults.groupBy(_.csvName)

    val histogramResults = Histogram.getHistograms(resultsByFileName)

    writeHistogramCsvFiles(histogramResults, baseName + "averages")
    writeTeamCityFile(histogramResults)

    writeCsvFiles(resultsByFileName, baseName)
  }

}