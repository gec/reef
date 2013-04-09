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
package org.totalgrid.reef.protocol.dnp3.common

import org.osgi.framework.{ ServiceRegistration, BundleContext }
import org.totalgrid.reef.protocol.api.{ AddRemoveValidation, Protocol }

import org.totalgrid.reef.osgi.Helpers._
import org.totalgrid.reef.protocol.dnp3.master.Dnp3MasterProtocol
import org.totalgrid.reef.protocol.dnp3.slave.Dnp3SlaveProtocol
import org.totalgrid.reef.osgi.ExecutorBundleActivator
import net.agileautomata.executor4s.Executor

class Dnp3ProtocolActivator extends ExecutorBundleActivator {

  // to be used in the dynamic OSGi world, the library can't be loaded by the static class loader
  System.loadLibrary("dnp3java")
  System.setProperty("reef.api.protocol.dnp3.nostaticload", "")
  private val masterProtocol = new Dnp3MasterProtocol with AddRemoveValidation
  private val slaveProtocol = new Dnp3SlaveProtocol with AddRemoveValidation

  private var registrations = List.empty[ServiceRegistration]

  override def start(context: BundleContext, exe: Executor) {
    registrations ::= context.createService(masterProtocol, Map("protocol" -> masterProtocol.name), classOf[Protocol])
    registrations ::= context.createService(slaveProtocol, Map("protocol" -> slaveProtocol.name), classOf[Protocol])
  }

  override def stop(context: BundleContext, executor: Executor) {
    registrations.foreach(_.unregister())
    masterProtocol.Shutdown()
    slaveProtocol.Shutdown()
  }

}