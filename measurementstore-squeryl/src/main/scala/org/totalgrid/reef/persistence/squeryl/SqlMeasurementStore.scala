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
package org.totalgrid.reef.persistence.squeryl

import org.totalgrid.reef.proto.Measurements.{ Measurement => Meas }

import org.totalgrid.reef.measurementstore.MeasurementStore

import org.squeryl.PrimitiveTypeMode._
import org.squeryl._

case class Measurement(
    val pointId: Long,
    val measTime: Long,
    val proto: Array[Byte]) extends KeyedEntity[Long] {
  var id: Long = 0
}

case class MeasName(
    val name: String) extends KeyedEntity[Long] {
  var id: Long = 0
}

object SqlMeasurementStoreSchema extends Schema {
  val updates = table[Measurement]
  val names = table[MeasName]

  on(updates)(s => declare(
    columns(s.pointId, s.measTime.~) are (indexed),
    columns(s.pointId, s.measTime.~, s.id) are (indexed)))

  on(names)(s => declare(
    columns(s.name) are (indexed, unique)))

  def reset() = {
    drop // its protected for some reason
    create
  }

}

import org.totalgrid.reef.persistence.ConnectionOperations
class SqlMeasurementStore(connection: ConnectionOperations[Boolean]) extends MeasurementStore {

  def makeUpdate(m: Meas, pNameMap: Map[String, Long]): Measurement = {
    new Measurement(pNameMap.get(m.getName).get, m.getTime, m.toByteString.toByteArray)
  }

  override def reset(): Boolean = {
    connection.doSync[Boolean] { r =>
      transaction {
        SqlMeasurementStoreSchema.reset
      }
      Some(true)
    }.getOrElse(throw new Exception("Couldn't reset database"))
  }

  override def points(): List[String] = {
    connection.doSync[List[String]] { r =>
      Some(transaction {
        SqlMeasurementStoreSchema.names.where(t => true === true).toList.map { _.name }
      })
    }.getOrElse(throw new Exception("Couldn't get list of points"))
  }

  def set(meas: Seq[Meas]) {
    if (!meas.nonEmpty) return
    connection.doAsync { r =>
      transaction {
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
        }

        // create the list of measurements to upload
        val toInsert = meas.map { makeUpdate(_, insertedMeas) }.toList
        SqlMeasurementStoreSchema.updates.insert(toInsert)
      }
    }
  }

  def get(names: Seq[String]): Map[String, Meas] = {
    var m = Map.empty[String, Meas]
    if (names.size == 0) return m

    connection.doSync[Map[String, Meas]] { r =>
      transaction {
        var insertedMeas = Map.empty[Long, String]
        val pNames = SqlMeasurementStoreSchema.names.where(n => n.name in names).toList
        pNames.foreach { p => insertedMeas = insertedMeas - p.id + (p.id -> p.name) }

        /*insertedMeas.foreach { case (pointId, name) =>
          val proto = from(SqlMeasurementStoreSchema.updates)(u =>
            where(u.pointId === pointId) select(u.proto)
              orderBy (timeOrder(u.measTime, false), timeOrder(u.id, false))).page(0, 1).single
          m = m + (name -> Meas.parseFrom(proto))
        }*/

        // http://groups.google.com/group/squeryl/browse_frm/thread/b58f3f8c23f76eb

        val ids = insertedMeas.keys.toList

        def maxMeasTime =
          from(SqlMeasurementStoreSchema.updates)(m =>
            where((m.pointId in ids))
              groupBy (m.pointId)
              compute (max(m.measTime)))
        def byId = join(SqlMeasurementStoreSchema.updates, maxMeasTime)((m, mmt) =>
          select(m.pointId, m.measTime, m.proto)
            on ((m.pointId === mmt.key) and (m.measTime === mmt.measures)))

        byId.foreach {
          case (pid, time, proto) =>
            m = m + (insertedMeas.get(pid).get -> Meas.parseFrom(proto))
        }
      }

      Some(m)
    }.getOrElse(throw new Exception("Error getting current value for measurements"))
  }

  def numValues(meas_name: String): Int = {
    connection.doSync[Int] { r =>
      transaction {
        val m = from(SqlMeasurementStoreSchema.updates, SqlMeasurementStoreSchema.names)(
          (u, n) => where(u.pointId === n.id and n.name === meas_name) compute (count(u.id)))
        val q = m.head.measures

        Some(q.toInt)
      }

    }.get
  }

  def remove(names: Seq[String]): Unit = {
    connection.doAsync { r =>
      transaction {

        val nameRows = SqlMeasurementStoreSchema.names.where(u => u.name in names).toList.map { _.id }
        if (nameRows.nonEmpty) {
          SqlMeasurementStoreSchema.updates.deleteWhere(u => u.pointId in nameRows)
          SqlMeasurementStoreSchema.names.deleteWhere(n => n.id in nameRows)
        }
      }
    }
  }

  def getInRange(meas_name: String, begin: Long, end: Long, max: Int, ascending: Boolean): Seq[Meas] = {
    connection.doSync[List[Meas]] { r =>
      // make start/end arguments optional
      val beginO = if (begin == 0) None else Some(begin)
      val endO = if (end == Long.MaxValue) None else Some(end)

      val meases = transaction {
        from(SqlMeasurementStoreSchema.updates, SqlMeasurementStoreSchema.names)(
          (u, n) => where((u.pointId === n.id) and (n.name === meas_name) and (u.measTime gte begin.?) and (u.measTime lte end.?))
            select (u)
            orderBy (timeOrder(u.measTime, ascending), timeOrder(u.id, ascending)) // we sort by id to keep insertion order
).page(0, max).toList //
      }
      val list = meases.map(m => Meas.parseFrom(m.proto))
      Some(list)
    }.get
  }

  import org.squeryl.dsl.ast.{ OrderByArg, ExpressionNode }
  def timeOrder(time: ExpressionNode, ascending: Boolean) = {
    if (ascending)
      new OrderByArg(time).asc
    else
      new OrderByArg(time).desc
  }
}
