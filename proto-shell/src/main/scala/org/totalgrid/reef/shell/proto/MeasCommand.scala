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

import org.apache.felix.gogo.commands.{ Command, Argument, Option => GogoOption }

import org.totalgrid.reef.shell.proto.presentation.{ MeasView }

import scala.collection.JavaConversions._

import java.io.File
import java.text.SimpleDateFormat
import org.totalgrid.reef.client.service.proto.Model.Point
import org.totalgrid.reef.client.service.proto.Measurements.{ Quality, Measurement }

@Command(scope = "meas", name = "list", description = "Prints all measurements or a specified measurement.")
class MeasListCommand extends ReefCommandSupport {

  @Argument(index = 0, name = "name", description = "Measurement name.", required = false, multiValued = false)
  var name: String = null

  @GogoOption(name = "-block", description = "Show only operator blocked measurements (aka. Not in Service).", required = false, multiValued = false)
  private var block = false

  @GogoOption(name = "-override", description = "Show only overridden measurements (aka. Substituted).", required = false, multiValued = false)
  private var override_ = false

  @GogoOption(name = "-good", description = "Show only measurements with validity good.", required = false, multiValued = false)
  private var good = false

  @GogoOption(name = "-invalid", description = "Show only measurements with validity invalid.", required = false, multiValued = false)
  private var invalid = false

  @GogoOption(name = "-questionable", description = "Show only measurements with validity questionable.", required = false, multiValued = false)
  private var questionable = false

  def doCommand() = {
    Option(name) match {
      case Some(measName) => print(getListByNameOrParentName(name))
      case None => print(getAllMeasurements)
    }
  }

  def getAllMeasurements = {
    val points = services.getPoints
    services.getMeasurementsByPoints(points).toList
  }

  def getListByNameOrParentName(name: String): List[Measurement] = {

    Option(services.findMeasurementByName(name)).map { List(_) }.getOrElse {
      // if didn't find point lets assume its an entity
      val entity = services.getEntityByName(name)
      val pointEntites = services.getPointsOwnedByEntity(entity)

      services.getMeasurementsByNames(pointEntites.map { _.getName }).toList
    }
  }

  def print(measurements: List[Measurement]) = {
    if (measurements.size == 1) {
      MeasView.printInspect(measurements(0))
    } else {
      if (!block && !override_ && !good && !invalid && !questionable) {
        MeasView.printTable(measurements)
      } else {
        val q = Quality.newBuilder()
        if (block) q.setOperatorBlocked(true)
        if (override_) q.setSource(Quality.Source.SUBSTITUTED)
        if (good) q.setValidity(Quality.Validity.GOOD)
        if (invalid) q.setValidity(Quality.Validity.INVALID)
        if (questionable) q.setValidity(Quality.Validity.QUESTIONABLE)
        MeasView.printTableFilteredByQuality(measurements, q.build)
      }
    }

  }
}

@Command(scope = "meas", name = "block", description = "Block a point so it does recieve measurement updates from the field (aka. Not in Service).")
class MeasBlockCommand extends ReefCommandSupport {

  @Argument(index = 0, name = "name", description = "Measurement name.", required = true, multiValued = false)
  var name: String = null

  def doCommand() = {
    var point = services.getPointByName(name)
    services.setPointOutOfService(point)
    MeasView.printInspect(services.getMeasurementByPoint(point))
  }
}

@Command(scope = "meas", name = "unblock", description = "Unblock a point so it recieves measurement updates from the field (aka. In Service).")
class MeasUnblockCommand extends ReefCommandSupport {

  @Argument(index = 0, name = "name", description = "Measurement name.", required = true, multiValued = false)
  var name: String = null

  def doCommand() = {
    var point = services.getPointByName(name)
    services.clearMeasurementOverridesOnPoint(point)
    MeasView.printInspect(services.getMeasurementByPoint(point))
  }
}

@Command(scope = "meas", name = "override", description = "Override a blocked point with the specified measurement.")
class MeasOverrideCommand extends ReefCommandSupport {

  @Argument(index = 0, name = "name", description = "Measurement name.", required = true, multiValued = false)
  var name: String = null

  @Argument(index = 1, name = "value", description = "Override value.", required = true, multiValued = false)
  var value: String = null

