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

import org.totalgrid.reef.client.service.proto.Calculations.InputQuality
import org.totalgrid.reef.client.service.proto.Measurements.{ Quality, Measurement }

trait QualityInputStrategy {
  def checkInputs(inputs: Map[String, List[Measurement]]): Option[Map[String, List[Measurement]]]
}

object QualityInputStrategy {

  def build(config: InputQuality.Strategy): QualityInputStrategy = config match {
    case InputQuality.Strategy.ONLY_WHEN_ALL_OK => new WhenAllOk
    case InputQuality.Strategy.REMOVE_BAD_AND_CALC => new FilterOutBad
    case _ => throw new Exception("Unknown quality input strategy")
  }

  class WhenAllOk extends QualityInputStrategy {
    def checkInputs(inputs: Map[String, List[Measurement]]): Option[Map[String, List[Measurement]]] = {
      if (inputs.values.forall(_.forall(m => m.getQuality.getValidity == Quality.Validity.GOOD))) {
        Some(inputs)
      } else {
        None
      }
    }
  }

  class FilterOutBad extends QualityInputStrategy {
    def checkInputs(inputs: Map[String, List[Measurement]]): Option[Map[String, List[Measurement]]] = {
      Some(inputs.mapValues { _.filter(_.getQuality.getValidity == Quality.Validity.GOOD) })
    }
  }
}
