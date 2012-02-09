/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.measurementstore

import org.totalgrid.reef.client.service.proto.Measurements.{ Measurement => Meas }

import scala.collection.mutable.ListBuffer
import scala.collection.immutable.TreeMap
import scala.collection.mutable

class MeasStorage(var startingValue: Meas, currentValueOnly: Boolean) {
  var historicValues = TreeMap.empty[Long, ListBuffer[Meas]]
  var lastWrittenValue = startingValue

  addMeas(startingValue)

  def addMeas(meas: Meas) {
    if (currentValueOnly) historicValues = TreeMap.empty[Long, ListBuffer[Meas]]
    historicValues.get(meas.getTime) match {
      case Some(l) => l += meas
      case None => historicValues += (meas.getTime -> ListBuffer(meas))
    }
    lastWrittenValue = meas
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

class InMemoryMeasurementStore(currentValueOnly: Boolean = false) extends MeasurementStore {

  var values = mutable.Map.empty[String, MeasStorage]

  def get(names: Seq[String]): Map[String, Meas] = this.synchronized {
    values.filterKeys(names.contains).map { x => x._1 -> x._2.lastWrittenValue }.toMap
  }
  def set(meas: Seq[Meas]): Unit = {
    meas.foreach { m =>
      val storage = this.synchronized {
        values.get(m.getName) match {
          case Some(hist) => Some(hist)
          case None =>
            values.put(m.getName, new MeasStorage(m, currentValueOnly))
            None
        }
      }
      storage.foreach { _.addMeas(m) }
    }
  }

  def getInRange(name: String, begin: Long, end: Long, max: Int, ascending: Boolean): Seq[Meas] = this.synchronized {
    checkHistorian
    values.get(name).map { _.getInRange(begin, end, max, ascending) }.getOrElse(Nil)
  }

  def numValues(name: String): Int = this.synchronized {
    checkHistorian
    values.get(name).map { _.numValues }.getOrElse(0)
  }

  def remove(names: Seq[String]): Unit = this.synchronized {
    names.foreach { name =>
      values.remove(name)
    }
  }

  def numPoints(): Int = this.synchronized {
    values.size
  }

  def allCurrent(): Seq[Meas] = this.synchronized {
    values.map { x => x._2.lastWrittenValue }.toList
  }

  def connect() = {}
  def disconnect() = {}

  private def checkHistorian = if (currentValueOnly) throw new Exception("Using currentValue store as historian!")
}