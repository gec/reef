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

import com.weiglewilczek.slf4s.Logging
import eval.{ EvalException, OperationSource, Expression }

class CalculationEvaluator(name: String,
  operationSource: OperationSource,
  inputData: InputDataSource,
  formula: Expression,
  qualInputStrategy: QualityInputStrategy,
  qualOutputStrategy: QualityOutputStrategy,
  timeStrategy: TimeStrategy,
  publisher: OutputPublisher)
    extends Logging {

  def attempt() {

    if (inputData.hasSufficient) {

      qualInputStrategy.checkInputs(inputData.getSnapshot).foreach { inputs =>

        try {
          val source = MappedVariableSource(inputs)

          val result = formula.evaluate(source, operationSource)

          val qual = qualOutputStrategy.getQuality(inputs)
          val time = timeStrategy.getTime(inputs)

          val outMeas = MeasurementConverter.convertOperationValue(result)
            .setQuality(qual)
            .setTime(time)

          publisher.publishMeasurement(outMeas)

        } catch {
          case ev: EvalException => logger.error("Calc: " + name + " evaluation error: " + ev.getMessage)
        }
      }
    }
  }
}
