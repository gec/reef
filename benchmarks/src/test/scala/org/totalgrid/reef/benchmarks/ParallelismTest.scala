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
import org.totalgrid.reef.benchmarks.system.EndpointLoaderBenchmark
import net.agileautomata.executor4s._
import org.totalgrid.reef.standalone.IntegratedSystem
import org.scalatest.{ BeforeAndAfterAll, FunSuite }

@RunWith(classOf[JUnitRunner])
class ParallelismTest extends FunSuite with BeforeAndAfterAll {

  def client(system: IntegratedSystem) = {
    val c = system.connection.login(system.userSettings.getUserName, system.userSettings.getUserPassword).await
    c.setHeaders(c.getHeaders.setTimeout(120000))
    c.setHeaders(c.getHeaders.setResultLimit(10000))

    c
  }

  var exe: ExecutorService = null
  var system: IntegratedSystem = null
  var readings = List.empty[BenchmarkReading]

  override protected def beforeAll() {
    exe = Executors.newResizingThreadPool(5.minutes)
    system = new IntegratedSystem(exe, "../standalone-node.cfg", true)
  }

  override protected def afterAll() {
    system.stop()
    exe.terminate()
    val results = readings.groupBy(_.csvName)
    val histogramResults = Histogram.getHistograms(results)

    BenchmarkUtilities.writeHistogramCsvFiles(histogramResults, "endpointLoading")
    BenchmarkUtilities.writeCsvFiles(results, "endpointLoading-")
  }

  val endpoints = 10
  val pointsPerEndpoint = 20
  val parallelisms = List(1, 5, 10)
  val batchSize = 1000

  parallelisms.foreach { threads =>
    test("Loading " + endpoints + " endpoints with " + pointsPerEndpoint + " points each with " + threads + " loaders") {

      val test = new EndpointLoaderBenchmark(endpoints, pointsPerEndpoint, threads, batchSize)
      readings :::= test.runTest(client(system), Some(Console.out))
    }
  }

}