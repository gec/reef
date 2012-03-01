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

import org.totalgrid.reef.util.XMLHelper

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import scala.collection.JavaConversions._

import org.totalgrid.reef.client.service.proto.Calculations._
import org.totalgrid.reef.client.service.proto.Calculations.SingleMeasurement.MeasurementStrategy
import org.totalgrid.reef.loader.equipment.PointProfile
import EnhancedXmlClasses._

@RunWith(classOf[JUnitRunner])
class CalculationsLoaderTest extends FunSuite with ShouldMatchers {

  def getXmlString(snippet: String) = """<?xml version="1.0" encoding="utf-8" standalone="yes"?>
<pointProfile version="1.0"
  targetNamespace="equipment.loader.reef.totalgrid.org"
  xmlns="equipment.loader.reef.totalgrid.org"
  xmlns:calc="calculations.loader.reef.totalgrid.org">
  """ + snippet + "</pointProfile>"

  def getCalculation(snip: String) = {
    val xml = XMLHelper.read(getXmlString(snip), classOf[PointProfile])
    val (c, inputNames) = CalculationsLoader.prepareCalculationProto("Output", "P", "BaseName.", xml.getCalculation(0))
    val inputs = inputNames.zip(c.getCalcInputsList.toList)
    (c, inputs)
  }

  def checkInput(inputs: List[(String, CalculationInput)], variable: String, name: String) = {
    val found = inputs.find(_._2.getVariableName == variable)
    found should not equal (None)
    found.get._1 should equal(name)
    found.get._2.getPoint.getName should equal(name)
    found.get._2
  }

  test("Simple Relative Names") {
    val testSnip = """
    <calc:calculation>
      <calc:inputs>
          <calc:single pointName="Current" variable="A"/>
          <calc:single pointName="Voltage" variable="B"/>
      </calc:inputs>
      <calc:formula>A * B</calc:formula>
    </calc:calculation>
    """
    val (c, inputs) = getCalculation(testSnip)

    c.getOutputPoint.getName should equal("Output")
    c.getAccumulate should equal(false)

    c.getFormula should equal("A * B")

    val inputA = checkInput(inputs, "A", "BaseName.Current")
    inputA.getSingle.getStrategy should equal(MeasurementStrategy.MOST_RECENT)

    val inputB = checkInput(inputs, "B", "BaseName.Voltage")
    inputB.getSingle.getStrategy should equal(MeasurementStrategy.MOST_RECENT)
  }

  test("Absolute Input PointNames") {
    val testSnip = """
    <calc:calculation>
      <calc:inputs>
          <calc:single pointName="Current" variable="A" addParentNames="false"/>
          <calc:single pointName="Voltage" variable="B" addParentNames="false"/>
      </calc:inputs>
      <calc:formula>A * B</calc:formula>
    </calc:calculation>
    """
    val (c, inputs) = getCalculation(testSnip)

    checkInput(inputs, "A", "Current")
    checkInput(inputs, "B", "Voltage")
  }

  test("Basic Ranges") {
    val testSnip = """
    <calc:calculation>
      <calc:inputs>
          <calc:multi pointName="TimeBased" variable="A" addParentNames="false">
              <calc:timeRange from="-300000"/>
          </calc:multi>
          <calc:multi pointName="MovingAverage" variable="B" addParentNames="false">
              <calc:sampleRange limit="100"/>
          </calc:multi>
      </calc:inputs>
      <calc:formula>A * B</calc:formula>
    </calc:calculation>
    """
    val (c, inputs) = getCalculation(testSnip)

    val timeRange = checkInput(inputs, "A", "TimeBased").getRange
    timeRange.getFromMs should equal(-300000)
    timeRange.hasToMs should equal(false)
    timeRange.hasLimit should equal(false)
    val sampleRange = checkInput(inputs, "B", "MovingAverage").getRange
    sampleRange.getLimit should equal(100)
    sampleRange.hasToMs should equal(false)
    sampleRange.hasFromMs should equal(false)
  }

  test("Duplicated variables are detected") {
    val testSnip = """
    <calc:calculation>
      <calc:inputs>
          <calc:single pointName="Current" variable="A"/>
          <calc:single pointName="Voltage" variable="A"/>
      </calc:inputs>
      <calc:formula>A * B</calc:formula>
    </calc:calculation>
    """
    intercept[LoadingException] {
      getCalculation(testSnip)
    }
  }

  ignore("Formula includes unbound variables") {
    val testSnip = """
    <calc:calculation>
      <calc:inputs>
          <calc:single pointName="Current" variable="A"/>
          <calc:single pointName="Voltage" variable="B"/>
      </calc:inputs>
      <calc:formula>A * B + C</calc:formula>
    </calc:calculation>
    """
    intercept[LoadingException] {
      getCalculation(testSnip)
    }
  }

  test("No inputs generates an error") {
    val testSnip = """
    <calc:calculation>
      <calc:inputs/>
      <calc:formula>A * B + C</calc:formula>
    </calc:calculation>
    """
    intercept[LoadingException] {
      getCalculation(testSnip)
    }
  }

  test("No formula generates an error") {
    val testSnip = """
    <calc:calculation>
      <calc:inputs>
          <calc:single pointName="Current" variable="A"/>
          <calc:single pointName="Voltage" variable="B"/>
      </calc:inputs>
    </calc:calculation>
    """
    intercept[LoadingException] {
      getCalculation(testSnip)
    }
  }

  def defaultCalc(extraSnip: String) = {
    """
    <calc:calculation>
    """ + extraSnip + """
      <calc:inputs>
          <calc:single pointName="Current" variable="A"/>
          <calc:single pointName="Voltage" variable="B"/>
      </calc:inputs>
      <calc:formula>A * B + C</calc:formula>
    </calc:calculation>
    """
  }
  def trigger(triggerSnip: String) = {
    val (c, _) = getCalculation(defaultCalc(triggerSnip))
    c.getTriggering
  }

