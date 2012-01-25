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
package org.totalgrid.reef.measurementstore

import org.totalgrid.reef.client.service.proto.Measurements.Measurement

/**
 * Uses two MeasurementStore implementations to implement MeasurementStore:
 *
 * Calls connect, reset, remove and set on both.
 *
 * Calls get only on realtime store
 *
 * Calls rest on historian only
 */
class MixedMeasurementStore(historian: MeasurementStore, realtime: MeasurementStore) extends MeasurementStore {

  // calls both historian and realtime

  override val supportsOutOfOrderInsertion = historian.supportsOutOfOrderInsertion && realtime.supportsOutOfOrderInsertion

  // TODO: use executor to run operations on historian and realtime in parallel

  def connect() = {
    historian.connect()
    realtime.connect()
  }

  def disconnect() {
    realtime.disconnect()
    historian.disconnect()
  }

  override def reset() = {
    historian.reset()
    realtime.reset()
  }

  def remove(names: Seq[String]) {
    historian.remove(names)
    realtime.remove(names)
  }

  // both stores need to be given new values
  def set(meas: Seq[Measurement]) {
    historian.set(meas)
    realtime.set(meas)
  }

  // realtime only functionality

  def get(names: Seq[String]) = {
    realtime.get(names)
  }

  override def points() = {
    // technically we could ask either store for the list of points but in general
    // the realtime store should be better at "wide" queries like listing names
    realtime.points()
  }

  // historian only functionality

  override val supportsMultipleMeasurementsPerMillisecond = historian.supportsMultipleMeasurementsPerMillisecond
  override val supportsTrim = historian.supportsTrim

  override def trim(numPoints: Long) = {
    historian.trim(numPoints)
  }

  def getInRange(name: String, begin: Long, end: Long, max: Int, ascending: Boolean) = {
    historian.getInRange(name, begin, end, max, ascending)
  }

  def numValues(name: String) = historian.numValues(name)

  override def archive(name: String, end: Long) = historian.archive(name, end)

  override def dbSize() = historian.dbSize()
}