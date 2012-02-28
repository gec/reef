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

import eval._
import org.totalgrid.reef.client.sapi.client.rest.Client
import org.totalgrid.reef.client.service.proto.Calculations.Calculation
import net.agileautomata.executor4s.Cancelable
import org.totalgrid.reef.client.service.proto.Measurements.{ Quality, Measurement }
import com.weiglewilczek.slf4s.Logging

class BasicCalculationFactory(client: Client) extends CalculationFactory {

  def build(config: Calculation): Cancelable = {

    null
  }
}

trait InputBucket {
  def getOperationValue: OperationValue
  def hasSufficient: Boolean
}

abstract class RunningCalculation extends Cancelable {
  protected val calcTrigger: Cancelable
  protected val inputManager: InputManager
}

