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
import org.totalgrid.reef.benchmarks.{ BenchmarkReading, BenchmarkTest }
import org.totalgrid.reef.util.Timing
import collection.mutable.Queue
import org.totalgrid.reef.client.service.proto.Model.Point

case class MeasurementHistoryReading(pointName: String, operation: String, time: Long) extends BenchmarkReading {
  def csvName = "measHistory"

  def testParameterNames = List("operation")
  def testParameters = List(operation)

  def testOutputNames = List("pointName", "time")
  def testOutputs = List(pointName, time)
}

class MeasurementHistoryBenchmark(points: List[Point], sizes: List[Int], getAll: Boolean) extends BenchmarkTest {
  def runTest(client: AllScadaService, stream: Option[PrintStream]) = {

    stream.foreach { _.println("Collecting historian stats for: " + points.size + " points using sizes: " + sizes) }

    val readings = Queue.empty[BenchmarkReading]

    points.foreach { p =>
      stream.foreach { _.println("Getting historian readings for: " + p.getName) }
      def time[A](name: String)(fun: => A): A = {
        Timing.time { t => readings.enqueue(new MeasurementHistoryReading(p.getName, name, t)) } {
          fun
        }
      }
      sizes.foreach { size =>
        time("oldest" + size + "Meas") {
          client.getMeasurementHistory(p, 0, Long.MaxValue, false, size).await
        }
        time("newest" + size + "Meas") {
          client.getMeasurementHistory(p, size).await
        }
      }
      if (getAll) {
        time("allMeas") {
          client.getMeasurementHistory(p, 10000).await
        }
      }
    }

    readings.toList
  }
}