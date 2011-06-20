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
package org.totalgrid.reef.protocol.benchmark

import org.osgi.framework.{ ServiceRegistration, BundleActivator, BundleContext }
import org.totalgrid.reef.executor.ReactActorExecutor

import com.weiglewilczek.scalamodules._
import org.totalgrid.reef.protocol.api.{ ChannelAlwaysOnline, EndpointAlwaysOnline, Protocol }

class Activator extends BundleActivator {

  val exe = new ReactActorExecutor {}
  var reg: Option[ServiceRegistration] = None

  final override def start(context: BundleContext) {
    val protocol = new BenchmarkProtocol(exe) with EndpointAlwaysOnline with ChannelAlwaysOnline
    val registration = context.createService(protocol, "protocol" -> protocol.name, interface[Protocol])
    reg = Some(registration)
    exe.start()
  }

  final override def stop(context: BundleContext) = exe.stop()

}