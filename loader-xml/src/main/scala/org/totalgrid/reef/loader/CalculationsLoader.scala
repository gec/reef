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
package org.totalgrid.reef.loader

import org.totalgrid.reef.loader.calculations.{ Calculation => CalcXml, Single, Multi, InputType }
import scala.collection.JavaConversions._

import org.totalgrid.reef.client.service.proto.Calculations._
import org.totalgrid.reef.client.service.proto.Model.Point

object CalculationsLoader {

  def prepareCalculationProto(outputPointName: String, basePointName: String, calc: CalcXml) = {
    val builder = Calculation.newBuilder

    builder.setOutputPoint(Point.newBuilder.setName(outputPointName))
    builder.setAccumulate(false)

    val triggerSettings = TriggerStrategy.newBuilder.setUpdateAny(true)
    builder.setTriggering(triggerSettings)

    if (!calc.getFormula.isSetValue) throw new LoadingException("Formula must not be blank.")
    val formula = calc.getFormula.getValue
    builder.setFormula(formula)

    val inputs = parseCalcInputs(basePointName, calc)
    builder.addAllCalcInputs(inputs)

    val variablesDefined = inputs.map { _.getVariableName }
    val variablesExpected = variablesDefined //OperationsParser.parse(formula).getArguments

    if (variablesDefined.sorted != variablesExpected.sorted) {
      throw new LoadingException("Not all variables in forumula (" + variablesExpected.mkString(",") +
        ") were defined in inputs (" + variablesDefined.mkString(",") + ")")
    }

    (builder.build, inputs.map { _.getPoint.getName })
  }

  private def parseCalcInputs(basePointName: String, calc: CalcXml) = {
    calc.getInputs.getSingleOrMulti.toList.map { node: InputType =>

      val input = CalculationInput.newBuilder

      input.setVariableName(node.getVariable)

      val inputName = if (node.isAddParentNames) basePointName + node.getPointName
      else node.getPointName

      input.setPoint(Point.newBuilder.setName(inputName))

      node match {
        case s: Single =>
          val strategy = SingleMeasurement.MeasurementStrategy.MOST_RECENT
          input.setSingle(SingleMeasurement.newBuilder.setStrategy(strategy))
        case m: Multi if (m.isSetSampleRange) =>
          val range = MeasurementRange.newBuilder.setLimit(m.getSampleRange.getLimit.toInt)
          input.setRange(range)
        case m: Multi if (m.isSetTimeRange) =>
          val range = MeasurementRange.newBuilder
          val rangeNode = m.getTimeRange
          if (!rangeNode.isSetFrom && !rangeNode.isSetTo && !rangeNode.isSetTo) throw new LoadingException("Must set atleast one of from,to,limit in TimeRange")
          if (rangeNode.isSetFrom) range.setFromMs(rangeNode.getFrom)
          if (rangeNode.isSetTo) range.setToMs(rangeNode.getTo)
          if (rangeNode.isSetLimit) range.setLimit(rangeNode.getLimit.toInt)
          input.setRange(range)
      }

      input.build
    }
  }

}
