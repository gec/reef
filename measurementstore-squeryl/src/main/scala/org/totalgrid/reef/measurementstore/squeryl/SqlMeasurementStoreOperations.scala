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
package org.totalgrid.reef.measurementstore.squeryl

import org.totalgrid.reef.proto.Measurements.{ Measurement => Meas }
import org.squeryl.PrimitiveTypeMode._

/**
 * operations on the SqlMeasurementStoreSchema that implement the MeasurementStore interface. All operations
 * assume they are being run from inside a database transaction.
 */
trait SqlMeasurementStoreOperations {
  private def makeUpdate(m: Meas, pNameMap: Map[String, Long]): Measurement = {
    new Measurement(pNameMap.get(m.getName).get, m.getTime, m.toByteString.toByteArray)
  }

  private def makeCurrentValue(m: Meas, pNameMap: Map[String, Long]): CurrentValue = {
    new CurrentValue(pNameMap.get(m.getName).get, m.toByteString.toByteArray)
  }

  def reset(): Boolean = {
    SqlMeasurementStoreSchema.reset
    true
  }

  def totalValues(): Long = {
    from(SqlMeasurementStoreSchema.updates)(u => compute(count(u.id)))
  }

  def trim(numPoints: Long): Long = {
    val counts = totalValues()
    if (numPoints < counts) {
      // to trim to a specific # of points we can take advantage of the ID column being an auto-incremented value
      // we use the currentValue table to get the most recently updated point (which should have highest id value)
      val mostRecentUpdate = from(SqlMeasurementStoreSchema.updates)(n => select(n) orderBy (n.id.desc)).page(0, 1).head
      // then delete all records with id less than the most recent update - numpoints we want in system
      SqlMeasurementStoreSchema.updates.deleteWhere(u => u.id.~ <= mostRecentUpdate.id - numPoints)
      counts - numPoints
    } else {
      0
    }
  }

  def points(): List[String] = {
    SqlMeasurementStoreSchema.names.where(t => true === true).toList.map { _.name }
  }

  def set(meas: Seq[Meas]) {
    // setup list of all the points we are trying to find ids for
    var insertedMeas: Map[String, Long] = meas.map { _.getName -> (-1: Long) }.toMap

    // ask db for points it has, update the map with those ids
    val pNames = SqlMeasurementStoreSchema.names.where(n => n.name in insertedMeas.keys).toList
    pNames.foreach { p => insertedMeas = insertedMeas - p.name + (p.name -> p.id) }

    // make a list of all of the new Points we need to add to the database
    val newNames = insertedMeas.foldLeft(Nil: List[Option[MeasName]]) { (list, entry) =>
      (if (entry._2 != -1) None else Some(new MeasName(entry._1))) :: list
    }.flatten

    if (newNames.nonEmpty) {
      // if we have new measNames to add do so, then read them back out to get the ids
      SqlMeasurementStoreSchema.names.insert(newNames)
      val addedNames = SqlMeasurementStoreSchema.names.where(n => n.name in newNames.map { _.name }).toList
      addedNames.foreach { p => insertedMeas = insertedMeas - p.name + (p.name -> p.id) }

      val addedCurrentValues = addedNames.map { p => new CurrentValue(p.id, new Array[Byte](0)) }
      SqlMeasurementStoreSchema.currentValues.insert(addedCurrentValues)
    }

    // create the list of measurements to upload
    val toInsert = meas.map { makeUpdate(_, insertedMeas) }.toList
    SqlMeasurementStoreSchema.updates.insert(toInsert)

    val toUpdate = meas.map { makeCurrentValue(_, insertedMeas) }.toList
    SqlMeasurementStoreSchema.currentValues.update(toUpdate)
  }

  def get(names: Seq[String]): Map[String, Meas] = {
    var m = Map.empty[String, Meas]
    var insertedMeas = Map.empty[Long, String]
    val pNames = SqlMeasurementStoreSchema.names.where(n => n.name in names).toList
    pNames.foreach { p => insertedMeas = insertedMeas - p.id + (p.id -> p.name) }

    val ids = insertedMeas.keys.toList
    val cvs = from(SqlMeasurementStoreSchema.currentValues)(cv =>
      where(cv.id in ids) select (cv.id, cv.proto))
    cvs.foreach { case (pid, proto) => m = m + (insertedMeas.get(pid).get -> Meas.parseFrom(proto)) }

    m
  }

  def numValues(meas_name: String): Int = {
    val m = from(SqlMeasurementStoreSchema.updates, SqlMeasurementStoreSchema.names)(
      (u, n) => where(u.pointId === n.id and n.name === meas_name) compute (count(u.id)))
    val q = m.head.measures

    q.toInt
  }

  def remove(names: Seq[String]): Unit = {
    val nameRows = SqlMeasurementStoreSchema.names.where(u => u.name in names).toList.map { _.id }
    if (nameRows.nonEmpty) {
      SqlMeasurementStoreSchema.updates.deleteWhere(u => u.pointId in nameRows)
      SqlMeasurementStoreSchema.names.deleteWhere(n => n.id in nameRows)
      SqlMeasurementStoreSchema.currentValues.deleteWhere(u => u.pointId in nameRows)
    }
  }

  def getInRange(meas_name: String, begin: Long, end: Long, max: Int, ascending: Boolean): Seq[Meas] = {

    val meases = getHistory(meas_name, begin, end, max, ascending)
    val list = meases.map(m => Meas.parseFrom(m.proto))
    list
  }

  private def getHistory(meas_name: String, begin: Long, end: Long, max: Int, ascending: Boolean): Seq[Measurement] = {
    // make start/end arguments optional
    val beginO = if (begin == 0) None else Some(begin)
    val endO = if (end == Long.MaxValue) None else Some(end)

    val meases =
      from(SqlMeasurementStoreSchema.updates, SqlMeasurementStoreSchema.names)(
        (u, n) => where((u.pointId === n.id) and (n.name === meas_name) and (u.measTime gte begin.?) and (u.measTime lte end.?))
          select (u)
          // we sort by id to keep insertion order
          orderBy (timeOrder(u.measTime, ascending), timeOrder(u.id, ascending))).page(0, max).toList
    meases
  }

  import org.squeryl.dsl.ast.{ OrderByArg, ExpressionNode }
  private def timeOrder(time: ExpressionNode, ascending: Boolean) = {
    if (ascending)
      new OrderByArg(time).asc
    else
      new OrderByArg(time).desc
  }
}
object SqlMeasurementStoreOperations extends SqlMeasurementStoreOperations