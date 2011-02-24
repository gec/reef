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

import org.totalgrid.reef.metrics.{ MetricsHookSource, StaticMetricsHooksBase }

class MeasSinkMetrics(sink: MeasSink, source: MetricsHookSource) extends StaticMetricsHooksBase(source) with MeasSink {

  val sets = counterHook("setOps")
  val setTime = timingHook[Unit]("setTime")

  def set(meas: Seq[Meas]): Unit = {
    sets(1)
    setTime { sink.set(meas) }
  }
}

class RTDatabaseMetrics(db: RTDatabase, source: MetricsHookSource) extends StaticMetricsHooksBase(source) with RTDatabase {

  val gets = counterHook("getOps")
  val keys = counterHook("keysRequested")
  val getTime = timingHook[Map[String, Meas]]("getTime")

  def get(names: Seq[String]): Map[String, Meas] = {
    gets(1)
    keys(names.size)
    getTime {
      db.get(names)
    }
  }
}

class HistorianMetrics(db: Historian, source: MetricsHookSource) extends StaticMetricsHooksBase(source) with Historian {

  val gets = counterHook("getOps")
  val entriesRetrieved = counterHook("entriesRetrieved")
  val getTime = timingHook[Seq[Meas]]("getTime")

  val counts = counterHook("countOps")
  val countTime = timingHook[Int]("countTime")
  val removes = counterHook("removeOps")
  val removeTime = timingHook[Unit]("removeTime")

  def getInRange(name: String, begin: Long, end: Long, max: Int, ascending: Boolean): Seq[Meas] = {
    gets(1)
    val result = getTime { db.getInRange(name, begin, end, max, ascending) }
    entriesRetrieved(result.size)
    result
  }

  def numValues(name: String): Int = {
    counts(1)
    countTime { db.numValues(name) }
  }

  def remove(names: Seq[String]): Unit = {
    removes(1)
    removeTime { db.remove(names) }
  }
}
