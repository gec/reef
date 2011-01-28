/**
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  Green Energy Corp licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.protocol.dnp3

import org.totalgrid.reef.proto.{ Measurements, Commands, Mapping }

import org.scalatest.Suite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class DNPTranslatorTests extends Suite with ShouldMatchers {

  def testAnything[T, A](obj: DataPoint, value: A, protoType: Measurements.Measurement.Type,
    setVal: (A) => Unit,
    trans: (T, String) => Measurements.Measurement,
    protoVal: (Measurements.Measurement) => A) {
    setVal(value)
    obj.SetTime(35)
    val proto = trans(obj.asInstanceOf[T], "obj1")
    proto.getType() should equal(protoType)
    protoVal(proto) should equal(value)
    proto.getTime() should equal(35)
    proto.getName() should equal("obj1")
  }

  def testBinary() {
    val obj = new Binary()
    testAnything[Binary, Boolean](obj, true, Measurements.Measurement.Type.BOOL,
      obj.SetValue, DNPTranslator.translate, (m: Measurements.Measurement) => m.getBoolVal())
  }
  def testAnalog() {
    val obj = new Analog()
    testAnything[Analog, Double](obj, 35, Measurements.Measurement.Type.DOUBLE,
      obj.SetValue, DNPTranslator.translate, (m: Measurements.Measurement) => m.getDoubleVal())
  }
  def testCounter() {
    val obj = new Counter()
    testAnything[Counter, Long](obj, 35, Measurements.Measurement.Type.INT,
      obj.SetValue, DNPTranslator.translate, (m: Measurements.Measurement) => m.getIntVal())
  }
  def testCommandStatus() {
    val obj = new ControlStatus()
    testAnything[ControlStatus, Boolean](obj, true, Measurements.Measurement.Type.BOOL,
      obj.SetValue, DNPTranslator.translate, (m: Measurements.Measurement) => m.getBoolVal())
  }
  def testSetpointStatus() {
    val obj = new SetpointStatus()
    testAnything[SetpointStatus, Double](obj, 35, Measurements.Measurement.Type.DOUBLE,
      obj.SetValue, DNPTranslator.translate, (m: Measurements.Measurement) => m.getDoubleVal())
  }

  // Test the translation of DNP command responses to proto equivalents
  def testCommandResponses() {
    // Build a map of DNP -> Proto
    val map = Map(CommandStatus.CS_SUCCESS -> Commands.CommandStatus.SUCCESS,
      CommandStatus.CS_TIMEOUT -> Commands.CommandStatus.TIMEOUT,
      CommandStatus.CS_NO_SELECT -> Commands.CommandStatus.NO_SELECT,
      CommandStatus.CS_FORMAT_ERROR -> Commands.CommandStatus.FORMAT_ERROR,
      CommandStatus.CS_NOT_SUPPORTED -> Commands.CommandStatus.NOT_SUPPORTED,
      CommandStatus.CS_ALREADY_ACTIVE -> Commands.CommandStatus.ALREADY_ACTIVE,
      CommandStatus.CS_HARDWARE_ERROR -> Commands.CommandStatus.HARDWARE_ERROR,
      CommandStatus.CS_LOCAL -> Commands.CommandStatus.LOCAL,
      CommandStatus.CS_TOO_MANY_OPS -> Commands.CommandStatus.TOO_MANY_OPS,
      CommandStatus.CS_NOT_AUTHORIZED -> Commands.CommandStatus.NOT_AUTHORIZED)

    // Test each conversion's translate call
    map.foreach(kvp => {
      val resp = new CommandResponse(kvp._1) // DNP class
      val proto = DNPTranslator.translate(resp, "resp1") // translation call, arbitrary name
      proto.getCorrelationId should equal("resp1") // arbitrary name
      proto.getStatus should equal(kvp._2) // proto class
    })
  }

  // Test the two types of commands, Commands and setpoints, from proto types to DNP codes
  def testCommandRequest() {
    // Build a map of conversions from DNP Command typs to proto
    val map = Map(
      Mapping.CommandType.LATCH_ON -> ControlCode.CC_LATCH_ON,
      Mapping.CommandType.LATCH_OFF -> ControlCode.CC_LATCH_OFF,
      Mapping.CommandType.PULSE -> ControlCode.CC_PULSE,
      Mapping.CommandType.PULSE_CLOSE -> ControlCode.CC_PULSE_CLOSE,
      Mapping.CommandType.PUSLE_TRIP -> ControlCode.CC_PULSE_TRIP)

    // Test translation for each Command code
    map.foreach(kvp => {
      val protoCmd = Mapping.CommandMap.newBuilder // proto object
        .setType(kvp._1) // proto command type
        .setCount(3) // arbitrary values
        .setOnTime(500)
        .setOffTime(600)
        .setIndex(5)
        .setCommandName("cmd1")

      // Translate and verify values input above
      val dnpCmd = DNPTranslator.translateBinaryOutput(protoCmd.build)
      dnpCmd.GetCode() should equal(kvp._2)
      dnpCmd.getMCount() should equal(3)
      dnpCmd.getMOnTimeMS() should equal(500)
      dnpCmd.getMOffTimeMS() should equal(600)
    })
  }
  def testSetpointRequest() {
    val protoCmd = Commands.CommandRequest.newBuilder
      .setType(Commands.CommandRequest.ValType.INT)
      .setIntVal(500)
      .setName("sp1")
      .setCorrelationId("spID1")
    val dnpCmd = DNPTranslator.translateSetpoint(protoCmd.build)
    dnpCmd.GetValue() should equal(500)
  }

}
