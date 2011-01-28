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

import MeasSink.Meas

import scala.collection.mutable.ListBuffer
import scala.collection.immutable.TreeMap

class MeasStorage(var startingValue: Meas) {
  var historicValues = TreeMap.empty[Long, ListBuffer[Meas]]

  addMeas(startingValue)

  def addMeas(meas: Meas) {
    historicValues.get(meas.getTime) match {
      case Some(l) => l += meas
      case None => historicValues += (meas.getTime -> ListBuffer(meas))
    }

  }

  def currentValue: Meas = {
    historicValues.get(historicValues.lastKey).get.lastOption.get
  }

  def getInRange(begin: Long, end: Long, max: Int, ascending: Boolean): Seq[Meas] = {
    val rend = if (end == Long.MaxValue) end else end + 1
    val entries = historicValues.range(begin, rend).map { _._2 }.flatten
    val rentries = if (!ascending) entries.toList.reverse else entries.toList
    rentries.slice(0, max)
  }

  def numValues(): Int = {
    historicValues.foldLeft(0) { (sum, x) => sum + x._2.size }
  }
}

class InMemoryMeasurementStore extends MeasurementStore {

  var values = Map.empty[String, MeasStorage]

  def get(names: Seq[String]): Map[String, Meas] = {
    values.filterKeys(names.contains).map { x => x._1 -> x._2.currentValue }
  }
  def set(meas: Seq[Meas]): Unit = {
    meas.foreach(m => values.get(m.getName) match {
      case Some(hist) => hist.addMeas(m)
      case None => values = values + (m.getName -> new MeasStorage(m))
    })
  }

  def getInRange(name: String, begin: Long, end: Long, max: Int, ascending: Boolean): Seq[Meas] = {
    values.get(name).map { _.getInRange(begin, end, max, ascending) }.getOrElse(Nil)
  }

  def numValues(name: String): Int = {
    values.get(name).map { _.numValues }.getOrElse(0)
  }

  def remove(names: Seq[String]): Unit = {
    names.foreach { name =>
      values -= name
    }
  }

  def numPoints(): Int = {
    values.size
  }

  def allCurrent(): Seq[Meas] = {
    values.map { x => x._2.currentValue }.toList
  }
}