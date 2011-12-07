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
package org.totalgrid.reef.measproc.processing

import org.totalgrid.reef.client.service.proto.Measurements._

object Action {
  type Evaluation = (Measurement) => Measurement

  sealed abstract class ActivationType {
    def apply(state: Boolean, prev: Boolean) = activated(state, prev)
    def activated(state: Boolean, prev: Boolean): Boolean
  }
  object High extends ActivationType {
    def activated(state: Boolean, prev: Boolean) = state
  }
  object Low extends ActivationType {
    def activated(state: Boolean, prev: Boolean) = !state
  }
  object Rising extends ActivationType {
    def activated(state: Boolean, prev: Boolean) = (state && !prev)
  }
  object Falling extends ActivationType {
    def activated(state: Boolean, prev: Boolean) = (!state && prev)
  }
  object Transition extends ActivationType {
    def activated(state: Boolean, prev: Boolean) = (state && !prev) || (!state && prev)
  }
}

/**
 * Container for processor actions, maintains the logic necessary to determine if
 * an action should be executed.
 *
 */
trait Action {
  /**
   * Name of the action
   */
  val name: String

  /**
   * Conditional action evaluation. Actions define what combination of previous state and current state
   * cause activation.
   *
   * @param m         Measurement to be acted upon
   * @param state     Current state of the trigger condition this action is associated with
   * @param prev      Previous state of the trigger condition this action is associated with
   * @return          Result of processing
   */
  def process(m: Measurement, state: Boolean, prev: Boolean): Measurement
}

/**
 * Implementation of action processing container.
 */
class BasicAction(val name: String, disabled: Boolean, activation: Action.ActivationType, eval: Action.Evaluation)
    extends Action {

  def process(m: Measurement, state: Boolean, prev: Boolean): Measurement = {
    if (!disabled && activation(state, prev))
      eval(m)
    else
      m
  }

  override def toString = name
}

