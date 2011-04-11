/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Green Energy Corp licenses this file
 * to you under the GNU Affero General Public License Version 3.0
 * (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.shell.proto

import org.apache.felix.gogo.commands.{ Command, Argument, Option => GogoOption }

import org.totalgrid.reef.shell.proto.presentation.{ MeasView }

import scala.collection.JavaConversions._
import org.totalgrid.reef.api.request.ReefUUID
import java.io.File
import org.totalgrid.reef.proto.Measurements.Measurement
import java.text.SimpleDateFormat

@Command(scope = "meas", name = "meas", description = "Prints all measurements or a specified measurement.")
class MeasCommand extends ReefCommandSupport {

  @Argument(index = 0, name = "name", description = "Measurement name.", required = false, multiValued = false)
  var name: String = null

  def doCommand() = {
    Option(name) match {
      case Some(measName) => MeasView.printInspect(services.getMeasurementByName(name))
      case None =>
        val points = services.getAllPoints
        MeasView.printTable(services.getMeasurementsByPoints(points).toList)
    }
  }
}

@Command(scope = "meas", name = "from", description = "Prints measurements under an entity.")
class MeasFromCommand extends ReefCommandSupport {

  @Argument(index = 0, name = "parentId", description = "Parent entity name.", required = true, multiValued = false)
  var parentName: String = null

  def doCommand(): Unit = {

    val entity = services.getEntityByName(parentName)
    val pointEntites = services.getEntityRelatedChildrenOfType(new ReefUUID(entity.getUid), "owns", "Point")

    MeasView.printTable(services.getMeasurementsByNames(pointEntites.map { _.getName }).toList)
  }
}

@Command(scope = "meas", name = "hist", description = "Prints recent history for a point.")
class MeasHistCommand extends ReefCommandSupport {

  @Argument(index = 0, name = "name", description = "Measurement name.", required = true, multiValued = false)
  var name: String = null

  @Argument(index = 1, name = "count", description = "Number of previous updates.", required = false, multiValued = false)
  var count: Int = 10

  def doCommand(): Unit = {

    val point = services.getPointByName(name)
    MeasView.printTable(services.getMeasurementHistory(point, count).toList)
  }
}

@Command(scope = "meas", name = "download", description = "Download all measurements for a point to CSV file.")
class MeasDownloadCommand extends ReefCommandSupport {

  @Argument(index = 0, name = "pointName", description = "Point name.", required = true, multiValued = false)
  var name: String = null

  @Argument(index = 1, name = "fileName", description = "Absolute filename to write csv file.", required = true, multiValued = false)
  var fileName: String = null

  //@GogoOption(name = "-i", description = "Chunk size", required = false, multiValued = false)
  var chunkSize: Int = 5000

  @GogoOption(name = "-c", description = "Columns in CSV file", required = false, multiValued = false)
  var columnString: String = "name,longTime,shortTime,value,shortQuality,longQuality,unit"

  @GogoOption(name = "-s", description = "Start time as \"yyyy-MM-dd HH:mm\" or milliseconds", required = false, multiValued = false)
  var startTime: String = null

  @GogoOption(name = "-e", description = "End time as \"yyyy-MM-dd HH:mm\" or milliseconds", required = false, multiValued = false)
  var endTime: String = null

  def doCommand(): Unit = {

    val columnIdentifiers: Array[String] = columnString.split(",")

    val columns = columnIdentifiers.map { ci: String =>
      (ci, ci match {
        case "name" => { m: Measurement => m.getName }
        case "longTime" => { m: Measurement => MeasView.timeString(m) }
        case "shortTime" => { m: Measurement => m.getTime.toString }
        case "value" => { m: Measurement => MeasView.value(m).toString }
        case "shortQuality" => { m: Measurement => MeasView.shortQuality(m) }
        case "longQuality" => { m: Measurement => MeasView.longQuality(m) }
        case "unit" => { m: Measurement => MeasView.unit(m) }
      })
    }

    val startTimeAsMillis = asMillis(startTime, 0)
    val endTimeAsMillis = asMillis(endTime, System.currentTimeMillis)

    // get the point before creating the file
    val point = services.getPointByName(name)
    val f = new File(fileName)

    printToFile(f) { p =>
      addMeasHeader(columns.map { _._1 }, p)
      var measurements = services.getMeasurementHistory(point, startTimeAsMillis, endTimeAsMillis, false, chunkSize).toList
      addCsvMeas(measurements, columns.map { _._2 }, p)
      while (chunkSize == measurements.size) {
        // TODO: add better paging for measurement history
        measurements = services.getMeasurementHistory(point, measurements.last.getTime + 1, endTimeAsMillis, false, chunkSize).toList
        addCsvMeas(measurements, columns.map { _._2 }, p)
      }
    }
  }

  def addCsvMeas(measurements: Seq[Measurement], columns: Seq[Measurement => String], p: java.io.PrintWriter) {
    measurements.foreach { m => p.println(columns.map { _(m) }.mkString(",")) }
  }

  def addMeasHeader(columns: Seq[String], p: java.io.PrintWriter) {
    p.println(columns.mkString(","))
  }

  def printToFile(f: java.io.File)(op: java.io.PrintWriter => Unit) {
    val p = new java.io.PrintWriter(f)
    try { op(p) } finally { p.close() }
  }

  def asMillis(str: String, default: Long): Long = {
    if (str == null) return default
    try {
      return str.toLong
    } catch {
      case nfe: NumberFormatException =>
        val date = new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(str)
        if (date == null) throw new Exception("Couldnt parse " + str + " into a valid date or millisecond value. Format is yyyy-MM-dd HH:mm, remember to enclose argument in quotes. Ex: -s \"2011-05-01 00:00\"")
        return date.getTime
    }
  }
}