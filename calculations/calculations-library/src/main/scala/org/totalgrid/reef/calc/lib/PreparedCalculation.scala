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

import org.totalgrid.reef.client.service.proto.Measurements.Measurement
import org.totalgrid.reef.client.service.AllScadaService
import org.totalgrid.reef.client.{ SubscriptionEvent, SubscriptionEventAcceptor, Client, SubscriptionResult }
import org.totalgrid.reef.client.sapi.client.Event
import org.totalgrid.reef.client.proto.Envelope.SubscriptionEventType
import net.agileautomata.executor4s.Executor

import scala.collection.JavaConversions._

/*
trait CalculationInputProvider {

  def getRange(name: String): List[Measurement]
  def getSingle(name: String): Measurement
  //
}
trait PreparedCalculation {

  def doCalculation(argumentProvider: CalculationInputProvider): Measurement.Builder
}

case class OperationArgument(name: String, isList: Boolean)

trait OperationNode {

  def getArguments: List[OperationArgument]

  def apply(): Measurement.Builder = Measurement.newBuilder
}

object OperationsParser {
  def parse(ops: String): OperationNode = {
    new OperationNode {
      def getArguments = List(OperationArgument("A", true))
    }
  }
}

object CalculationFactory {

  def prepareCalculation(calc: Calculation): PreparedCalculation = {
    val opTree = OperationsParser.parse(calc.operations)

    val args = opTree.getArguments

    args.foreach { a =>
      if (calc.inputs.find(_.variableName == a).isEmpty) {
        throw new BadCalculationException("Operations expected variable with name: " + a)
      }
    }
    new PreparedCalculation {
      def doCalculation(argumentProvider: CalculationInputProvider) = opTree.apply()
    }
  }
}

trait DataCollator extends CalculationInputProvider with VariableUpdateProvider

class SubscriptionDataCollator(calcInputs: List[CalcInput], client: AllScadaService)
    extends DataCollator with SubscriptionEventAcceptor[Measurement] {

  // the buckets would get the smarts of maintaining the right set of data
  sealed abstract trait MeasurementBucket {
    def addMeas(meas: Measurement)
  }
  case class RangeBucket(var meases: List[Measurement]) extends MeasurementBucket {
    def addMeas(newMeas: Measurement) = meases ::= newMeas
  }
  case class SingleBucket(var meas: Option[Measurement]) extends MeasurementBucket {
    def addMeas(newMeas: Measurement) = meas = Some(newMeas)
  }

  def prepareBucket(input: CalcInput): MeasurementBucket = input.rangeSettings match {
    case MostRecentMeasurement => SingleBucket(None)
    case RecentHistory(time) => RangeBucket(Nil)
    case RecentSamples(samples) => RangeBucket(Nil)
  }

  val pointsToVariables = calcInputs.map { i => i.pointName -> i.variableName }.toMap
  val variableMappings = calcInputs.map { i => i.variableName -> prepareBucket(i) }.toMap

  def onEvent(event: SubscriptionEvent[Measurement]) {
    val meas = event.getValue
    pointsToVariables.get(meas.getName) match {
      case Some(variableName) =>
        variableMappings.get(variableName).get.addMeas(meas)
        notifyUpdated(variableName)
      case None => // unknown measuremen
    }
  }

  val result = client.subscribeToMeasurementsByNames(pointsToVariables.keys.toList)
  result.getResult.foreach { meas => onEvent(Event[Measurement](SubscriptionEventType.ADDED, meas)) }
  result.getSubscription.start(this)

  def getRange(variableName: String) = variableMappings.get(variableName) match {
    case Some(RangeBucket(meases)) => meases
    case Some(SingleBucket(_)) => throw new BadCalculationException("Expected Range, was single: " + variableName)
    case None => throw new BadCalculationException("Unknown variable: " + variableName)
  }

  def getSingle(variableName: String) = variableMappings.get(variableName) match {
    case Some(SingleBucket(meas)) => meas.getOrElse(throw new BadCalculationException("No measurement yet for: " + variableName))
    case Some(RangeBucket(_)) => throw new BadCalculationException("Expected Single, was range: " + variableName)
    case None => throw new BadCalculationException("Unknown variable: " + variableName)
  }
}

trait VariableUpdateListener {
  def onVariableUpdated(str: String)
}

trait VariableUpdateProvider {
  var listeners = List.empty[VariableUpdateListener]
  def addUpdateListener(listener: VariableUpdateListener) = listeners ::= listener

  def notifyUpdated(variableName: String) {
    listeners.foreach { _.onVariableUpdated(variableName) }
  }
}

trait CalcTrigger {

  var triggeredFun = Option.empty[() => Unit]

  def onTriggered(fun: => Unit) = triggeredFun = Some(() => fun)

  def notifyTriggered() = triggeredFun.foreach { _() }
}

class AnyUpdateTrigger(dataCollator: VariableUpdateProvider)
    extends CalcTrigger with VariableUpdateListener {

  dataCollator.addUpdateListener(this)

  def onVariableUpdated(str: String) = notifyTriggered()
}

object RealtimeTriggerFactory {
  def makeTrigger(calc: Calculation, dataCollator: VariableUpdateProvider, exe: Executor): CalcTrigger = {
    val trigger = calc.triggering match {
      case OnAnyUpdate => new AnyUpdateTrigger(dataCollator)
    }
    trigger
  }
}

class OutputHandler(calc: Calculation, preparedCalc: PreparedCalculation, argumentProvider: CalculationInputProvider) {

  val outputSettings = calc.output
  def doCalc(publisher: (Measurement) => Unit) {
    // need to get arguments to handle quality
    val rawOutput = preparedCalc.doCalculation(argumentProvider)

    rawOutput.setName(outputSettings.name)
    publisher(rawOutput.build)
  }
}

class RealtimeCalculationFactory(client: AllScadaService, exe: Executor) {

  def make(calc: Calculation, publisher: (Measurement) => Unit) {
    val dataCollator = new SubscriptionDataCollator(calc.inputs, client)

    val ops = CalculationFactory.prepareCalculation(calc)

    val trigger = RealtimeTriggerFactory.makeTrigger(calc, dataCollator, exe)

    val outputHandler = new OutputHandler(calc, ops, dataCollator)

    trigger.onTriggered {
      outputHandler.doCalc(publisher)
    }
  }

}

class BadCalculationException(msg: String) extends RuntimeException(msg)

case class CalcOutput(name: String, unit: String, accumulate: Boolean)
case class CalcInput(variableName: String, pointName: String, rangeSettings: CalcRangeSettings)

sealed abstract class CalcRangeSettings
case object MostRecentMeasurement extends CalcRangeSettings
case class RecentHistory(withinMillis: Long) extends CalcRangeSettings
case class RecentSamples(samples: Int) extends CalcRangeSettings

sealed abstract class CalcTriggerSettings
case object OnAnyUpdate extends CalcTriggerSettings
case class OnMeasurementUpdate(measName: String) extends CalcTriggerSettings
case class OnMeasurementUpdateOutOfDeadband(measName: String, deadbandSize: Long) extends CalcTriggerSettings
case class OnPeriod(periodMillis: Long) extends CalcTriggerSettings
case class OnSchedule(cronString: String) extends CalcTriggerSettings

case class Calculation(operations: String, triggering: CalcTriggerSettings, inputs: List[CalcInput], output: CalcOutput)

trait CalculationService {
  def addCalculation(calc: Calculation, endpointName: String): Calculation

  def deleteCalculation(calc: Calculation): Calculation

  def getCalculationForPoint(outputPoint: String): Calculation

  def getCalculationsForEndpoint(endpointName: String): List[Calculation]

  def subscribeToCalculationsForEndpoint(endpointName: String): SubscriptionResult[List[Calculation], Calculation]
} */

