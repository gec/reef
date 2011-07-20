/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.protocol.dnp3

import org.totalgrid.reef.proto.{ Measurements, Commands, Mapping }

object DNPTranslator {

  // it's annoying to have to call swigValue to operate on the enum values
  // this implicit takes care of that
  private implicit def wrapSwigEnum(i: { def swigValue(): Int }) = i.swigValue

  //it's also annoying to have to test bitmasks for != 0 in if statements
  private implicit def convertIntToBool(i: Int) = (i != 0)

  /* Translation functions from DNP3 type to proto measurements */

  def translate(v: Binary, name: String, unit: String) = {
    translateCommon(v, name, unit, translateBinaryQual) { m =>
      m.setType(Measurements.Measurement.Type.BOOL)
      m.setBoolVal(v.GetValue)
    }
  }

  def translate(v: Analog, name: String, unit: String) = {
    translateCommon(v, name, unit, translateAnalogQual) { m =>
      m.setType(Measurements.Measurement.Type.DOUBLE)
      m.setDoubleVal(v.GetValue)
    }
  }

  def translate(v: Counter, name: String, unit: String) = {
    translateCommon(v, name, unit, translateCounterQual) { m =>
      m.setType(Measurements.Measurement.Type.INT)
      m.setIntVal(v.GetValue)
    }
  }

  def translate(v: ControlStatus, name: String, unit: String) = {
    translateCommon(v, name, unit, translateControlQual) { m =>
      m.setType(Measurements.Measurement.Type.BOOL)
      m.setBoolVal(v.GetValue)
    }
  }

  def translate(v: SetpointStatus, name: String, unit: String) = {
    translateCommon(v, name, unit, translateSetpointQual) { m =>
      m.setType(Measurements.Measurement.Type.DOUBLE)
      m.setDoubleVal(v.GetValue)
    }
  }

  def translate(rsp: CommandResponse, id: String) = {
    val status = rsp.getMResult match {
      case CommandStatus.CS_SUCCESS => Commands.CommandStatus.SUCCESS
      case CommandStatus.CS_TIMEOUT => Commands.CommandStatus.TIMEOUT
      case CommandStatus.CS_NO_SELECT => Commands.CommandStatus.NO_SELECT
      case CommandStatus.CS_FORMAT_ERROR => Commands.CommandStatus.FORMAT_ERROR
      case CommandStatus.CS_NOT_SUPPORTED => Commands.CommandStatus.NOT_SUPPORTED
      case CommandStatus.CS_ALREADY_ACTIVE => Commands.CommandStatus.ALREADY_ACTIVE
      case CommandStatus.CS_HARDWARE_ERROR => Commands.CommandStatus.HARDWARE_ERROR
      case CommandStatus.CS_LOCAL => Commands.CommandStatus.LOCAL
      case CommandStatus.CS_TOO_MANY_OPS => Commands.CommandStatus.TOO_MANY_OPS
      case CommandStatus.CS_NOT_AUTHORIZED => Commands.CommandStatus.NOT_AUTHORIZED
      case _ => Commands.CommandStatus.UNDEFINED
    }
    Commands.CommandResponse.newBuilder.setStatus(status).setCorrelationId(id).build
  }

  /* Translation functions from bus CommandRequests to DNP3 types */

  def translateBinaryOutput(c: Mapping.CommandMap) = {
    // TODO - Make the mapping types shorts
    new BinaryOutput(translate(c.getType), c.getCount.toShort, c.getOnTime, c.getOffTime)
  }

  def translateSetpoint(c: Commands.CommandRequest) = c.getType match {
    case Commands.CommandRequest.ValType.INT => new Setpoint(c.getIntVal)
    case Commands.CommandRequest.ValType.DOUBLE => new Setpoint(c.getDoubleVal)
    case _ => throw new Exception("wrong type for setpoint")
  }

  /* private helper functions */

  private def translate(c: Mapping.CommandType) = c match {
    case Mapping.CommandType.LATCH_ON => ControlCode.CC_LATCH_ON
    case Mapping.CommandType.LATCH_OFF => ControlCode.CC_LATCH_OFF
    case Mapping.CommandType.PULSE => ControlCode.CC_PULSE
    case Mapping.CommandType.PULSE_CLOSE => ControlCode.CC_PULSE_CLOSE
    case Mapping.CommandType.PULSE_TRIP => ControlCode.CC_PULSE_TRIP
    case _ => throw new Exception("Invalid Command code")
  }

  private def translateCommon(v: DataPoint, name: String, unit: String, q: Short => Measurements.Quality.Builder)(f: Measurements.Measurement.Builder => Unit) = {
    val m = Measurements.Measurement.newBuilder
      .setName(name)
      .setQuality(q(v.GetQuality)) // apply the specified quality conversion function
      .setUnit(unit)
    // we only set the time on the proto if the protocol gave us a valid time
    val t = v.GetTime
    if (t != 0) m.setTime(t)
    f(m) // apply the specified measurement building function
    m.build
  }

  private def translateBinaryQual(q: Short) = {
    translateQual(q, BinaryQuality.BQ_STATE) { dqual =>
      if (q & BinaryQuality.BQ_CHATTER_FILTER) dqual.setOscillatory(true)
    }
  }

  private def translateAnalogQual(q: Short) = {
    translateQual(q, 0) { dqual =>
      if (q & AnalogQuality.AQ_OVERRANGE) dqual.setOverflow(true)
      if (q & AnalogQuality.AQ_REFERENCE_CHECK) dqual.setBadReference(true)
    }
  }

  private def translateCounterQual(q: Short) = translateQuality(q, 0)
  private def translateSetpointQual(q: Short) = translateQuality(q, 0)
  private def translateControlQual(q: Short) = translateQuality(q, ControlQuality.TQ_STATE)

  private def translateQuality(q: Short, validBits: Int) = {
    val qual = getQual(q, validBits)
    val dqual = getDetailQual(q)
    buildQual(qual, dqual)
  }

  private def translateQual(q: Short, validBits: Int)(f: (Measurements.DetailQual.Builder) => Unit) = {
    val qual = getQual(q, validBits)
    val dqual = getDetailQual(q)
    f(dqual)
    buildQual(qual, dqual)
  }

  private def buildQual(q: Measurements.Quality.Builder, dq: Measurements.DetailQual.Builder) = {
    q.setDetailQual(dq)
    q
  }

  private def getDetailQual(q: Short) = {
    val dqual = Measurements.DetailQual.newBuilder
    val old = BinaryQuality.BQ_RESTART | BinaryQuality.BQ_RESTART
    if (q & old) dqual.setOldData(true)
    dqual
  }

  private def getQual(q: Short, validbits: Int) = {

    val substituted = BinaryQuality.BQ_REMOTE_FORCED_DATA | BinaryQuality.BQ_LOCAL_FORCED_DATA
    //ignore online, substituted bits when determined validity
    val valid = validbits | BinaryQuality.BQ_ONLINE | substituted

    val qual = Measurements.Quality.newBuilder
    if (q & ~valid) qual.setValidity(Measurements.Quality.Validity.INVALID)
    if (q & substituted) qual.setSource(Measurements.Quality.Source.SUBSTITUTED)
    qual
  }

}