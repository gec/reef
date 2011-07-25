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
package org.totalgrid.reef.protocol.dnp3

import org.osgi.framework.{ ServiceRegistration, BundleActivator, BundleContext }
import org.totalgrid.reef.protocol.api.{ AddRemoveValidation, Protocol }

import com.weiglewilczek.scalamodules._

class Activator extends BundleActivator {
  var reg: Option[ServiceRegistration] = None

  override def start(context: BundleContext) {
    // to be used in the dynamic OSGi world, the library can't be loaded by the static class loader
    System.loadLibrary("dnp3java")
    System.setProperty("reef.protocol.dnp3.nostaticload", "")
    val protocol = new Dnp3Protocol with AddRemoveValidation
    reg = Some(context.createService(protocol, "protocol" -> protocol.name, interface[Protocol]))
  }

  override def stop(context: BundleContext) {
    reg.foreach(_.unregister)
  }

}