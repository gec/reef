/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Green Energy Corp licenses this file
 * to you under the GNU Affero General Public License Version 3.0
 * (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.measproc

import org.totalgrid.reef.proto.{ Measurements, Processing }
import org.totalgrid.reef.proto.FEP._
import org.totalgrid.reef.proto.Model._
import Measurements._
import Processing._

object ProtoHelper {

  def makeBatch(m: Measurement): MeasurementBatch = makeBatch(m :: Nil)

  def makeBatch(ms: List[Measurement]): MeasurementBatch = {
    val mb = MeasurementBatch.newBuilder.setWallTime(0)
    ms.foreach(mb.addMeas(_))
    mb.build
  }

  def makeAnalog(name: String, value: Double, time: Long = System.currentTimeMillis, unit: String = "raw"): Measurements.Measurement = {
    val m = Measurements.Measurement.newBuilder
    m.setTime(time)
    m.setName(name)
    m.setType(Measurements.Measurement.Type.DOUBLE)
    m.setDoubleVal(value)
    m.setQuality(makeNominalQuality)
    m.setUnit(unit)
    m.build
  }

  def makeInt(name: String, value: Long, time: Long = System.currentTimeMillis): Measurement = {
    Measurement.newBuilder
      .setTime(time)
      .setName(name)
      .setType(Measurement.Type.INT)
      .setIntVal(value)
      .setQuality(makeNominalQuality)
      .build

  }
  def makeBool(name: String, value: Boolean, time: Long = System.currentTimeMillis): Measurement = {
    Measurement.newBuilder
      .setTime(time)
      .setName(name)
      .setType(Measurement.Type.BOOL)
      .setBoolVal(value)
      .setQuality(makeNominalQuality)
      .build

  }

  def makeString(name: String, value: String, time: Long = System.currentTimeMillis): Measurements.Measurement = {
    Measurements.Measurement.newBuilder
      .setTime(time)
      .setName(name)
      .setType(Measurements.Measurement.Type.STRING)
      .setStringVal(value)
      .setQuality(makeNominalQuality)
      .build
  }

  def makeNominalQuality() = {
    Measurements.Quality.newBuilder.setDetailQual(Measurements.DetailQual.newBuilder).build
  }

  def makeAbnormalQuality() = {
    Measurements.Quality.newBuilder.setDetailQual(Measurements.DetailQual.newBuilder).setValidity(Measurements.Quality.Validity.INVALID).build
  }

  def updateQuality(m: Measurements.Measurement, q: Measurements.Quality): Measurements.Measurement = {
    m.toBuilder.setQuality(q).build
  }

  def makePoint(pointName: String): Point = {
    Point.newBuilder.setName(pointName).build
  }

  def makeNodeByUid(nodeUid: String): Entity = {
    Entity.newBuilder.setUid(nodeUid).build
  }

  def makeNodeByName(name: String): Entity = {
    Entity.newBuilder.setName(name).build
  }

  def makePointByNodeUid(nodeUid: String): Point = {
    Point.newBuilder.setLogicalNode(makeNodeByUid(nodeUid)).build
  }

  def makePointByNodeName(name: String): Point = {
    Point.newBuilder.setLogicalNode(makeNodeByName(name)).build
  }

}
