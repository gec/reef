/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the GNU Affero General Public License
 * Version 3.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.shell.proto

import org.apache.karaf.shell.console.OsgiCommandSupport
import org.totalgrid.reef.metrics.client.{ MetricsMapHelpers, CSVMetricPublisher }
import org.apache.felix.gogo.commands.{ Argument, Command, Option }
import org.totalgrid.reef.client.service.AllScadaService
import org.totalgrid.reef.metrics.client.MetricsService
import org.totalgrid.reef.metrics.client.proto.Metrics.MetricsRead
import scala.collection.JavaConversions._

/**
 * base class, keeps the state filters, calculations and output settings
 * for all of the metrics commands
 */
abstract class MetricsCommands extends ReefCommandSupport {

  val filters = list[String]("metrics.filters")
  val calculations = list[String]("metrics.calcs")

  val outputToScreen = obj[Boolean]("metrics.toScreen", true)
  val outputToCSV = obj[scala.Option[String]]("metrics.csv", None)

  protected def metrics: MetricsService = {
    this.reefClient.getRpcInterface(classOf[MetricsService])
  }

  def output(pubValues: Map[String, Any]) = {
    if (outputToScreen.get) {
      printMetrics(pubValues)
    }
    outputToCSV.get.foreach { fileName =>
      // TODO: move csv publisher to admin package?
      val publisher = new CSVMetricPublisher(fileName)
      publisher.publishValues(pubValues)
      publisher.close
    }
  }

  import scala.collection.JavaConversions._
  def getMetrics(executeCalculations: Boolean = true) = {
    val rawMetrics = fromProto(metrics.getMetrics())
    if (executeCalculations) MetricsMapHelpers.performCalculations(rawMetrics, calculations.get) else rawMetrics
  }

  def fromProto(rd: MetricsRead): Map[String, Any] = {
    rd.getResultsList.toList.map(r => (r.getName, r.getValue)).toMap
  }

  def printMetrics(values: Map[String, Any]) {
    val names = values.keys.toList.sorted
    names.foreach { name => println(name + " => " + values(name)) }
  }

}

@Command(scope = "metrics", name = "reset", description = "Reset metrics counters")
class MetricsReset extends MetricsCommands {

  def doCommand() {
    filters.get() match {
      case Nil => printMetrics(fromProto(metrics.resetMetrics()))
      case l: List[String] => printMetrics(fromProto(metrics.resetMetricsWithFilters(l)))
    }
  }
}

@Command(scope = "metrics", name = "metrics", description = "Output the current metrics the channels setup in metrics:outputs")
class MetricsShow extends MetricsCommands {

  def doCommand() {
    output(getMetrics())
  }
}

@Command(scope = "metrics", name = "outputs", description = "Output the metrics to a csv file (metrics.csv by default)")
class MetricsOutputs extends MetricsCommands {

  @Argument(index = 0, name = "csvFile", description = "Path to CSV file to generate", required = false, multiValued = false)
  private var csvFile: String = null

  @Option(name = "-quiet", aliases = Array[String](), description = "Suppress screen output", required = false, multiValued = false)
  private var quiet: Boolean = false

  def doCommand() {
    outputToScreen.set(!quiet)
    outputToCSV.set(scala.Option(csvFile))
  }
}

@Command(scope = "metrics", name = "rates", description = "Get metrics twice seperated by X millis and calcs rates")
class MetricsRates extends MetricsCommands {

  @Argument(index = 0, name = "collectionTime", description = "Time to wait for rate calculation (seconds)", required = false, multiValued = false)
  private var time: Long = 10

  def doCommand() {
    val startValues = getMetrics(false)

    println("Waiting " + time + " seconds for rates")
    Thread.sleep(time * 1000)

    val endValues = getMetrics(false)
    val pubValues = MetricsMapHelpers.changePerSecond(startValues, endValues, time * 1000)
    val calcedValues = MetricsMapHelpers.performCalculations(pubValues, calculations.get)

    output(calcedValues)
  }
}

@Command(scope = "metrics", name = "filters", description = "Add filters to what metrics are displayed")
class MetricsFilter extends MetricsCommands {

  @Argument(index = 0, name = "filterKey", description = "Key to add to filter list", required = false, multiValued = false)
  private var filterKey: String = null

  @Option(name = "-clear", aliases = Array[String](), description = "Clear the set filter list", required = false, multiValued = false)
  private var clearAll = false

  @Option(name = "-remove", aliases = Array[String](), description = "Remove a key from the list", required = false, multiValued = false)
  private var removeFilters: Boolean = false

  def doCommand() {
    if (clearAll) filters.clear
    if (filterKey != null) {
      if (removeFilters) filters.remove(filterKey)
      else filters.add(filterKey)
    }
  }
}

@Command(scope = "metrics", name = "calcs", description = "Add calculations on metrics points")
class MetricsCalcs extends MetricsCommands {

  @Argument(index = 0, name = "key", description = "Key to do operation on (needs wildcard \"*\" to be useful)", required = true, multiValued = false)
  private var key: String = null

  // TODO: implement calculations other than sumCount
  @Option(name = "-sumCount", aliases = Array[String](), description = "Sum up and count matching metrics generating \"key.Sum\" and \"key.Count\" points", required = false, multiValued = false)
  private var sumAndCount: Boolean = true

  @Option(name = "-dontSave", aliases = Array[String](), description = "Dont run the same calc on future metrics reads", required = false, multiValued = false)
  private var dontSave: Boolean = false

  def doCommand() {
    val preFilteredMetrics = fromProto(metrics.getMetricsWithFilter(key))

    val calcs = MetricsMapHelpers.sumAndCount(preFilteredMetrics, key)
    val calcsPlusSourceData = MetricsMapHelpers.mergeMap(List(preFilteredMetrics, calcs))

    output(calcsPlusSourceData)

    if (!dontSave) calculations.add(key)
  }
}

@Command(scope = "metrics", name = "throughput", description = "Displays current state and overall rate for a key.")
class MetricsThroughput extends MetricsCommands {

  @Argument(index = 0, name = "key", description = "Key to do operation on (usually needs wildcard \"*\" to be useful)", required = true, multiValued = false)
  private var key: String = null

  @Option(name = "-time", aliases = Array[String](), description = "Dont run the same calc on future metrics reads", required = false, multiValued = false)
  private var time: Int = 10

  def doCommand() {

    val startValues = fromProto(metrics.getMetricsWithFilter(key))
    val totals = MetricsMapHelpers.performCalculations(startValues, List(key))

    println("Totals:")
    output(totals)

    println("Waiting " + time + " seconds for rates")
    println
    Thread.sleep(time * 1000)
    println("Rates:")

    val endValues = fromProto(metrics.getMetricsWithFilter(key))

    val valuesWithRates = MetricsMapHelpers.changePerSecond(startValues, endValues, time * 1000)

    val calcedRates = MetricsMapHelpers.performCalculations(valuesWithRates, List(key + ".Rate"))

    output(calcedRates)
  }
}