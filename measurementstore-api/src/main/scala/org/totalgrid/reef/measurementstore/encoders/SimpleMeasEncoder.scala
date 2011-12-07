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
package org.totalgrid.reef.measurementstore.encoders

import java.lang.{ Double => JDouble }

import scala.collection.JavaConversions._
import org.totalgrid.reef.client.service.proto.Measurements.{ Measurement => Meas, Quality, MeasArchive, MeasArchiveUnit }

object SimpleMeasEncoder {

  case class DblEnc(mant: Long, exp: Int, neg: Boolean)
  case class Last(time: Long, quality: Quality, intVal: Long, dblVal: Long)

  // encode against t=0, default quality, 0 values
  val start = Last(0, Quality.newBuilder.build, 0, JDouble.doubleToLongBits(0.0))
}

class SimpleMeasEncoder extends MeasEncoder {

  import SimpleMeasEncoder._

  case class EncInfo[A](meas: A, last: Last)

  def encode(meas: Seq[Meas]): Array[Byte] = {

    val ret = MeasArchive.newBuilder
    meas.foldLeft(start) { (last, current) =>
      val next = getEncoding(EncInfo(current, last))
      ret.addMeas(next.meas)
      next.last
    }
    ret.build.toByteArray
  }

  def decode(serialized: Array[Byte]): Seq[Meas] = {
    val archive = MeasArchive.parseFrom(serialized)
    var last = start
    archive.getMeasList.map { m =>
      val ret = getDecoding(EncInfo(m, last))
      last = ret.last
      ret.meas
    }
  }

  def getDecoding(ei: EncInfo[MeasArchiveUnit]): EncInfo[Meas] = {

    // values for next iteration
    val time = ei.last.time + ei.meas.getTime
    val quality = if (ei.meas.hasQuality) ei.meas.getQuality else ei.last.quality
    val intVal = if (ei.meas.hasIntVal) ei.last.intVal + ei.meas.getIntVal else ei.last.intVal
    val dblVal = if (ei.meas.hasDoubleVal) ei.last.dblVal + ei.meas.getDoubleVal else ei.last.dblVal

    val ret = Meas.newBuilder.setTime(time).setQuality(quality)
    ret.setType(getType(ei.meas).get) // blow up here if it isn't set

    ret.getType match {
      case Meas.Type.INT => ret.setIntVal(intVal)
      case Meas.Type.BOOL => ret.setBoolVal(ei.meas.getBoolVal)
      case Meas.Type.DOUBLE => ret.setDoubleVal(JDouble.longBitsToDouble(dblVal))
      case Meas.Type.STRING => ret.setStringVal(ei.meas.getStringVal)
    }
    EncInfo(ret.build, Last(time, quality, intVal, dblVal))
  }

  def getEncoding(ei: EncInfo[Meas]): EncInfo[MeasArchiveUnit] = {

    val ret = MeasArchiveUnit.newBuilder.setTime(ei.meas.getTime - ei.last.time)

    // only encode the quality if it has changed
    if (ei.meas.getQuality != ei.last.quality) ret.setQuality(ei.meas.getQuality)

    val intVal = if (ei.meas.hasIntVal) ei.meas.getIntVal else ei.last.intVal
    val dblVal = if (ei.meas.hasDoubleVal) JDouble.doubleToLongBits(ei.meas.getDoubleVal) else ei.last.dblVal

    ei.meas.getType match {
      // integers can take advantage of variable length encoding
      case Meas.Type.INT => ret.setIntVal(intVal - ei.last.intVal)
      case Meas.Type.BOOL => ret.setBoolVal(ei.meas.getBoolVal)
      case Meas.Type.DOUBLE => ret.setDoubleVal(dblVal - ei.last.dblVal)
      case Meas.Type.STRING => ret.setStringVal(ei.meas.getStringVal)
    }

    EncInfo(ret.build, Last(ei.meas.getTime, ei.meas.getQuality, intVal, dblVal))
  }

}