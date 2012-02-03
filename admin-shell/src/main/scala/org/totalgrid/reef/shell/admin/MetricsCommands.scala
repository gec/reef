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
package org.totalgrid.reef.shell.admin

import org.apache.felix.gogo.commands.{ Command, Argument, Option }
import org.apache.karaf.shell.console.OsgiCommandSupport
import org.totalgrid.reef.metrics.{ MetricsMapHelpers, CSVMetricPublisher, MetricsSink }

/**
 * base class, keeps the state filters, calculations and output settings
 * for all of the metrics commands
 */
abstract class MetricsCommands extends OsgiCommandSupport {

  // note we use an anonymous function for the session retriever (since its not ready at construction)
  val filters = new SessionHeldList[String]("metrics.filters", { this.session })
  val calculations = new SessionHeldList[String]("metrics.calcs", { this.session })

  val outputToScreen = new SessionHeldObject[Boolean]("metrics.toScreen", { this.session }, true)
  val outputToCSV = new SessionHeldObject[scala.Option[String]]("metrics.csv", { this.session }, None)

  def output(pubValues: Map[String, Any]) = {
    val names = pubValues.keys.toList.sorted
    if (outputToScreen.get) names.foreach { name => println(name + " => " + pubValues(name)) }
    outputToCSV.get.foreach { fileName =>
      // TODO: move csv publisher to admin package?
      val publisher = new CSVMetricPublisher(fileName)
      publisher.publishValues(pubValues)
      publisher.close
    }
  }

  def getMetrics(executeCalculations: Boolean = true) = {
    val rawMetrics = MetricsSink.values(filters.get)
    if (executeCalculations) MetricsMapHelpers.performCalculations(rawMetrics, calculations.get) else rawMetrics
  }
}

@Command(scope = "metrics", name = "reset", description = "Reset metrics counters")
class MetricsReset extends MetricsCommands {

  override def doExecute(): Object = {
    MetricsSink.reset(filters.get)
    null
  }
}

@Command(scope = "metrics", name = "metrics", description = "Output the current metrics the channels setup in metrics:outputs")
class MetricsShow extends MetricsCommands {

  override def doExecute(): Object = {
    output(getMetrics())
    null
  }
}

@Command(scope = "metrics", name = "outputs", description = "Output the metrics to a csv file (metrics.csv by default)")
class MetricsOutputs extends MetricsCommands {

  @Argument(index = 0, name = "csvFile", description = "Path to CSV file to generate", required = false, multiValued = false)
  private var csvFile: String = null

  @Option(name = "-quiet", aliases = Array[String](), description = "Suppress screen output", required = false, multiValued = false)
  private var quiet: Boolean = false

  override def doExecute(): Object = {
    outputToScreen.set(!quiet)
    outputToCSV.set(scala.Option(csvFile))
    null
  }
}

@Command(scope = "metrics", name = "rates", description = "Get metrics twice seperated by X millis and calcs rates")
class MetricsRates extends MetricsCommands {

  @Argument(index = 0, name = "collectionTime", description = "Time to wait for rate calculation (seconds)", required = false, multiValued = false)
  private var time: Long = 10

  override def doExecute(): Object = {
    val startValues = getMetrics(false)

    println("Waiting " + time + " seconds for rates")
    Thread.sleep(time * 1000)

    val endValues = getMetrics(false)
    val pubValues = MetricsMapHelpers.changePerSecond(startValues, endValues, time * 1000)
    val calcedValues = MetricsMapHelpers.performCalculations(pubValues, calculations.get)

    output(calcedValues)

    null
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

  override def doExecute(): Object = {
    if (clearAll) filters.clear
    if (filterKey != null) {
      if (removeFilters) filters.remove(filterKey)
      else filters.add(filterKey)
    }
    null
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

  override def doExecute(): Object = {

    val preFilteredMetrics = MetricsSink.values(List(key))

    val calcs = MetricsMapHelpers.sumAndCount(preFilteredMetrics, key)
    val calcsPlusSourceData = MetricsMapHelpers.mergeMap(List(preFilteredMetrics, calcs))

    output(calcsPlusSourceData)

    if (!dontSave) calculations.add(key)

    null
  }
}

@Command(scope = "metrics", name = "throughput", description = "Displays current state and overall rate for a key.")
class MetricsThroughput extends MetricsCommands {

  @Argument(index = 0, name = "key", description = "Key to do operation on (usually needs wildcard \"*\" to be useful)", required = true, multiValued = false)
  private var key: String = null

  @Option(name = "-time", aliases = Array[String](), description = "Dont run the same calc on future metrics reads", required = false, multiValued = false)
  private var time: Int = 10

  override def doExecute(): Object = {

    val filters = List(key)

    val startValues = MetricsSink.values(filters)
    val totals = MetricsMapHelpers.performCalculations(startValues, List(key))

    println("Totals:")
    output(totals)

    println("Waiting " + time + " seconds for rates")
    println
    Thread.sleep(time * 1000)
    println("Rates:")

    val endValues = MetricsSink.values(filters)

    val valuesWithRates = MetricsMapHelpers.changePerSecond(startValues, endValues, time * 1000)

    val calcedRates = MetricsMapHelpers.performCalculations(valuesWithRates, List(key + ".Rate"))

    output(calcedRates)

    null
  }
}