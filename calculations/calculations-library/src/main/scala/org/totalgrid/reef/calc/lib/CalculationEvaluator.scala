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

import com.typesafe.scalalogging.slf4j.Logging
import org.totalgrid.reef.calc.lib.eval.{ EvalException, Formula }
import org.totalgrid.reef.client.service.proto.Measurements.Measurement

case class CalculationComponents(formula: Formula,
  qualInputStrategy: QualityInputStrategy,
  qualOutputStrategy: QualityOutputStrategy,
  timeStrategy: TimeStrategy,
  measSettings: MeasurementSettings)

class CalculationEvaluator(
  inputData: InputDataSource,
  publisher: OutputPublisher,
  components: CalculationComponents,
  metrics: CalculationMetrics)
    extends Logging {

  def attempt() {
    import components._

    metrics.attempts(1)

    inputData.getSnapshot.flatMap(qualInputStrategy.checkInputs(_)).foreach { inputs =>
      metrics.evals(1)
      metrics.evalTime {
        try {
          val m = attemptCalculation(inputs)

          publisher.publish(m)

        } catch {

          case ev: EvalException =>
            metrics.errors(1)
            val name = components.measSettings.name
            logger.error("Calc: " + name + " evaluation error: " + ev.getMessage)
            try {
              publisher.publish(ErrorMeasurement.build(name))
            } catch {
              case ex: Exception => logger.error("Error on publishing error meas: " + ex.toString)
            }
        }
      }
    }

  }

  def attemptCalculation(inputs: Map[String, List[Measurement]]): Measurement = {
    import components._
    val source = MappedVariableSource(inputs)

    val result = formula.evaluate(source)

    val qual = qualOutputStrategy.getQuality(inputs)
    val time = timeStrategy.getTime(inputs)

    val outMeas = MeasurementConverter.convertOperationValue(result)
      .setQuality(qual)
      .setTime(time)

    measSettings.set(outMeas).build
  }
}

