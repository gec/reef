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
package org.totalgrid.reef.metrics

import org.totalgrid.reef.util.{ MetricsHookSource, MetricsHooks, Logging }

import org.totalgrid.reef.proto.Measurements._

import java.util.concurrent.atomic.AtomicInteger

trait MetricValueHolder {
  def update(i: Int)

  def publish(name: String, pub: NonOperationalDataSink)
}

abstract class IntMetricHolder extends MetricValueHolder with Logging {

  def update(i: Int)

  val value = new AtomicInteger(0)
  def getValue(): Int = { value.get }

  def publish(name: String, pub: NonOperationalDataSink) {
    val c = value.get
    debug(name + " => " + c)
    pub.nonOp(name, c)
  }
}

class CounterMetric extends IntMetricHolder {
  def update(i: Int) = {
    value.addAndGet(i)
  }
}

class ValueMetric extends IntMetricHolder {
  def update(i: Int) = {
    value.set(i)
  }
}

/**
 * keeps a windowed average over the last size updates
 */
class AverageMetric(size: Int) extends MetricValueHolder with Logging {

  var next = 0
  val counts = new Array[Int](size)
  var sum = 0

  def update(i: Int) = {
    synchronized {
      sum -= counts.apply(next % size)
      counts.update(next % size, i)
      next += 1
      sum += i
    }
  }

  def publish(name: String, pub: NonOperationalDataSink) {

    var s = 0
    synchronized {
      s = sum
    }
    // can look this up unsafely since after startup we dont actually
    // need this value to be accurate
    val num = if (next > size) size else next
    val d = if (num == 0) 0 else s.toDouble / num
    debug(name + " => " + d)
    pub.nonOp(name, d)
  }
}

class CurrentMetricsValueHolder(val baseName: String) extends MetricsHookSource {
  var metrics = Map.empty[String, MetricValueHolder]

  def getSinkFunction(hookName: String, typ: MetricsHooks.HookType): (Int) => Unit = {
    val metricHolder = {
      typ match {
        case MetricsHooks.Counter => new CounterMetric
        case MetricsHooks.Value => new ValueMetric
        case MetricsHooks.Average => new AverageMetric(30)
      }
    }
    metrics += hookName -> metricHolder
    metricHolder.update
  }
}

/**
 * publish all of the current values in the CurrentMetricsValueHolders to
 * a bus channel in one MeasurementBatch
 */
class NonOpMetricPublisher(publish: MeasurementBatch => Unit) {

  private var stores: List[CurrentMetricsValueHolder] = Nil

  /**
   * Get a metric hook source with the right base name
   * @param baseName
   * @return hookSource
   */
  def getStore(baseName: String): MetricsHookSource = {
    val s = new CurrentMetricsValueHolder(baseName)
    stores ::= s
    s
  }

  /**
   * publish all of the metrics in one batch
   */
  def publishAll(): Unit = {
    val batch = MeasurementBatch.newBuilder
    val pub = new NonOperationalDataSink {
      def addMeasurement(meas: Measurement.Builder) {

        batch.addMeas(meas)
      }
    }
    stores.foreach { s =>
      s.metrics.foreach { m =>
        m._2.publish(s.baseName + "." + m._1, pub)
      }
    }
    batch.setWallTime(System.currentTimeMillis)
    publish(batch.build)
  }

}
