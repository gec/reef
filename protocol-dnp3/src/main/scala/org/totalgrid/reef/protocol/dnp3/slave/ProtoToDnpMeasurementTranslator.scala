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
package org.totalgrid.reef.api.protocol.dnp3.slave

import org.totalgrid.reef.api.proto.Mapping.{ DataType, MeasMap }
import org.totalgrid.reef.api.protocol.dnp3._
import org.totalgrid.reef.api.proto.Measurements.{ Quality, Measurement }
import org.totalgrid.reef.api.proto.Measurements.Quality.Validity

object ProtoToDnpMeasurementTranslator {

  def getAnalog(m: Measurement) = {
    val a = (m.hasDoubleVal, m.hasIntVal) match {
      case (true, false) => makeAnalog(m.getDoubleVal, translateQuality(m))
      case (false, true) => makeAnalog(m.getIntVal, translateQuality(m))
      case _ => makeAnalog(0, badQuality)
    }
    a.SetTime(m.getTime)
    a
  }
  def getSetpointStatus(m: Measurement) = {
    val a = (m.hasDoubleVal, m.hasIntVal) match {
      case (true, false) => makeSetpointStatus(m.getDoubleVal, translateQuality(m))
      case (false, true) => makeSetpointStatus(m.getIntVal, translateQuality(m))
      case _ => makeSetpointStatus(0, badQuality)
    }
    a.SetTime(m.getTime)
    a
  }
  def getCounter(m: Measurement) = {
    val a = if (m.hasIntVal) makeCounter(m.getIntVal, translateQuality(m))
    else makeCounter(0, badQuality)
    a.SetTime(m.getTime)
    a
  }
  def getBinary(m: Measurement) = {
    val a = if (m.hasBoolVal) makeBinary(m.getBoolVal, translateQuality(m))
    else makeBinary(false, badQuality)
    a.SetTime(m.getTime)
    a
  }
  def getControlStatus(m: Measurement) = {
    val a = if (m.hasBoolVal) makeControlStatus(m.getBoolVal, translateQuality(m))
    else makeControlStatus(false, badQuality)
    a.SetTime(m.getTime)
    a
  }

  private def makeBinary(value: Boolean, quality: Short) = {
    val b = new Binary()
    b.SetQuality(quality)
    b.SetValue(value)
    b
  }
  private def makeAnalog(value: Long, quality: Short): Analog = {
    makeAnalog(value.toDouble, quality)
  }
  private def makeAnalog(value: Double, quality: Short): Analog = {
    val b = new Analog()
    b.SetQuality(quality)
    b.SetValue(value)
    b
  }
  private def makeCounter(value: Long, quality: Short) = {
    val b = new Counter()
    b.SetQuality(quality)
    b.SetValue(value)
    b
  }
  private def makeControlStatus(value: Boolean, quality: Short) = {
    val b = new ControlStatus()
    b.SetQuality(quality)
    b.SetValue(value)
    b
  }
  private def makeSetpointStatus(value: Double, quality: Short) = {
    val b = new SetpointStatus()
    b.SetQuality(quality)
    b.SetValue(value)
    b
  }

  private def translateQuality(m: Measurement): Short = translateQuality(m.getQuality)

  private def badQuality: Short = {
    AnalogQuality.AQ_COMM_LOST.swigValue().toShort
  }

  private def translateQuality(quality: Quality): Short = {
    val qual = quality.getValidity match {
      case Validity.GOOD => AnalogQuality.AQ_ONLINE
      case Validity.INVALID => AnalogQuality.AQ_COMM_LOST
      case Validity.QUESTIONABLE => AnalogQuality.AQ_COMM_LOST
    }
    qual.swigValue().toShort
  }
}