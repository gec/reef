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
package org.totalgrid.reef.client.sapi.rpc.impl

import org.totalgrid.reef.client.service.proto.Processing.MeasOverride
import org.totalgrid.reef.client.service.proto.Measurements.Measurement
import org.totalgrid.reef.client.service.proto.OptionalProtos._
import org.totalgrid.reef.client.sapi.rpc.impl.builders.MeasurementOverrideRequestBuilders._

import org.totalgrid.reef.client.sapi.rpc.MeasurementOverrideService
import org.totalgrid.reef.client.sapi.client.rpc.framework.HasAnnotatedOperations
import org.totalgrid.reef.client.service.proto.Model.{ ReefID, ReefUUID, Point }

trait MeasurementOverrideServiceImpl extends HasAnnotatedOperations with MeasurementOverrideService {

  override def setPointOutOfService(point: Point) = {
    ops.operation("Couldn't set point uuid:" + point.uuid + " name: " + point.name + " out of service") {
      _.put(makeNotInService(point)).map(_.one)
    }
  }

  override def setPointOverride(point: Point, measurement: Measurement) = {
    ops.operation("Couldn't override point uuid:" + point.uuid + " name: " + point.name + " to: " + measurement) {
      _.put(makeOverride(point, measurement)).map(_.one)
    }
  }

  override def setPointOutOfServiceByUuid(pointUuid: ReefUUID) = {
    ops.operation("Couldn't set point uuid:" + pointUuid + " out of service") {
      _.put(makeNotInService(makePoint(pointUuid))).map(_.one)
    }
  }

  override def setPointOverrideByUuid(pointUuid: ReefUUID, measurement: Measurement) = {
    ops.operation("Couldn't override point uuid:" + pointUuid + " to: " + measurement) {
      _.put(makeOverride(makePoint(pointUuid), measurement)).map(_.one)
    }
  }

  override def getMeasurementOverrides() = {
    ops.operation("Couldn't get all overrides") {
      _.get(getById(ReefID.newBuilder.setValue("*").build)).map(_.many)
    }
  }

  override def deleteMeasurementOverride(measOverride: MeasOverride) = {
    ops.operation("Couldn't delete measurementOverride: " + measOverride.meas + " on: " + measOverride.point) {
      _.delete(measOverride).map(_.one)
    }
  }

  override def deleteMeasurementOverrideById(id: ReefID) = {
    ops.operation("Couldn't delete measurementOverride: " + id.value) {
      _.delete(getById(id)).map(_.one)
    }
  }

  override def clearMeasurementOverridesOnPoint(point: Point) = {
    ops.operation("Couldn't clear measurementOverrides on point uuid: " + point.uuid + " name: " + point.name) {
      _.delete(getByPoint(point)).map { _.oneOrNone }
    }
  }

}