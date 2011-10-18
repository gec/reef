package org.totalgrid.reef.measurementstore.squeryl

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
import org.totalgrid.reef.api.proto.Measurements.{ Measurement => Meas }

import org.totalgrid.reef.measurementstore.MeasurementStore

import org.squeryl.PrimitiveTypeMode._

import org.totalgrid.reef.persistence.ConnectionOperations

/**
 * implementation of measurement store that uses SqlMeasurementStoreOperations functions,
 * handles sync/async, opening/closing database transaction and error message generation
 */
class SqlMeasurementStore(connection: ConnectionOperations[Boolean]) extends MeasurementStore {

  override val supportsTrim = true

  override def reset(): Boolean = {
    connection.doSync[Boolean] { r =>
      Some(transaction {
        SqlMeasurementStoreOperations.reset
      })
    }.getOrElse(throw new Exception("Couldn't reset database"))
  }

  override def trim(numPoints: Long): Long = {
    connection.doSync[Long] { r =>
      Some(transaction {
        SqlMeasurementStoreOperations.trim(numPoints)
      })
    }.getOrElse(throw new Exception("Couldn't trim database"))
  }

  override def points(): List[String] = {
    connection.doSync[List[String]] { r =>
      Some(transaction {
        SqlMeasurementStoreOperations.points
      })
    }.getOrElse(throw new Exception("Couldn't get list of points"))
  }

  def set(meas: Seq[Meas]) {
    if (!meas.nonEmpty) return
    connection.doSync { r =>
      Some(transaction {
        SqlMeasurementStoreOperations.set(meas)
      })
    }.getOrElse(throw new Exception("Couldn't store measurements in measurement store."))
  }

  def get(names: Seq[String]): Map[String, Meas] = {
    if (names.size == 0) return Map.empty[String, Meas]

    connection.doSync[Map[String, Meas]] { r =>
      Some(transaction {
        SqlMeasurementStoreOperations.get(names)
      })
    }.getOrElse(throw new Exception("Error getting current value for measurements"))
  }

  def numValues(meas_name: String): Int = {
    connection.doSync[Int] { r =>
      Some(transaction {
        SqlMeasurementStoreOperations.numValues(meas_name)
      })
    }.get
  }

  def remove(names: Seq[String]): Unit = {
    connection.doSync[Unit] { r =>
      Some(transaction {
        SqlMeasurementStoreOperations.remove(names)
      })
    }.getOrElse(throw new Exception("Couldn't remove points: " + names))
  }

  def getInRange(meas_name: String, begin: Long, end: Long, max: Int, ascending: Boolean): Seq[Meas] = {
    connection.doSync[Seq[Meas]] { r =>
      Some(transaction {
        SqlMeasurementStoreOperations.getInRange(meas_name, begin, end, max, ascending)
      })
    }.get
  }
}
