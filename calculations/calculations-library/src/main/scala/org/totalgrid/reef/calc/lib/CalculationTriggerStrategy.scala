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

sealed trait CalculationTriggerStrategy {
  private var handler = Option.empty[() => Unit]
  protected def attemptCalculation = (handler.get)()
  def setEvaluationFunction(func: () => Unit) = handler = Some(func)
}

trait InitiatingTriggerStrategy extends CalculationTriggerStrategy with Cancelable {
  def start(exe: Executor): Unit
}

trait EventedTriggerStrategy extends CalculationTriggerStrategy {
  def handle(m: Measurement)
}

class IntervalTrigger(interval: Long) extends InitiatingTriggerStrategy {
  var timer: Option[Timer] = None
  def start(exe: Executor) {
    timer = Some(exe.scheduleWithFixedOffset(interval.milliseconds)(attemptCalculation))
  }
  def cancel() {
    timer.foreach(_.cancel())
  }
}

class AnyUpdateTrigger extends EventedTriggerStrategy {
  def handle(m: Measurement) {
    attemptCalculation
  }
}

object CalculationTriggerStrategy {

  def build(config: TriggerStrategy): CalculationTriggerStrategy = {
    if (config.hasPeriodMs) {
      new IntervalTrigger(config.getPeriodMs)
    } else if (config.hasUpdateAny && config.getUpdateAny) {
      new AnyUpdateTrigger
    } else {
      new AnyUpdateTrigger
      //throw new Exception("Can't use trigger configuration: " + config)
    }
  }
}
