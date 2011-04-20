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

import org.totalgrid.reef.proto.Measurements

import MeasSink.Meas

/// A wide interface for reading/writing historical data
trait Historian {

  /* -----  abstract functions ------- */

  /**
   *   Returns the most recent values within a range up to [max]
   *     @param name The name of measurement
   *     @param begin Beginning of the time range (inclusive)
   *     @param end End of the time range (inclusive)
   *     @param max Maximum number of records to retrieve
   *     @param ascending
   */
  def getInRange(name: String, begin: Long, end: Long, max: Int, ascending: Boolean): Seq[Meas]

  /**
   *   Returns the number of records for a particular
   *     @param name The name of the measurement to be counted
   */
  def numValues(name: String): Int

  /**
   *   Removes all records for a set of measurements
   */
  def remove(names: Seq[String]): Unit

  /* -----  composed helper functions ------- */

  /**
   *   Returns the most recent values within a range up to [max]
   *     @param name The name of measurement
   *     @param begin Beginning of the time range (inclusive)
   *     @param end End of the time range (inclusive)
   *     @param max Maximum number of records to retrieve
   */
  def getNewestInRange(name: String, begin: Long, end: Long, max: Int): Seq[Meas] = {
    getInRange(name, begin, end, max, false)
  }

  /**
   *   Returns the least recent values within a range up to [max]
   *     @param name The name of measurement
   *     @param begin Beginning of the time range (inclusive)
   *     @param end End of the time range (inclusive)
   *     @param max Maximum number of records to retrieve
   */
  def getOldestInRange(name: String, begin: Long, end: Long, max: Int): Seq[Meas] = {
    getInRange(name, begin, end, max, true)
  }

  /**
   *   Returns the most recent values since a certain time
   *     @param name The name of measurement
   *     @param since Reference begin time (inclusive)
   *     @param max Maximum number of records to retrieve
   */
  def getNewestSince(name: String, since: Long, max: Int): Seq[Meas] = {
    getNewestInRange(name, since, Long.MaxValue, max)
  }

  /**
   *   Returns the least recent values since a certain time
   *     @param name The name of measurement
   *     @param since Reference begin time (inclusive)
   *     @param max Maximum number of records to retrieve
   */
  def getOldestSince(name: String, since: Long, max: Int): Seq[Meas] = {
    getOldestInRange(name, since, Long.MaxValue, max)
  }

  /**
   *   Returns the most recent values of a measurement
   *     @param name The name of measurement
   *     @param max Maximum number of records to retrieve
   */
  def getNewest(name: String, max: Int): Seq[Meas] = {
    getNewestSince(name, 0, max)
  }

  /**
   *   Returns the least recent values of a measurement
   *     @param name The name of measurement
   *     @param max Maximum number of records to retrieve
   */
  def getOldest(name: String, max: Int): Seq[Meas] = {
    getOldestSince(name, 0, max)
  }

  /**
   *   Returns the most recent value of a measurement
   *     @param name The name of measurement
   */
  def getNewest(name: String): Option[Meas] = {
    getNewest(name, 1) match {
      case Seq(x) => Some(x)
      case _ => None
    }
  }

  /**
   *   Returns the most oldest value of a measurement
   *     @param name The name of measurement
   */
  def getOldest(name: String): Option[Meas] = {
    getOldest(name, 1) match {
      case Seq(x) => Some(x)
      case _ => None
    }
  }

}
