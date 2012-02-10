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

import org.totalgrid.reef.benchmarks._
import org.totalgrid.reef.client.sapi.rpc.AllScadaService
import java.io.PrintStream
import collection.mutable.Queue
import org.totalgrid.reef.util.Timing

case class MeasurementStat(statName: String, pointName: String, value: Long) extends BenchmarkReading {
  def csvName = "measStats"

  def testParameterNames = List("stat")
  def testParameters = List(statName)

  def testOutputNames = List("pointName", "value")
  def testOutputs = List(pointName, value)
}

class MeasurementStatBenchmark(pointNames: List[String]) extends AllScadaServicesTest {
  def runTest(client: AllScadaService, stream: Option[PrintStream]) = {

    val readings = Queue.empty[BenchmarkReading]

    pointNames.foreach { pointName =>
      stream.foreach { _.println("Getting measurement stats for: " + pointName) }
      val stats = Timing.time { t => readings.enqueue(new MeasurementHistoryReading(pointName, "measStat", t)) } {
        client.getMeasurementStatisticsByName(pointName).await
      }

      readings.enqueue(new MeasurementStat("totalMeas", pointName, stats.getCount))

      val now = System.currentTimeMillis()
      readings.enqueue(new MeasurementStat("oldestMeas", pointName, (now - stats.getOldestTime) / 1000))
    }

    readings.toList
  }
}