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
package org.totalgrid.reef.entry

import org.osgi.framework._

import org.totalgrid.reef.executor.{ LifecycleWrapper }
import org.totalgrid.reef.measproc.ProcessorEntryPoint
import org.totalgrid.reef.persistence.squeryl.SqlProperties
import org.totalgrid.reef.osgi.OsgiConfigReader
import org.totalgrid.reef.broker.BrokerProperties

class ProcessingActivator extends BundleActivator {

  var wrapper: Option[LifecycleWrapper] = None

  def start(context: BundleContext) {

    org.totalgrid.reef.executor.Executor.setupThreadPools

    val processor = ProcessorEntryPoint.makeContext(BrokerProperties.get(new OsgiConfigReader(context, "org.totalgrid.reef")), SqlProperties.get(new OsgiConfigReader(context, "org.totalgrid.reef")))

    wrapper = Some(new LifecycleWrapper(processor))
    wrapper.get.start
  }

  def stop(context: BundleContext) = wrapper.foreach { _.stop }

}
