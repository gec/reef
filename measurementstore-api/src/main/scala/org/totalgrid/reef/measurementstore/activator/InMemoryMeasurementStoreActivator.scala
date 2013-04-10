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
package org.totalgrid.reef.measurementstore.activator

import org.osgi.framework.{ BundleContext, BundleActivator }
import org.totalgrid.reef.osgi.Helpers._
import org.totalgrid.reef.measurementstore.{ MeasurementStoreProvider, InMemoryMeasurementStore }

class InMemoryMeasurementStoreActivator extends BundleActivator {
  def start(context: BundleContext) {

    val historianMeasurementStore = new InMemoryMeasurementStore(false)
    val realtimeMeasurementStore = new InMemoryMeasurementStore(true)

    val commonOptions = Map[String, Any]("impl" -> "memory", "realtime" -> true)
    val historianOptions = commonOptions + ("historian" -> true)
    val realtimeOptions = commonOptions + ("historian" -> false)

    context.createService(MeasurementStoreProvider(historianMeasurementStore), historianOptions, classOf[MeasurementStoreProvider])
    context.createService(MeasurementStoreProvider(realtimeMeasurementStore), realtimeOptions, classOf[MeasurementStoreProvider])

  }

  def stop(context: BundleContext) {}
}