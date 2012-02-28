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

import org.totalgrid.reef.client.service.proto.Calculations.Calculation
import net.agileautomata.executor4s.{ Executor, Cancelable }

//class CalculationTrigger(handler: () => Unit)

class AnyUpdateTrigger(subscriptionManager: InputSubscriptionManager, handler: () => Unit) {

  subscriptionManager.onAnyChange { m => handler() }
}

trait CalculationTriggerFactory {
  def makeTrigger(calc: Calculation): Cancelable
}

class RealCalculationTriggerFactory(subscriptionManager: InputSubscriptionManager, exe: Executor) extends CalculationTriggerFactory {

  def makeTrigger(calc: Calculation): Cancelable = {
    null
  }
}

/*
trait CalcTrigger {

  var triggeredFun = Option.empty[() => Unit]

  def onTriggered(fun: => Unit) = triggeredFun = Some(() => fun)

  def notifyTriggered() = triggeredFun.foreach { _() }
}

class AnyUpdateTrigger(dataCollator: VariableUpdateProvider)
    extends CalcTrigger with VariableUpdateListener {

  dataCollator.addUpdateListener(this)

  def onVariableUpdated(str: String) = notifyTriggered()
}

object RealtimeTriggerFactory {
  def makeTrigger(calc: Calculation, dataCollator: VariableUpdateProvider, exe: Executor): CalcTrigger = {
    val trigger = calc.triggering match {
      case OnAnyUpdate => new AnyUpdateTrigger(dataCollator)
    }
    trigger
  }
}
 */
