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

import org.totalgrid.reef.client.service.proto.Model.Point
import org.totalgrid.reef.benchmarks.{ BenchmarkTest, BenchmarkReading }
import org.totalgrid.reef.client.sapi.rpc.AllScadaService
import java.io.PrintStream
import collection.mutable.Queue

case class MeasurementStat(statName: String, pointName: String, value: Long) extends BenchmarkReading {
  def csvName = "measStats"

  def testParameterNames = List("stat")
  def testParameters = List(statName)

  def testOutputNames = List("pointName", "value")
  def testOutputs = List(pointName, value)
}

class MeasurementStatBenchmark(points: List[Point]) extends BenchmarkTest {
  def runTest(client: AllScadaService, stream: Option[PrintStream]) = {

    val readings = Queue.empty[BenchmarkReading]

    points.foreach { p =>
      stream.foreach { _.println("Getting measurement stats for: " + p.getName) }

      val now = System.currentTimeMillis()

      val totalMeas = client.getMeasurementHistory(p, 10000).await
      readings.enqueue(new MeasurementStat("totalMeas", p.getName, totalMeas.size))
      client.getMeasurementHistory(p, 0, Long.MaxValue, false, 1).await.headOption.map { oldest =>
        readings.enqueue(new MeasurementStat("oldestMeas", p.getName, (now - oldest.getTime) / 1000))
      }
      client.getMeasurementHistory(p, 1).await.headOption.map { newest =>
        readings.enqueue(new MeasurementStat("newestMeas", p.getName, (now - newest.getTime) / 1000))
      }
    }

    readings.toList
  }
}