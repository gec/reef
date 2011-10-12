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
package org.totalgrid.reef.measproc

import org.totalgrid.reef.proto.Model.{ Point, ReefUUID, Entity }
import org.totalgrid.reef.proto.Measurements.{ Quality, DetailQual, Measurement, MeasurementBatch }
import org.totalgrid.reef.proto.Processing.{ TriggerSet, MeasOverride }

object ProtoHelper {

  def makeBatch(m: Measurement): MeasurementBatch = makeBatch(m :: Nil)

  def makeBatch(ms: List[Measurement]): MeasurementBatch = {
    val mb = MeasurementBatch.newBuilder.setWallTime(0)
    ms.foreach(mb.addMeas(_))
    mb.build
  }

  def makeAnalog(name: String, value: Double, time: Long = System.currentTimeMillis, unit: String = "raw"): Measurement = {
    val m = Measurement.newBuilder
    m.setTime(time)
    m.setName(name)
    m.setType(Measurement.Type.DOUBLE)
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

  def makeString(name: String, value: String, time: Long = System.currentTimeMillis): Measurement = {
    Measurement.newBuilder
      .setTime(time)
      .setName(name)
      .setType(Measurement.Type.STRING)
      .setStringVal(value)
      .setQuality(makeNominalQuality)
      .build
  }

  def makeNominalQuality() = {
    Quality.newBuilder.setDetailQual(DetailQual.newBuilder).build
  }

  def makeAbnormalQuality() = {
    Quality.newBuilder.setDetailQual(DetailQual.newBuilder).setValidity(Quality.Validity.INVALID).build
  }

  def updateQuality(m: Measurement, q: Quality): Measurement = {
    m.toBuilder.setQuality(q).build
  }

  def makePoint(pointName: String): Point = {
    Point.newBuilder.setName(pointName).build
  }

  def makeNodeByUid(nodeUid: String): Entity = {
    Entity.newBuilder.setUuid(ReefUUID.newBuilder.setUuid(nodeUid)).build
  }

  def makeNodeByUid(nodeUid: ReefUUID): Entity = {
    Entity.newBuilder.setUuid(nodeUid).build
  }

  def makeNodeByName(name: String): Entity = {
    Entity.newBuilder.setName(name).build
  }

  def makePointByNodeUid(nodeUid: String): Point = {
    Point.newBuilder.setLogicalNode(makeNodeByUid(nodeUid)).build
  }
  def makePointByNodeUid(nodeUid: ReefUUID): Point = {
    Point.newBuilder.setLogicalNode(makeNodeByUid(nodeUid)).build
  }

  def makePointByNodeName(name: String): Point = {
    Point.newBuilder.setLogicalNode(makeNodeByName(name)).build
  }

  def makeNIS(name: String) = {
    MeasOverride.newBuilder.setPoint(Point.newBuilder.setName(name)).build
  }
  def makeOverride(name: String, value: Double, unit: String): MeasOverride = {
    MeasOverride.newBuilder
      .setPoint(Point.newBuilder.setName(name))
      .setMeas(Measurement.newBuilder
        .setTime(85)
        .setName(name)
        .setType(Measurement.Type.DOUBLE)
        .setDoubleVal(value)
        .setQuality(Quality.getDefaultInstance)
        .setUnit(unit))
      .build
  }
  def triggerSet = {
    TriggerSet.newBuilder
      .setPoint(Point.newBuilder.setName("meas01"))
      .addTriggers(triggerRlcLow("meas01"))
      .addTriggers(triggerTransformation("meas01"))
      .build
  }
  def triggerRlcLow(measName: String) = {
    import org.totalgrid.reef.proto.Processing._
    Trigger.newBuilder
      .setTriggerName("rlclow")
      .setStopProcessingWhen(ActivationType.HIGH)
      .setUnit("raw")
      .setAnalogLimit(AnalogLimit.newBuilder.setLowerLimit(0).setDeadband(5))
      .addActions(
        Action.newBuilder
          .setActionName("strip")
          .setType(ActivationType.HIGH)
          .setStripValue(true))
        .addActions(
          Action.newBuilder
            .setActionName("qual")
            .setType(ActivationType.HIGH)
            .setQualityAnnotation(Quality.newBuilder.setValidity(Quality.Validity.QUESTIONABLE)))
          .addActions(
            Action.newBuilder
              .setActionName("eventrise")
              .setType(ActivationType.RISING)
              .setEvent(EventGeneration.newBuilder.setEventType("event01")))
            .addActions(
              Action.newBuilder
                .setActionName("eventfall")
                .setType(ActivationType.FALLING)
                .setEvent(EventGeneration.newBuilder.setEventType("event02")))
              .build
  }
  def triggerTransformation(measName: String) = {
    import org.totalgrid.reef.proto.Processing._
    Trigger.newBuilder
      .setTriggerName("trans")
      .setUnit("raw")
      .addActions(
        Action.newBuilder
          .setActionName("linear")
          .setType(ActivationType.HIGH)
          .setLinearTransform(LinearTransform.newBuilder.setScale(10).setOffset(50000)))
        .addActions(
          Action.newBuilder
            .setActionName("unit")
            .setType(ActivationType.HIGH)
            .setSetUnit("V"))
          .build
  }
}
