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

  import ProtoUtils.safeValueOf

  def prepareCalculationProto(outputPointName: String, outputUnit: String, basePointName: String, calc: CalcXml) = {
    val builder = Calculation.newBuilder

    builder.setOutputPoint(Point.newBuilder.setName(outputPointName).setUnit(outputUnit))

    val inputs = parseCalcInputs(basePointName, calc)
    builder.addAllCalcInputs(inputs)

    val variablesDefined = inputs.map { _.getVariableName }
    // TODO: check that formula matches input variables
    val variablesExpected = variablesDefined //OperationsParser.parse(formula).getArguments

    if (variablesDefined != variablesDefined.distinct) {
      throw new LoadingException("Some variables were defined more than once " + variablesDefined.mkString("(", ",", ")"))
    }

    if (variablesDefined.sorted != variablesExpected.sorted) {
      throw new LoadingException("Not all variables in forumula " + variablesExpected.mkString("(", ",", ")") +
        " were defined in inputs " + variablesDefined.mkString("(", ",", ")"))
    }

    val triggerSettings = parseTriggers(calc)
    builder.setTriggering(triggerSettings)

    builder.setTriggeringQuality(parseTriggeringQuality(calc))

    builder.setTimeOutput(parseTimeOutput(calc))
    builder.setQualityOutput(parseOutputQuality(calc))

    builder.setAccumulate(calc.isSetOutput && calc.getOutput.isSetAccumulate && calc.getOutput.getAccumulate)

    if (!calc.isSetFormula || !calc.getFormula.isSetValue) throw new LoadingException("Formula must not be blank.")
    val formula = calc.getFormula.getValue
    builder.setFormula(formula)

    (builder.build, inputs.map { _.getPoint.getName })
  }

  private def parseTimeOutput(calc: CalcXml) = {
    val timeSetting = OutputTime.newBuilder
    if (calc.isSetOutput && calc.getOutput.isSetOutputTime) {
      val strategy = safeValueOf(calc.getOutput.getOutputTime.getStrategy, OutputTime.Strategy.values(), OutputTime.Strategy.valueOf _)
      timeSetting.setStrategy(strategy)
    } else {
      timeSetting.setStrategy(OutputTime.Strategy.MOST_RECENT)
    }
    timeSetting
  }

  private def parseOutputQuality(calc: CalcXml) = {
    val timeSetting = OutputQuality.newBuilder
    if (calc.isSetOutput && calc.getOutput.isSetOutputQuality) {
      val strategy = safeValueOf(calc.getOutput.getOutputQuality.getStrategy, OutputQuality.Strategy.values(), OutputQuality.Strategy.valueOf _)
      timeSetting.setStrategy(strategy)
    } else {
      timeSetting.setStrategy(OutputQuality.Strategy.WORST_QUALITY)
    }
    timeSetting
  }

  private def parseTriggeringQuality(calc: CalcXml) = {
    val inputQuality = InputQuality.newBuilder
    if (calc.isSetTriggering && calc.getTriggering.isSetInputQualityStrategy) {
      val strategy = safeValueOf(calc.getTriggering.getInputQualityStrategy, InputQuality.Strategy.values(), InputQuality.Strategy.valueOf _)
      inputQuality.setStrategy(strategy)
    } else {
      inputQuality.setStrategy(InputQuality.Strategy.ONLY_WHEN_ALL_OK)
    }
    inputQuality
  }

  private def parseTriggers(calc: CalcXml) = {
    val triggerSettings = TriggerStrategy.newBuilder
    if (calc.isSetTriggering) {
      val trigger = calc.getTriggering
      val options = List(trigger.isSetUpdateEveryPeriodMS, trigger.isSetUpdateOnAnyChange)
      if (options.filter(_ == true).size > 1) throw new LoadingException("Must only specify one trigger type.")
      trigger match {
        case t if (t.isSetUpdateEveryPeriodMS) => triggerSettings.setPeriodMs(t.getUpdateEveryPeriodMS)
        case t if (t.isSetUpdateOnAnyChange) => triggerSettings.setUpdateAny(t.isSetUpdateOnAnyChange && t.getUpdateOnAnyChange)
        case _ => triggerSettings.setUpdateAny(true)
      }
    } else {
      triggerSettings.setUpdateAny(true)
    }
    triggerSettings
  }

  private def parseCalcInputs(basePointName: String, calc: CalcXml) = {
    val inputs = calc.getInputs.getSingleOrMulti.toList
    if (inputs.isEmpty) throw new LoadingException("Must include atleast one input in a calculation")
    inputs.map { node: InputType =>

      val input = CalculationInput.newBuilder

      input.setVariableName(node.getVariable)

      val inputName = if (node.getAddParentNames) basePointName + node.getPointName
      else node.getPointName

      input.setPoint(Point.newBuilder.setName(inputName))

      node match {
        case s: Single =>
          val strategy = SingleMeasurement.MeasurementStrategy.MOST_RECENT
          input.setSingle(SingleMeasurement.newBuilder.setStrategy(strategy))
        case m: Multi if (m.isSetSampleRange) =>
          val range = MeasurementRange.newBuilder.setLimit(m.getSampleRange.getLimit.toInt)
          if (m.isSetSinceLastPublish && m.getSinceLastPublish) range.setSinceLast(true)
          input.setRange(range)
        case m: Multi if (m.isSetTimeRange) =>
          val range = MeasurementRange.newBuilder
          val rangeNode = m.getTimeRange
          if (m.isSetSinceLastPublish && m.getSinceLastPublish) throw new LoadingException("Cannot use both timeRange and sinceLastPublish on same variable")
          if (!rangeNode.isSetFrom && !rangeNode.isSetTo && !rangeNode.isSetLimit) throw new LoadingException("Must set atleast one of from,to,limit in TimeRange")
          if (rangeNode.isSetFrom) range.setFromMs(rangeNode.getFrom)
          if (rangeNode.isSetTo) range.setToMs(rangeNode.getTo)
          if (rangeNode.isSetLimit) range.setLimit(rangeNode.getLimit.toInt)
          input.setRange(range)
        case m: Multi if (m.isSetSinceLastPublish) =>
          if (!m.isSetSinceLastPublish || !m.getSinceLastPublish) throw new LoadingException("Must include sampleRange or timeRange if disabling sinceLastPublish")
          input.setRange(MeasurementRange.newBuilder.setSinceLast(true))
      }

      input.build
    }
  }
}
