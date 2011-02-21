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
package org.totalgrid.reef.measurementstore

/**
 * Interface that defines everything a measurement store is responsible for
 */
trait MeasurementStore extends Historian with RTDatabase with MeasSink {

  /**
   * not all implementations can handle storing 2 measurements with the same timestamp,
   * if it doesn't the behavior is undefined on which measurement is kept
   */
  val supportsMultipleMeasurementsPerMillisecond = true

  /**
   * supports the trim operation
   */
  val supportsTrim = false

  /**
   *  tell the measurement store that it can (if it chooses) move measurements before end
   * to a slower secondary storage
   * @return whether archiving is supported
   */
  def archive(name: String, end: Long): Boolean = false

  /**
   * if available returns size of database, otherwise None
   */
  def dbSize(): Option[Long] = None

  /**
   * completely clears the measurementstore if possible
   * @returns whether the database was reset
   */
  def reset(): Boolean = false

  /**
   * some implementations can easily return the list of all points in the system, this is useful
   * for archiving and managing though shouldn't be relied on
   */
  def points(): List[String] = Nil

  /**
   * removes the least recent points so there is only numPoints of history left in the system
   */
  def trim(numPoints: Long): Long = { 0 }
}
