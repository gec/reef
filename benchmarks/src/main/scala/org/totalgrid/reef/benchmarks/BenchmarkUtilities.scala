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

import org.totalgrid.reef.client.sapi.client.rest.Client
import java.io.PrintStream
import org.totalgrid.reef.benchmarks.output.{ DelimitedFileOutput, TeamCityStatisticsXml }

object BenchmarkUtilities {
  def runTests(client: Client, stream: Option[PrintStream], tests: List[BenchmarkTest]): Map[String, List[BenchmarkReading]] = {
    val allResults = tests.map(_.runTest(client, stream)).flatten
    allResults.groupBy(_.csvName)
  }

  def writeTeamCityFile(histogramResults: List[Histogram]) {
    val teamCity = new TeamCityStatisticsXml("teamcity-info.xml")
    histogramResults.foreach { h =>
      h.outputsWithLabels.foreach {
        case (label, value) => teamCity.addRow(label, value)
      }
    }
    teamCity.close()
  }

  def writeCsvFiles(resultsByFileName: Map[String, scala.List[BenchmarkReading]], baseName: String = "") {
    stableForeach(resultsByFileName) { (csvName, results) =>
      val output = new DelimitedFileOutput(baseName + csvName + ".csv", false)

      output.addRow(results.head.columnNames)
      results.foreach { r => output.addRow(r.values.map { _.toString }) }
      output.close()
    }
  }

  def writeHistogramCsvFiles(histogramResults: List[Histogram], baseName: String = "histograms") {

    val output = new DelimitedFileOutput(baseName + ".csv", false)

    output.addRow("testType,parameters,fieldName,min,max,average,count".split(",").toList)
    histogramResults.foreach { h => output.addRow(h.values.map { _.toString }) }
    output.close()
  }

  /**
   * output all of the results in csv, and histogram data in both csv and teamcity format
   */
  def outputResults(resultsByFileName: Map[String, List[BenchmarkReading]]) {

    val histogramResults = Histogram.getHistograms(resultsByFileName)

    writeTeamCityFile(histogramResults)
    writeHistogramCsvFiles(histogramResults)

    writeCsvFiles(resultsByFileName)
  }

  def stableForeach[A](collection: Map[String, A])(fun: (String, A) => Unit) {
    stableMap[A, Unit](collection)(fun)
  }

  def stableMap[A, B](collection: Map[String, A])(fun: (String, A) => B) = {
    collection.keys.toList.sorted.map { name =>
      fun(name, collection(name))
    }
  }

  def takeRandom[A](max: Int, list: List[A]): List[A] = {
    if (list.size < max) list
    else {
      scala.util.Random.shuffle(list).take(max)
    }
  }
}