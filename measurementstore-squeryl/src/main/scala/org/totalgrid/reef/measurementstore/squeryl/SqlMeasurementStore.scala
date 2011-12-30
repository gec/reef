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

import org.totalgrid.reef.measurementstore.MeasurementStore

import org.squeryl.PrimitiveTypeMode._
import org.totalgrid.reef.client.exception.InternalServiceException

/**
 * implementation of measurement store that uses SqlMeasurementStoreOperations functions,
 * handles sync/async, opening/closing database transaction and error message generation
 */
class SqlMeasurementStore(connectFunction: () => Unit, includeHistory: Boolean = true) extends MeasurementStore {

  override val supportsTrim = true

  override def connect() = connectFunction()

  override def reset(): Boolean = attempt("Couldn't reset database")(SqlMeasurementStoreOperations.reset)

  override def trim(numPoints: Long): Long =
    attempt("Couldn't trim database")(SqlMeasurementStoreOperations.trim(numPoints))

  override def points(): List[String] = attempt("Couldn't get list of points")(SqlMeasurementStoreOperations.points)

  def set(meas: Seq[Meas]) =
    if (meas.nonEmpty) attempt("Couldn't store measurements in measurement store") {
      SqlMeasurementStoreOperations.set(meas, includeHistory)
    }

  def get(names: Seq[String]): Map[String, Meas] = {
    if (names.size == 0) Map.empty[String, Meas]
    else attempt("Error getting current value for measurements")(SqlMeasurementStoreOperations.get(names))
  }

  def numValues(meas_name: String): Int =
    attempt("Error retrieving number of values")(SqlMeasurementStoreOperations.numValues(meas_name))

  def remove(names: Seq[String]): Unit =
    attempt("Couldn't remove points: " + names)(SqlMeasurementStoreOperations.remove(names))

  def getInRange(meas_name: String, begin: Long, end: Long, max: Int, ascending: Boolean): Seq[Meas] =
    attempt("Error retrieving history")(SqlMeasurementStoreOperations.getInRange(meas_name, begin, end, max, ascending))

  private def attempt[A](msg: String)(f: => A): A = {
    try { inTransaction(f) }
    catch { case ex: Exception => throw new InternalServiceException(msg, ex) }
  }

}
