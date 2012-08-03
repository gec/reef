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
package org.totalgrid.reef.jmx

import org.totalgrid.reef.util.Timing
import scala.collection.mutable
import management.ManagementFactory

trait Timer {
  def apply[A](f: => A): A
}

trait MetricsContainer {
  def add(name: String, v: MetricValue)
  def get(name: String): MetricValue
  def getAll: List[(String, MetricValue)]
}

object MetricsContainer {

  def apply(): MetricsContainer = new DefaultMetricsContainer

  class DefaultMetricsContainer extends MetricsContainer {
    private val map = mutable.Map.empty[String, MetricValue]

    def add(name: String, v: MetricValue) {
      map += (name -> v)
    }

    def get(name: String) = {
      map.get(name) getOrElse { throw new Exception("Failed to get metrics from metrics container " + name) }
    }

    def getAll: List[(String, MetricValue)] = map.toList
  }
}

trait Metrics {
  def gauge(name: String): (Int) => Unit
  def counter(name: String): (Int) => Unit
  def average(name: String): (Int) => Unit
  def timer(name: String): Timer

  //def subMetrics(subId: String): Metrics
}

object Metrics {

  def apply(container: MetricsContainer): Metrics = new DefaultMetrics(container)

  class DefaultTimer(metric: MetricValue.AverageMetric) extends Timer {
    def apply[A](f: => A) = {
      Timing.time(x => metric.update(x.toInt))(f)
    }
  }

  class DefaultMetrics(container: MetricsContainer) extends Metrics {

    private def register(name: String, v: MetricValue) = {
      container.add(name, v)
      container.get(name).update(_)
    }

    def gauge(name: String) = {
      register(name, new MetricValue.GaugeMetric)
    }

    def counter(name: String) = {
      register(name, new MetricValue.CounterMetric)
    }

    def average(name: String) = {
      register(name, new MetricValue.AverageMetric(30))
    }
    def average(name: String, size: Int) = {
      register(name, new MetricValue.AverageMetric(size))
    }

    def timer(name: String) = {
      val metric = new MetricValue.AverageMetric(30)
      container.add(name, metric)
      new DefaultTimer(metric)
    }
  }
}

trait MetricsManager {
  def metrics(name: String): Metrics
  def register()
}

case class Tag(name: String, value: String)

object MetricsManager {

  val instanceTag = "instance"

  def apply(domain: String): MetricsManager = new DefaultMetricsManager(domain, Nil)
  def apply(domain: String, instance: String): MetricsManager = new DefaultMetricsManager(domain, List(Tag(instanceTag, instance)))

  //def apply(domain: String, tags: List[Tag]): MetricsManager = new DefaultMetricsManager(domain, tags)

  case class MetricsInfo(domain: String, tags: List[Tag], name: String, container: MetricsContainer)

  class DefaultMetricsManager(domain: String, tags: List[Tag]) extends MetricsManager {

    private var stash = List.empty[MetricsInfo]

    def metrics(name: String): Metrics = {
      val container = MetricsContainer()
      val info = MetricsInfo(domain, tags, name, container)
      stash ::= info
      Metrics(container)
    }

    // TODO: invariants on calling only once and not calling metrics again afterwards
    def register() {

      val server = ManagementFactory.getPlatformMBeanServer

      stash.foreach {
        case MetricsInfo(domain, tags, name, container) => {
          val objectName = MBeanUtils.objectName(domain, tags, name)

          // Exception will be thrown if we try to register twice,
          // since we may have modified what attributes the bean has we just replace it
          if (server.isRegistered(objectName)) {
            server.unregisterMBean(objectName)
          }

          server.registerMBean(new MetricsMBean(objectName, container), objectName)
        }
      }
    }
  }
}
