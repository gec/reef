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

import org.totalgrid.reef.client.service.proto.Measurements.{ Quality, Measurement }
import org.totalgrid.reef.client.service.proto.Calculations.OutputQuality

trait QualityOutputStrategy {
  def getQuality(inputs: Map[String, List[Measurement]]): Quality
}

object QualityOutputStrategy {
  def build(config: OutputQuality.Strategy) = config match {
    case OutputQuality.Strategy.ALWAYS_OK => new AlwaysOk
    case _ => throw new Exception("Unknown quality output strategy")
  }

  class AlwaysOk extends QualityOutputStrategy {
    def getQuality(inputs: Map[String, List[Measurement]]): Quality = {
      Quality.newBuilder().setValidity(Quality.Validity.GOOD).setSource(Quality.Source.PROCESS).build()
    }
  }
}