  def doCommand() = {
    var point = services.getPointByName(name)
    var measurement = services.getMeasurementByName(name)
    var m = measurement.toBuilder
    val typ = measurement.getType
    try {
      m = typ match {
        case Measurement.Type.INT => m.setIntVal(value.toLong)
        case Measurement.Type.DOUBLE => m.setDoubleVal(value.toDouble)
        case Measurement.Type.BOOL => m.setBoolVal(value.toBoolean)
        case Measurement.Type.STRING => m.setStringVal(value)
        case Measurement.Type.NONE =>
          // TODO: We'll do better when Point has the type information.
          throw new Exception("Point " + name + " has no measurements. Type is unknown. Measurement not overridden.")
      }
      m.setUnit(measurement.getUnit)
      services.setPointOverride(point, m.build)
      MeasView.printInspect(services.getMeasurementByPoint(point))
    } catch {
      case ex: NumberFormatException =>
        Console.println("ERROR: Expected measurement value of type " + typ.toString + ", but found '" + value + "' instead.")
      case ex: Throwable => throw ex
    }
  }
}

@Command(scope = "meas", name = "override-list", description = "View all overridden measurements")
class MeasOverrideListCommand extends ReefCommandSupport {

  def doCommand() = {
    var overrides = services.getMeasurementOverrides().toList

    MeasView.printTable(overrides.map { _.getMeas })
  }
}

/**
 * provides -w (watch) flag for measurement commands
 */
abstract class MeasViewBaseCommand extends ReefCommandSupport {

  @GogoOption(name = "-w", description = "Watch measurement updates and continuously display new values coming in. Press cntrl-c to stop.", required = false, multiValued = false)
  var watch: Boolean = false

  protected def displayMeasurements(pointNames: List[String]) {
    if (!watch) {
      val meas = services.getMeasurementsByNames(pointNames).toList
      MeasView.printTable(meas)
    } else {
      val subResult = services.subscribeToMeasurementsByNames(pointNames)
      val widths = MeasView.printTable(subResult.getResult.toList)
      runSubscription(subResult.getSubscription) { event =>
        MeasView.printMeasRow(event.getValue, widths)
      }
    }
  }
}

@Command(scope = "meas", name = "from", description = "Prints measurements under an entity.")
class MeasFromCommand extends MeasViewBaseCommand {

  @Argument(index = 0, name = "parentId", description = "Parent entity name.", required = true, multiValued = false)
  var parentName: String = null

  def doCommand(): Unit = {

    val entity = services.getEntityByName(parentName)
    val pointNames = services.getEntityRelatedChildrenOfType(entity.getUuid, "owns", "Point").toList.map { _.getName }

    displayMeasurements(pointNames)
  }
}

@Command(scope = "meas", name = "view", description = "Prints a user specified list of measurements")
class MeasViewCommand extends MeasViewBaseCommand {

  @Argument(index = 0, name = "Point names", description = "Names of all of the points we want to display", required = true, multiValued = true)
  var pointNames: java.util.List[String] = null

  def doCommand(): Unit = {

    val names = pointNames.toList match {
      case List("*") => services.getPoints.toList.map { _.getName }
      case _ => pointNames.toList
    }

    displayMeasurements(names)
  }
}

@Command(scope = "meas", name = "endpoint", description = "Prints measurements under an endpoint.")
class MeasFromEndpointCommand extends MeasViewBaseCommand {

  @Argument(index = 0, name = "endpointName", description = "Endpoint name.", required = true, multiValued = false)
  var endpointName: String = null

