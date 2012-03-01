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
package org.totalgrid.reef.calc.lib

import org.totalgrid.reef.client.service.proto.Measurements.Measurement
import net.agileautomata.executor4s._
import org.totalgrid.reef.client.service.proto.Calculations.{ TriggerStrategy, Calculation }

sealed trait CalculationTriggerStrategy

trait InitiatingTriggerStrategy extends CalculationTriggerStrategy with Cancelable {
  def start(): Unit
}

trait EventedTriggerStrategy extends CalculationTriggerStrategy {
  def handle(m: Measurement)
}

class IntervalTrigger(exe: Executor, interval: Long, handler: () => Unit) extends InitiatingTriggerStrategy {
  var timer: Option[Timer] = None
  def start() {
    timer = Some(exe.scheduleWithFixedOffset(interval.milliseconds)(handler()))
  }
  def cancel() {
    timer.foreach(_.cancel())
  }
}

class AnyUpdateTrigger(handler: () => Unit) extends EventedTriggerStrategy {
  def handle(m: Measurement) {
    handler()
  }
}

object CalculationTriggerStrategy {

  def build(config: TriggerStrategy, exe: Executor, handler: () => Unit): CalculationTriggerStrategy = {
    if (config.hasPeriodMs) {
      new IntervalTrigger(exe, config.getPeriodMs, handler)
    } else if (config.hasUpdateAny && config.getUpdateAny) {
      new AnyUpdateTrigger(handler)
    } else {
      throw new Exception("Can't use trigger configuration: " + config)
    }
  }
}
