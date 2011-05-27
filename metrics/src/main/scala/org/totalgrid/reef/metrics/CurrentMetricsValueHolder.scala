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
package org.totalgrid.reef.metrics

import java.util.concurrent.atomic.AtomicInteger
import org.totalgrid.reef.util.{ Logging }

trait MetricValueHolder {
  def update(i: Int)

  def reset()

  def value: AnyVal
}

abstract class IntMetricHolder extends MetricValueHolder with Logging {

  protected val current = new AtomicInteger(0)

  def update(i: Int)

  def value: Int = { current.get }
  def reset = current.set(0)

}

class CounterMetric extends IntMetricHolder {
  def update(i: Int) = {
    current.addAndGet(i)
  }
}

class ValueMetric extends IntMetricHolder {
  def update(i: Int) = {
    current.set(i)
  }
}

/**
 * keeps a windowed average over the last size updates
 */
class AverageMetric(size: Int) extends MetricValueHolder with Logging {

  private var next = 0
  private val counts = new Array[Int](size)
  private var sum = 0

  def update(i: Int) = {
    synchronized {
      sum -= counts.apply(next % size)
      counts.update(next % size, i)
      next += 1
      sum += i
    }
  }

  def reset = {
    synchronized {
      next = 0
      sum = 0
      Range(0, size).foreach(counts.update(_, 0))
    }
  }

  def value: Double = {
    var s = 0
    synchronized {
      s = sum
    }
    // can look this up unsafely since after startup we dont actually
    // need this value to be accurate
    val num = if (next > size) size else next
    val d = if (num == 0) 0 else s.toDouble / num
    d
  }
}

class CurrentMetricsValueHolder(val baseName: String) extends MetricsHookSource with MetricsHolder {
  var metrics = Map.empty[String, MetricValueHolder]

  def getSinkFunction(hookName: String, typ: MetricsHooks.HookType): (Int) => Unit = {
    val name = baseName + "." + hookName
    val metricHolder = {
      typ match {
        case MetricsHooks.Counter => new CounterMetric
        case MetricsHooks.Value => new ValueMetric
        case MetricsHooks.Average => new AverageMetric(30)
      }
    }
    metrics += name -> metricHolder
    metricHolder.update
  }

  def values(keys: List[String]): Map[String, Any] = {

    val matchingKeys = metrics.keys.filter(MetricsMapHelpers.matchesAny(_, keys))

    matchingKeys.map { name => name -> metrics(name).value }.toMap
  }

  def reset(keys: List[String]) {
    val matchingKeys = metrics.keys.filter(MetricsMapHelpers.matchesAny(_, keys))
    matchingKeys.foreach { name => metrics(name).reset }
  }
}