  def doCommand(): Unit = {

    val endpoint = services.getEndpointByName(endpointName)
    val pointNames = services.getPointsBelongingToEndpoint(endpoint.getUuid).toList.map { _.getName }

    displayMeasurements(pointNames)
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

@Command(scope = "meas", name = "stat", description = "Prints measurement statistics.")
class MeasStatCommand extends ReefCommandSupport {

  @Argument(index = 0, name = "name", description = "Measurement name.", required = true, multiValued = false)
  var name: String = null

  def doCommand(): Unit = {
    MeasView.printStats(services.getMeasurementStatisticsByName(name))
  }
}

@Command(scope = "meas", name = "download", description = "Download all measurements for a point to CSV file.\n Ex: meas:download -s \"2012-02-10 00:00\" PV.csv LV.Line_PV.kW_tot")
class MeasDownloadCommand extends ReefCommandSupport {

  @Argument(index = 0, name = "fileName", description = "Absolute filename to write csv file.", required = true, multiValued = false)
  var fileName: String = null

  @Argument(index = 1, name = "pointNames", description = "Point names.", required = true, multiValued = true)
  var names: java.util.List[String] = null

  //@GogoOption(name = "-i", description = "Chunk size", required = false, multiValued = false)
  var chunkSize: Int = 1000

  @GogoOption(name = "-c", description = "Columns in CSV file", required = false, multiValued = false)
  var columnString: String = "name,longTime,shortTime,value,shortQuality,longQuality,unit"

  @GogoOption(name = "-s", description = "Start time as \"yyyy-MM-dd HH:mm\" or milliseconds, defaults to 0", required = false, multiValued = false)
  var startTime: String = null

  @GogoOption(name = "-e", description = "End time as \"yyyy-MM-dd HH:mm\" or milliseconds, defaults to now", required = false, multiValued = false)
  var endTime: String = null

  @GogoOption(name = "-oh", description = "Offest Hours, number of hours before end time", required = false, multiValued = false)
  var hoursAgo: Int = 0

  @GogoOption(name = "-om", description = "Offest Minutes, number of minutes before end time", required = false, multiValued = false)
  var minutesAgo: Int = 0

  case class LastPoint(point: Point, var startTime: Long, var hasMore: Boolean, var measurements: List[Measurement], var totalRead: Int)

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

    val endTimeAsMillis = asMillis(endTime, System.currentTimeMillis)

    val startDefault = if (hoursAgo != 0 || minutesAgo != 0) endTimeAsMillis - ((hoursAgo * 60 * 60) + (minutesAgo * 60)) * 1000 else 0
    val startTimeAsMillis = asMillis(startTime, startDefault)

    // get the points before creating the file
    val points = names.toList.map { name => new LastPoint(services.getPointByName(name), startTimeAsMillis, true, Nil, 0) }
    val f = new File(fileName)

    printToFile(f) { p =>
      addMeasHeader(points.map { _.point }, columns.map { _._1 }, p)
      while (points.find(_.hasMore == true).isDefined) {
        points.foreach { lastPoint =>
          lastPoint.measurements = services.getMeasurementHistory(lastPoint.point, lastPoint.startTime, endTimeAsMillis, false, chunkSize).toList
          val measRead = lastPoint.measurements.size
          lastPoint.hasMore = measRead == chunkSize
          if (lastPoint.hasMore) {
            lastPoint.startTime = lastPoint.measurements.last.getTime + 1
          }
          lastPoint.totalRead += measRead
        }
        addCsvMeas(points.map { _.measurements }, columns.map { _._2 }, p)
      }
    }
    println("Created csv file: " + f.getAbsolutePath())
    points.foreach(lastPoint => println(lastPoint.point.getName + " : " + lastPoint.totalRead + " measurements"))
  }

  private def addCsvMeas(measurementsForPoint: List[List[Measurement]], columns: Seq[Measurement => String], p: java.io.PrintWriter) {
    val measurements: List[List[Measurement]] = padAndTranspose(measurementsForPoint, null)
    measurements.foreach { l: List[Measurement] =>
      p.println(l.map { m: Measurement =>
        columns.map { func =>
          if (m == null) "" else func(m)
        }
      }.flatten.mkString(","))
    }
  }

  private def addMeasHeader(points: Seq[Point], columns: Seq[String], p: java.io.PrintWriter) {
    p.println(points.map { p => columns }.flatten.mkString(","))
  }

  private def printToFile(f: java.io.File)(op: java.io.PrintWriter => Unit) {
    val p = new java.io.PrintWriter(f)
    try { op(p) } finally { p.close() }
  }

  private def asMillis(str: String, default: Long): Long = {
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

  /**
   * take a list of lists and return a List with zipped together elements of the original lists.
   * Output is same size as longest list with default elements added to fill out zipped sets
   *
   * List(List(a, b, c), List(1, 2), List(6, 7, 8, 9)) =>
   * List(List(a, 1, 6), List(b, 2, 7), List(c, null, 8), List(null, null, 9))
   */
  private def padAndTranspose[T](l: List[List[T]], padding: T): List[List[T]] = {

    def transpose[T](l: List[List[T]]): List[List[T]] = l match {
      case Nil => Nil
      case Nil :: _ => Nil
      case _ => (l map (_.head)) :: transpose(l map (_.tail))
    }

    // get longest entry
    val size = l.map { _.size }.sorted.reverse.head
    // pad them to matching length
    val trans = l.map(_.padTo(size, padding))
    transpose(trans)
  }
}