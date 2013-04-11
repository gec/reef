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
package org.totalgrid.reef.measurementstore.encoders

import java.lang.{ Double => JDouble }

import scala.collection.JavaConversions._

import org.totalgrid.reef.client.service.proto.Measurements.{ Measurement => Meas, Quality, MeasArchive, MeasArchiveUnit }

class NullMeasEncoder extends MeasEncoder {

  def encode(meas: Seq[Meas]): Array[Byte] = {
    val ret = MeasArchive.newBuilder
    meas.foreach { m => ret.addMeas(getEncoding(m)) }
    ret.build.toByteArray
  }

  def decode(serialized: Array[Byte]): Seq[Meas] = {
    val archive = MeasArchive.parseFrom(serialized)
    archive.getMeasList.map { m => getDecoding(m) }
  }

  def getDecoding(m: MeasArchiveUnit): Meas = {
    val ret = Meas.newBuilder
    ret.setTime(m.getTime)
    ret.setQuality(m.getQuality)
    ret.setType(getType(m).get)
    ret.getType match {
      case Meas.Type.INT => ret.setIntVal(m.getIntVal)
      case Meas.Type.BOOL => ret.setBoolVal(m.getBoolVal)
      case Meas.Type.DOUBLE => ret.setDoubleVal(JDouble.longBitsToDouble(m.getDoubleVal))
      case Meas.Type.STRING => ret.setStringVal(m.getStringVal)
      case Meas.Type.NONE =>
    }
    ret.build
  }

  def getEncoding(m: Meas): MeasArchiveUnit = {
    val ret = MeasArchiveUnit.newBuilder.setTime(m.getTime)
    ret.setQuality(m.getQuality)
    m.getType match {
      case Meas.Type.INT => ret.setIntVal(m.getIntVal)
      case Meas.Type.BOOL => ret.setBoolVal(m.getBoolVal)
      case Meas.Type.DOUBLE => ret.setDoubleVal(JDouble.doubleToLongBits(m.getDoubleVal))
      case Meas.Type.STRING => ret.setStringVal(m.getStringVal)
      case Meas.Type.NONE =>
    }
    ret.build
  }

}