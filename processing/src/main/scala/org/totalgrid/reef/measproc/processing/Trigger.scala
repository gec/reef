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
import org.totalgrid.reef.persistence.ObjectCache
import com.weiglewilczek.slf4s.Logging
import org.totalgrid.reef.client.sapi.types.Optional._

object Trigger extends Logging {

  /**
   * Super-type for condition logic.
   */
  type Condition = (Measurement, Boolean) => Boolean

  /**
   * Helper function to extract analog values from measurements
   * @param m   Measurement
   * @return    Value in form of Double, or None
   */
  def analogValue(m: Measurement): Option[Double] = {
    (m.hasDoubleVal ? m.getDoubleVal) orElse
      (m.hasIntVal ? m.getIntVal.asInstanceOf[Double])
  }

  /**
   * Evaluates a list of triggers against a measurement
   * @param m           Measurement input
   * @param cache       Previous trigger state cache
   * @param triggers    Triggers associated with the measurement point.
   * @return            Result of trigger/action processing.
   */
  def processAll(m: Measurement, cache: ObjectCache[Boolean], triggers: List[Trigger]): Measurement = {
    triggers.foldLeft(m) { (meas, trigger) =>
      // Evaluate trigger
      logger.debug("Applying trigger: " + trigger + " to meas: " + meas)
      val (result, stopProcessing) = trigger.process(meas, cache)

      // Either continue folding or return immediately if the trigger requires us to stop processing
      logger.debug("Result: " + result + ", stop: " + stopProcessing)
      if (stopProcessing) return result
      else result
    }
  }
}

/**
 * Interface for conditional measurement processing logic
 */
trait Trigger {
  /**
   * Conditionally process input measurement. May raise flag to stop further processing.
   *
   * @param m         Input measurement.
   * @param cache     Previous trigger state cache.
   * @return          Measurement result, flag to stop further processing.
   */
  def process(m: Measurement, cache: ObjectCache[Boolean]): (Measurement, Boolean)
}

/**
 * Implementation of conditional measurement processing logic. Maintains a condition function
 * and a list of actions to perform. Additionally may stop processing when condition is in a
 * given state.
 */
class BasicTrigger(
  cacheID: String,
  conditions: List[Trigger.Condition],
  actions: List[Action],
  stopProcessing: Option[Action.ActivationType])
    extends Trigger with Logging {

  def process(m: Measurement, cache: ObjectCache[Boolean]): (Measurement, Boolean) = {

    // Get the previous state of this trigger
    val prev = cache.get(cacheID) getOrElse false

    // Evaluate the current state
    //info("Conditions: " + conditions)
    val state = if (conditions.isEmpty) true
    else conditions.forall(_(m, prev))

    // Store the state in the previous state cache
    cache.put(cacheID, state)

    // Allow actions to determine if they should evaluate, roll up a result measurement
    //info("Trigger state: " + state)
    //val res = actions.foldLeft(m) { (meas, action) => action.process(meas, state, prev) }

    val opt: Option[Measurement] = Some(m)
    val res = actions.foldLeft(opt) { (optMeas, action) => optMeas.map(meas => action.process(meas, state, prev)).flatten }

    res match {
      case None => (m, false)
      case Some(result) => {

        // Check stop processing flag (default to continue processing)
        val stopProc = stopProcessing.map(_(state, prev)) getOrElse false
        (result, stopProc)
      }
    }
  }

  override def toString = cacheID + " (" + actions.mkString(", ") + ")"
}

