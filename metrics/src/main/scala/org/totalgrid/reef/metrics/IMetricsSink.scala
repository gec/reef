/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  Green Energy Corp licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.metrics

trait IMetricsSink {
  def getStore(name: String): MetricsHookSource
}

/**
 *
 */
class SimpleMetricsSink extends IMetricsSink {

  var stores = Map.empty[String, CurrentMetricsValueHolder]

  def getStore(name: String): MetricsHookSource = {
    stores.get(name) match {
      case Some(cv) => cv
      case None =>
        val cmvh = new CurrentMetricsValueHolder(name)
        stores = stores + (name -> cmvh)
        cmvh
    }
  }
  def publishAll(pub: NonOperationalDataSink) {
    stores.foreach { case (name, holder) => holder.publishAll(pub) }
  }

  def resetAll() {
    stores.foreach { case (name, holder) => holder.resetAll() }
  }
}

object MetricsSink {

  var sinks = Map.empty[String, SimpleMetricsSink]

  def getInstance(instanceName: String): IMetricsSink = {
    sinks.get(instanceName) match {
      case Some(cv) => cv
      case None =>
        val cmvh = new SimpleMetricsSink
        sinks = sinks + (instanceName -> cmvh)
        cmvh
    }
  }

  def dumpToFile(fileName: String) {
    val pub = new CSVMetricPublisher(fileName)
    sinks.foreach { case (name, holder) => holder.publishAll(pub) }
    pub.close
  }

  def resetAll() {
    sinks.foreach { case (name, holder) => holder.resetAll() }
  }

}
