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

import org.totalgrid.reef.client.service.proto.Measurements

import scala.util.Random
import org.totalgrid.reef.util.Timing

import org.scalatest.{ FunSuite, BeforeAndAfterAll }
import org.scalatest.{ Reporter, Stopper, Filter, Distributor, Tracker }
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class InMemoryRTDatabaseReadPerformanceTest extends RTDatabaseReadPerformanceTestBase {
  lazy val cm = new InMemoryMeasurementStore()
  val fname = "inmem.plt"
}

abstract class RTDatabaseReadPerformanceTestBase extends FunSuite with ShouldMatchers with BeforeAndAfterAll {

  def cm: MeasurementStore
  def fname: String

  def getMeas(name: String, time: Int) = {
    val meas = Measurements.Measurement.newBuilder
    meas.setName(name).setType(Measurements.Measurement.Type.INT).setIntVal(Long.MaxValue)
    meas.setQuality(Measurements.Quality.newBuilder.build)
    meas.setTime(time)
    meas.build
  }

  def getName(baseName: String, i: Int): String = {
    String.format("%s%03d", baseName, int2Integer(i))
  }

  val setNum = 100
  val setiters = 1
  val getIters = 25
  val getPoints = 100
  val getNum = 1000
  val histiters = 1
  val histpoints = 10
  val updates = 1000
  val rand = new Random()
  val batchSize = 100

  var sequentialReadTimings = List.empty[Tuple2[Int, Long]]
  var sequentialWriteTimings = List.empty[Tuple2[Int, Long]]
  var removeTimings = List.empty[Tuple2[Int, Long]]
  var historyReads = List.empty[Tuple2[Int, Long]]

  override def afterAll() {
    if (sequentialReadTimings.size == 0) return
    val outFile = new java.io.FileOutputStream(fname)
    val outStream = new java.io.PrintStream(outFile)
    sequentialReadTimings.foreach { case (s, t) => outStream.println(s + "\t" + t) }
    outStream.println("\n\n")
    sequentialWriteTimings.foreach { case (s, t) => outStream.println(s + "\t" + t) }
    outStream.println("\n\n")
    removeTimings.foreach { case (s, t) => outStream.println(s + "\t" + t) }
    outStream.println("\n\n")
    historyReads.foreach { case (s, t) => outStream.println(s + "\t" + t) }
    outStream.println("\n\n")
  }

  ignore("Performance") {
    val basename = "RTDBPerformance"

    def doSets(iters: Int, queryFun: Int => Seq[Measurements.Measurement]) = {
      var inserts = List.empty[Tuple2[Int, Long]]
      var removes = List.empty[Tuple2[Int, Long]]
      for (i <- 1 to iters) {
        val size = rand.nextInt(setNum - 1) + 1
        val query = queryFun(size)
        def addTime(t: Long) = inserts ::= (size, t)
        Timing.time(addTime _) { cm.set(query) }
        def remTime(t: Long) = removes ::= (size, t)
        Timing.time(remTime _) { cm.remove(query.map { _.getName }) }
      }
      (inserts, removes)
    }
    def sequentialInsert(num: Int): Seq[Measurements.Measurement] = {
      for (i <- 1 to num) yield getMeas(getName(basename, i), i)
    }
    val ret = doSets(setiters, sequentialInsert)
    sequentialWriteTimings = ret._1
    removeTimings = ret._2

    cm.remove(for (i <- 1 to getPoints) yield getName(basename, i))
    // make sure there is atleast 1 measurement for each point
    cm.set(for (i <- 1 to getPoints) yield getMeas(getName(basename, i), 0))
    for (i <- 1 to getNum / batchSize) {
      val meas = for (j <- 1 to batchSize) yield getMeas(getName(basename, rand.nextInt(getPoints - 1) + 1), i * batchSize + j)
      cm.set(meas)
    }

    def sequentialQuery(num: Int): Seq[String] = {
      for (i <- 1 to num) yield getName(basename, i)
    }

    def doGets(iters: Int, queryFun: Int => Seq[String]) = {
      var timings = List.empty[Tuple2[Int, Long]]
      for (i <- 1 to iters) {
        val size = rand.nextInt(getPoints - 1) + 1
        def addTime(t: Long) = timings ::= (size, t)
        val result = Timing.time(addTime _) { cm.get(queryFun(size)) }
        result.size should equal(size)
      }
      timings
    }
    sequentialReadTimings = doGets(getIters, sequentialQuery)

    val measNames = for (i <- 1 to histpoints) yield getName("HistorianPerformance", i)
    cm.remove(measNames)
    measNames.map(n => for (i <- 1 to updates / batchSize) {
      val meas = for (j <- 1 to batchSize) yield getMeas(n, (i - 1) * batchSize + j)
      cm.set(meas)
    })

    def doHistoryGets(iters: Int) = {
      var timings = List.empty[Tuple2[Int, Long]]
      for (i <- 1 to iters) {
        val name = getName("HistorianPerformance", rand.nextInt(histpoints - 1) + 1)
        val size = rand.nextInt(updates - 1) + 1
        val begin = rand.nextInt(updates - size)
        val end = begin + size
        val ascending = false
        def addTime(t: Long) = timings ::= (size, t)
        val result = Timing.time(addTime _) {
          cm.getInRange(name, begin, end, size, ascending)
        }
        result.size should equal(size)
        var prev: Long = Long.MaxValue
        result.forall { m => val ret = m.getTime <= prev; prev = m.getTime; ret } should equal(true)
      }
      timings
    }
    historyReads = doHistoryGets(histiters)
  }
}
