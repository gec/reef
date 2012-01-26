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
import org.totalgrid.reef.util.Timing
import collection.mutable.Queue

case class MeasurementCurrentValueReading(points: Int, time: Long) extends BenchmarkReading {
  def csvName = "measCurrent"

  def testParameterNames = List("points")
  def testParameters = List(points)

  def testOutputNames = List("time")
  def testOutputs = List(time)
}

object MeasurementCurrentValueBenchmark {
  def testSizes(pointsCount: Int) = {
    List(1, 10, 20, pointsCount / 8, pointsCount / 6, pointsCount / 4, pointsCount / 3, pointsCount / 2, pointsCount)
  }
}

class MeasurementCurrentValueBenchmark(pointNames: List[String], sizes: List[Int], attempts: Int) extends AllScadaServicesTest {
  def runTest(client: AllScadaService, stream: Option[PrintStream]) = {

    stream.foreach { _.println("Collecting currentValue stats for: " + pointNames.size + " points using sizes: " + sizes) }

    val readings = Queue.empty[BenchmarkReading]

    def time[A](points: Int)(fun: => A): A = {
      Timing.time { t => readings.enqueue(new MeasurementCurrentValueReading(points, t)) } {
        fun
      }
    }
    (1 to attempts).foreach { i =>
      sizes.foreach { size =>
        time(size) {
          client.getMeasurementsByNames(BenchmarkUtilities.takeRandom(size, pointNames)).await
        }
      }
    }

    readings.toList
  }
}