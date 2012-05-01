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

import org.totalgrid.reef.client.service.proto.Measurements.{ Measurement => Meas }
import org.squeryl.PrimitiveTypeMode._
import scala.collection.mutable

/**
 * operations on the SqlMeasurementStoreSchema that implement the MeasurementStore interface. All operations
 * assume they are being run from inside a database transaction.
 */
trait SqlMeasurementStoreOperations {

  case class MeasId(var pointId: Long, meas: Meas)

  private def makeUpdate(m: Meas, pNameMap: mutable.Map[String, MeasId]): Measurement = {
    new Measurement(pNameMap.get(m.getName).get.pointId, m.getTime, m.toByteString.toByteArray)
  }

  private def makeCurrentValue(measId: MeasId): CurrentValue = {
    new CurrentValue(measId.pointId, measId.meas.toByteString.toByteArray)
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

  def set(meas: Seq[Meas], includeHistory: Boolean) {
    // setup list of all the points we are trying to find ids for
    val measToInsert = mutable.Map.empty[String, MeasId]
    meas.foreach { m => measToInsert.put(m.getName, MeasId(-1, m)) }

    // ask db for points it has, update the map with those ids
    val pNames = SqlMeasurementStoreSchema.names.where(n => n.name in measToInsert.keys).toList
    pNames.foreach { p => measToInsert.get(p.name).get.pointId = p.id }

    val (newMeas, updates) = measToInsert.partition(e => e._2.pointId == -1)

    if (newMeas.nonEmpty) {
      // if we have new measNames to add do so, then read them back out to get the ids
      SqlMeasurementStoreSchema.names.insert(newMeas.keys.map { new MeasName(_) })
      val addedNames = SqlMeasurementStoreSchema.names.where(n => n.name in newMeas.keys).toList
      addedNames.foreach { p => measToInsert.get(p.name).get.pointId = p.id }

      val addedCurrentValues = newMeas.map { case (name, measId) => makeCurrentValue(measId) }.toList
      SqlMeasurementStoreSchema.currentValues.insert(addedCurrentValues)
    }

    if (includeHistory) {
      // create the list of measurements to upload
      val toInsert = meas.map { makeUpdate(_, measToInsert) }.toList
      SqlMeasurementStoreSchema.updates.insert(toInsert)
    }

    // something odd occurs with parellel writes and we get batch update exceptions
    // when inserting and updating in the same transaction so we manually update and insert
    // if inserting any new values. Should go away when we implement "add" for measusment store and
    // eliminate the "lazy" adding we are doing now.
    // TODO: refactor MeasurementStore to not allow "lazy adding of measurements"
    val toUpdate = updates.map { case (name, measId) => makeCurrentValue(measId) }.toList
    SqlMeasurementStoreSchema.currentValues.forceUpdate(toUpdate)
  }

  def get(names: Seq[String]): Map[String, Meas] = {
    from(SqlMeasurementStoreSchema.names, SqlMeasurementStoreSchema.currentValues)((name, cv) =>
      where((name.name in names) and (cv.pointId === name.id))
        select (name.name, cv.proto)).toList.map { t => (t._1, Meas.parseFrom(t._2)) }.toMap
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