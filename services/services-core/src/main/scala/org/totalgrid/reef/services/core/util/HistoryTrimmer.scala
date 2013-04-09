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
package org.totalgrid.reef.services.core.util

import com.typesafe.scalalogging.slf4j.Logging

import org.totalgrid.reef.measurementstore.MeasurementStore
import org.totalgrid.reef.services.framework.ServerSideProcess
import net.agileautomata.executor4s._

class HistoryTrimmer(ms: MeasurementStore, period: Long, totalMeasurements: Long) extends ServerSideProcess with Logging {

  var repeater = Option.empty[Timer]

  def startProcess(exe: Executor) {
    if (ms.supportsTrim) repeater = Some(exe.scheduleWithFixedOffset(period.milliseconds)(doTrimOperation(exe)))
  }
  def stopProcess() {
    repeater.foreach(_.cancel)
  }

  private def doTrimOperation(exe: Executor) {
    val num = ms.trim(totalMeasurements)
    if (num > 0) {
      logger.debug("trimmed: " + num + " measurements")
    }
  }
}