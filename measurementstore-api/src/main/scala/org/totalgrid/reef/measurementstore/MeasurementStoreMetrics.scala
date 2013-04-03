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
import org.totalgrid.reef.jmx.Metrics

class MeasSinkMetrics(sink: MeasSink, metrics: Metrics) extends MeasSink {

  val sets = metrics.counter("setOps")
  val setTime = metrics.timer("setTime")

  def set(meas: Seq[Meas]): Unit = {
    sets(1)
    setTime(sink.set(meas))
  }
}

class RTDatabaseMetrics(db: RTDatabase, metrics: Metrics) extends RTDatabase {

  val gets = metrics.counter("getOps")
  val keys = metrics.counter("keysRequested")
  val getTime = metrics.timer("getTime")

  def get(names: Seq[String]): Map[String, Meas] = {
    gets(1)
    keys(names.size)
    getTime(db.get(names))
  }
}

class HistorianMetrics(db: Historian, metrics: Metrics) extends Historian {

  val gets = metrics.counter("getOps")
  val entriesRetrieved = metrics.counter("entriesRetrieved")
  val getTime = metrics.timer("getTime")

  val counts = metrics.counter("countOps")
  val countTime = metrics.timer("countTime")
  val removes = metrics.counter("removeOps")
  val removeTime = metrics.timer("removeTime")

  def getInRange(name: String, begin: Long, end: Long, max: Int, ascending: Boolean): Seq[Meas] = {
    gets(1)
    val result = getTime(db.getInRange(name, begin, end, max, ascending))
    entriesRetrieved(result.size)
    result
  }

  def numValues(name: String): Int = {
    counts(1)
    countTime(db.numValues(name))
  }

  def remove(names: Seq[String]) {
    removes(1)
    removeTime(db.remove(names))
  }
}
