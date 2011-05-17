/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Green Energy Corp licenses this file
 * to you under the GNU Affero General Public License Version 3.0
 * (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.services.core.util

import org.totalgrid.reef.messaging.AMQPProtoFactory
import org.totalgrid.reef.util.Logging
import org.totalgrid.reef.services.ProtoServiceCoordinator
import org.totalgrid.reef.reactor.Reactable
import org.totalgrid.reef.measurementstore.MeasurementStore

class HistoryTrimmer(ms: MeasurementStore, period: Long, totalMeasurements: Long) extends ProtoServiceCoordinator with Logging {
  def addAMQPConsumers(amqp: AMQPProtoFactory, reactor: Reactable) {
    if (!ms.supportsTrim) return
    reactor.repeat(period) {
      val trimmed = ms.trim(totalMeasurements)
      if (trimmed > 0) {
        reefLogger.debug("trimmed: {} measurements", trimmed)
      }
    }
  }
}