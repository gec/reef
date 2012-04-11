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

import com.weiglewilczek.slf4s.Logging
import org.osgi.framework.BundleContext
import com.weiglewilczek.scalamodules._
import org.totalgrid.reef.osgi.OsgiConfigReader
import org.totalgrid.reef.client.settings.util.PropertyLoading
import net.agileautomata.executor4s._

object MeasurementStoreFinder extends Logging {

  /**
   * Gets the measurement store implementation configured in the org.totalgrid.reef.mstore file.
   *
   * @return measurement store
   */
  def getInstance(context: BundleContext): MeasurementStore = {

    val config = OsgiConfigReader.load(context, "org.totalgrid.reef.mstore")
    val historian = PropertyLoading.getString("org.totalgrid.reef.mstore.historianImpl", config)
    val currentValue = PropertyLoading.getString("org.totalgrid.reef.mstore.currentValueImpl", config)

    def fail() {
      throw new IllegalArgumentException("Measurement store implementation not found, make sure packages are active and installed. " +
        " historian: " + historian + " realtime: " + currentValue)
    }

    // if the two impls are the same get a single measurementstore that does both
    if (historian == currentValue) {
      val store = getImplementation(context, historian, Some(true), Some(true))

      if (store.isEmpty) fail()

      store.get
    } else {

      // otherwise load both implementations and return the mixed store
      // first try to find implementation that doesn't do both, then one that does both if it can't find it
      val historianStore = getImplementation(context, historian, Some(true), Some(false)).orElse(
        getImplementation(context, historian, Some(true), None))
      val currentStore = getImplementation(context, currentValue, Some(false), Some(true)).orElse(
        getImplementation(context, currentValue, None, Some(true)))

      if (historianStore.isEmpty || currentStore.isEmpty) fail()

      // don't actually make the executor unless connect is called
      val executorSource = { Executors.newResizingThreadPool(5.minutes) }

      new MixedMeasurementStore(executorSource, historianStore.get, currentStore.get)
    }
  }

  /**
   * get a particular implementation of MeasurementStore with the specific historian and realtime values
   */
  private def getImplementation(context: BundleContext, implementation: String, historian: Option[Boolean], realtime: Option[Boolean]): Option[MeasurementStore] = {

    val serviceOptions = context findServices withInterface[MeasurementStoreProvider] withFilter
      "impl" === implementation andApply { (service, properties) =>
        // filtering doesn't work as expected, seems to do an "or" rather than an "and"
        if (properties.get("impl").get != implementation) None
        else if (historian.isDefined && properties.get("historian").get != historian.get) None
        else if (realtime.isDefined && properties.get("realtime").get != realtime.get) None
        else Some(service)
      }
    serviceOptions.flatten.headOption.map { _.createStore }
  }
}