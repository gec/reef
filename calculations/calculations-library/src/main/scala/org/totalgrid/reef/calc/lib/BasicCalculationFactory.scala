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

import eval.{ OperationParser, OperationSource }
import org.totalgrid.reef.client.sapi.client.rest.Client
import net.agileautomata.executor4s.Cancelable

import org.totalgrid.reef.client.service.proto.OptionalProtos._
import org.totalgrid.reef.client.service.proto.Calculations.{ InputQuality, Calculation }

/*

message Calculation{
    optional org.totalgrid.reef.client.service.proto.Model.ReefUUID          uuid               = 9;
    optional org.totalgrid.reef.client.service.proto.Model.Point             output_point       = 1;
    optional bool              accumulate         = 2;

    optional TriggerStrategy   triggering         = 3;
    repeated CalculationInput  calc_inputs        = 4;

    optional InputQuality      triggering_quality = 5;
    optional OutputQuality     quality_output     = 6;
    optional OutputTime        time_output        = 7;

    optional string            formula            = 8;
}

 */

class BasicCalculationFactory(client: Client, operations: OperationSource) extends CalculationFactory {

  def build(config: Calculation): Cancelable = {

    val expr = config.formula.map(OperationParser.parseFormula(_)).getOrElse {
      throw new Exception("Need formula in calculation config")
    }

    val qualInputStrat = config.triggeringQuality.strategy.map(QualityInputStrategy.build(_)).getOrElse {
      throw new Exception("Need quality input strategy in calculation config")
    }

    val qualOutputStrat = config.qualityOutput.strategy.map(QualityOutputStrategy.build(_)).getOrElse {
      throw new Exception("Need quality output strategy in calculation config")
    }

    val timeOutputStrat = config.timeOutput.strategy.map(TimeStrategy.build(_)).getOrElse {
      throw new Exception("Need time strategy in calculation config")
    }

    null
  }
}

trait InputBucket {
  def hasSufficient: Boolean
}

abstract class RunningCalculation extends Cancelable {
  protected val calcTrigger: Cancelable
  protected val inputManager: InputManager
}

