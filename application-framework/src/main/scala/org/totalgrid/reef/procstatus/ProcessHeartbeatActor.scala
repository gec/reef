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
package org.totalgrid.reef.procstatus

import org.totalgrid.reef.api.proto.ProcessStatus.StatusSnapshot

import org.totalgrid.reef.executor.{ Executor, Lifecycle }

import org.totalgrid.reef.api.proto.Application.HeartbeatConfig
import org.totalgrid.reef.japi.ReefServiceException
import org.totalgrid.reef.util.{ Timer, Logging }
import org.totalgrid.reef.sapi.request.ApplicationService

abstract class ProcessHeartbeatActor(services: ApplicationService, configuration: HeartbeatConfig)
    extends Executor with Lifecycle with Logging {

  private def makeProto(online: Boolean): StatusSnapshot = {
    StatusSnapshot.newBuilder
      .setProcessId(configuration.getProcessId)
      .setInstanceName(configuration.getInstanceName)
      .setTime(System.currentTimeMillis)
      .setOnline(online).build
  }

  private var repeater: Option[Timer] = None

  override def afterStart() = {
    repeater = Some(this.repeat(configuration.getPeriodMs)(heartbeat))
  }

  override def beforeStop() = {
    repeater.foreach(_.cancel)
    // beforeStop is called on the executor thread
    publish(makeProto(false))
  }

  private def heartbeat() = publish(makeProto(true))

  private def publish(ss: StatusSnapshot) {
    try {
      services.sendHeartbeat(ss).await()
    } catch {
      case rse: ReefServiceException =>
        logger.warn("Problem sending heartbeat: " + rse.getMessage)
    }
  }

}