  test("Default Triggering") {
    val t = trigger("")

    t.hasPeriodMs should equal(false)
    t.hasSchedule should equal(false)
    t.getUpdateAny should equal(true)
    t.getVariablesCount should equal(0)
  }

  test("Periodic Triggering") {
    val t = trigger("""<calc:triggering updateEveryPeriodMS="10000"/>""")

    t.getPeriodMs should equal(10000)
    t.hasSchedule should equal(false)
    t.hasUpdateAny should equal(false)
    t.getVariablesCount should equal(0)
  }

  test("Scheduled Triggering") {
    val t = trigger("""<calc:triggering schedule="* * * * *"/>""")

    t.hasPeriodMs should equal(false)
    t.getSchedule should equal("* * * * *")
    t.hasUpdateAny should equal(false)
    t.getVariablesCount should equal(0)
  }

  test("Trigger only when A changes") {
    val t = trigger("""
    <calc:triggering>
      <calc:variableTrigger variable="A" />
    </calc:triggering>
    """)

    t.hasPeriodMs should equal(false)
    t.hasSchedule should equal(false)
    t.hasUpdateAny should equal(false)
    t.getVariablesCount should equal(1)
    t.getVariables(0).getVariableName should equal("A")
    t.getVariables(0).hasDeadbandValue should equal(false)
    t.getVariables(0).getType should equal(FilteredMeas.FilterType.ANY_CHANGE)
  }

  test("Trigger when A changes, or B moves out of deadband") {
    val t = trigger("""
    <calc:triggering>
      <calc:variableTrigger variable="A" />
      <calc:variableTrigger variable="B" type="DEADBAND" deadband="5" />
    </calc:triggering>
    """)

    t.hasPeriodMs should equal(false)
    t.hasSchedule should equal(false)
    t.hasUpdateAny should equal(false)
    t.getVariablesCount should equal(2)
    t.getVariables(0).getVariableName should equal("A")
    t.getVariables(0).hasDeadbandValue should equal(false)
    t.getVariables(0).getType should equal(FilteredMeas.FilterType.ANY_CHANGE)
    t.getVariables(1).getVariableName should equal("B")
    t.getVariables(1).getDeadbandValue should equal(5.00)
    t.getVariables(1).getType should equal(FilteredMeas.FilterType.DEADBAND)
  }

  test("Trigger using unknown variable") {
    intercept[LoadingException] {
      trigger("""
    <calc:triggering>
      <calc:variableTrigger variable="C" />
    </calc:triggering>
    """)
    }
  }

  test("Multiple Trigger options") {
    intercept[LoadingException] {
      trigger("""
    <calc:triggering schedule="">
      <calc:variableTrigger variable="A" />
    </calc:triggering>
    """)
    }
  }

  test("Missing deadband setting") {
    intercept[LoadingException] {
      trigger("""
    <calc:triggering>
      <calc:variableTrigger variable="A" type="DEADBAND"/>
    </calc:triggering>
    """)
    }
  }

  test("Unknown variable triggering type") {
    val msg = intercept[LoadingException] {
      trigger("""
      <calc:triggering>
        <calc:variableTrigger variable="A" type="asdasd"/>
      </calc:triggering>
      """)
    }.getMessage
    msg should include("DEADBAND")
    msg should include("asdasd")
  }

  test("Missing DEADBAND value") {
    intercept[LoadingException] {
      trigger("""
    <calc:triggering>
      <calc:variableTrigger variable="A" type="DEADBAND"/>
    </calc:triggering>
    """)
    }
  }

  test("Bad InputStrategy string") {
    val testSnip = defaultCalc("""
      <calc:triggering inputQualityStrategy="sadsa" />
    """)
    intercept[LoadingException] {
      getCalculation(testSnip)
    }.getMessage should include("ONLY_WHEN_ALL_OK")
  }

  test("Default InputStrategy") {
    val testSnip = defaultCalc("")
    val (c, inputs) = getCalculation(testSnip)
    c.getTriggeringQuality.getStrategy should equal(InputQuality.Strategy.ONLY_WHEN_ALL_OK)
  }
  test("Custom InputStrategy") {
    val testSnip = defaultCalc("""
      <calc:triggering inputQualityStrategy="DONT_CALC_IF_ANY_BAD" />
    """)
    val (c, inputs) = getCalculation(testSnip)
    c.getTriggeringQuality.getStrategy should equal(InputQuality.Strategy.DONT_CALC_IF_ANY_BAD)
  }

  test("Output Quality and Time defaults") {
    val testSnip = defaultCalc("")
    val (c, inputs) = getCalculation(testSnip)
    c.getTimeOutput.getStrategy should equal(OutputTime.Strategy.MOST_RECENT)
    c.getQualityOutput.getStrategy should equal(OutputQuality.Strategy.WORST_QUALITY)
    c.getAccumulate should equal(false)
  }

  test("Custom Quality and Time settings") {
    val testSnip = defaultCalc("""
    <calc:output accumulate="true">
       <calc:outputTime strategy="AVERAGE_TIME" />
       <calc:outputQuality strategy="ALWAYS_OK" />
    </calc:output>
    """)
    val (c, inputs) = getCalculation(testSnip)
    c.getTimeOutput.getStrategy should equal(OutputTime.Strategy.AVERAGE_TIME)
    c.getQualityOutput.getStrategy should equal(OutputQuality.Strategy.ALWAYS_OK)
    c.getAccumulate should equal(true)
  }
}