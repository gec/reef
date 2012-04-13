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
package org.totalgrid.reef.services.core

import org.totalgrid.reef.services.framework._

import org.totalgrid.reef.models._
import org.squeryl.PrimitiveTypeMode._
import com.weiglewilczek.slf4s.Logging
import net.agileautomata.executor4s._

class ProcessStatusCoordinator(model: ProcessStatusServiceModel, contextSource: RequestContextSource) extends ServerSideProcess with Logging {

  var repeater = Option.empty[Timer]

  def startTimeoutChecks(exe: Executor) {
    // we need to delay the timeout check a bit to make sure any already queued heartbeat messages are waiting
    // to be processed. If we checked the timeouts before processing all waiting messages we would always disable 
    // all applications if this coordinator had been turned off for longer than periodMs even if the other apps
    // had been sending heartbeats the whole.
    repeater = Some(exe.scheduleWithFixedOffset(10000.milliseconds, 10000.milliseconds) { doCheckTimeouts(exe) })
  }

  private def doCheckTimeouts(exe: Executor) {
    try {
      checkTimeouts(System.currentTimeMillis)
    } catch {
      case e: Exception => logger.error("Error checking timeout", e)
    }
  }

  def startProcess(exe: Executor) {
    startTimeoutChecks(exe)
  }
  def stopProcess() {
    repeater.foreach(_.cancel)
  }

  def checkTimeouts(now: Long) {
    contextSource.transaction { context =>
      ApplicationSchema.heartbeats.where(heartbeat => heartbeat.isOnline === true and (heartbeat.timeoutAt lte now)).foreach(h => {
        logger.info("App: " + h.instanceName.value + ": has timed out at: " + now + " (" + (h.timeoutAt - now) + ")")
        model.handleOfflineHeartbeat(context, h, now)
      })
    }
  }
}
