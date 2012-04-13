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

import org.totalgrid.reef.calc.lib.eval._
import org.totalgrid.reef.client.sapi.client.rest.Client
import org.totalgrid.reef.client.service.proto.OptionalProtos._
import org.totalgrid.reef.client.service.proto.Calculations.{ Calculation }
import net.agileautomata.executor4s.{ Cancelable }
import scala.collection.JavaConversions._
import org.totalgrid.reef.client.sapi.rpc.AllScadaService
import com.weiglewilczek.slf4s.Logging

case class CalculationSettings(components: CalculationComponents,
  triggerStrategy: CalculationTriggerStrategy,
  inputs: List[InputConfig],
  accumulate: Boolean)

class BasicCalculationFactory(
    rootClient: Client,
    operations: OperationSource,
    metricsSource: CalculationMetricsSource,
    output: OutputPublisher,
    timeSource: TimeSource) extends CalculationFactory with Logging {

  import BasicCalculationFactory._

  def build(config: Calculation): Cancelable = {
    try {
      setupCalculation(config)
    } catch {
      case e: Exception =>
        val pointName = config.getOutputPoint.getName
        logger.error("Error setting up calculation for point: " + pointName + " - " + e.getMessage, e)
        output.publish(ErrorMeasurement.build(pointName))
        new Cancelable {
          def cancel() {}
        }
    }
  }

  private def setupCalculation(config: Calculation): Cancelable = {

    var settings = parseConfig(config, operations)

    val metrics = metricsSource.getCalcMetrics(settings.components.measSettings.name)

    // get a new client (strand) for each calculation
    val client = rootClient.spawn()
    val services = client.getRpcInterface(classOf[AllScadaService])

    val currentMeasurement = services.getMeasurementByName(settings.components.measSettings.name).await

    if (settings.accumulate) {
      val meas = try {
        MeasurementConverter.convertMeasurement(currentMeasurement)
      } catch {
        case e: EvalException =>
          NumericConst(0)
      }
      val accumulatedFormula = new AccumulatedFormula(meas, settings.components.formula)
      settings = settings.copy(components = settings.components.copy(formula = accumulatedFormula))
    }

    val inputDataManager = new MeasInputManager(services, timeSource)

    val evaluator = new CalculationEvaluator(inputDataManager, output, settings.components, metrics)

    val (eventedTrigger, initiatingTrigger) = settings.triggerStrategy match {
      case ev: EventedTriggerStrategy => (Some(ev), None)
      case in: InitiatingTriggerStrategy => (None, Some(in))
    }

    settings.triggerStrategy.setEvaluationFunction(evaluator.attempt)

    inputDataManager.initialize(currentMeasurement, settings.inputs, eventedTrigger)

    initiatingTrigger.foreach(_.start(client))

    new MultiCancelable(List(Some(inputDataManager), initiatingTrigger).flatten)
  }
}

object BasicCalculationFactory {
  class MultiCancelable(list: List[Cancelable]) extends Cancelable {
    def cancel() { list.foreach(_.cancel()) }
  }

  /**
   * takes a calculation config and parses it into a valid
   */
  def parseConfig(config: Calculation, operations: OperationSource): CalculationSettings = {
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

    val name = config.outputPoint.name.getOrElse {
      throw new Exception("Must have output point name")
    }

    val measSettings = MeasurementSettings(name, config.outputPoint.unit)

    val formula = Formula(expr, operations)

    val components = CalculationComponents(formula, qualInputStrat, qualOutputStrat, timeOutputStrat, measSettings)

    val inputs = config.getCalcInputsList.toList.map(InputBucket.build(_))

    val triggerStrategy = config.triggering.map(CalculationTriggerStrategy.build(_)).getOrElse {
      throw new Exception("Must have triggering config")
    }

    CalculationSettings(components, triggerStrategy, inputs, config.getAccumulate)
  }
}

