package org.totalgrid.reef.api.sapi.client.rpc.impl

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

import org.totalgrid.reef.api.proto.Model.Point
import org.totalgrid.reef.api.proto.Processing.MeasOverride
import org.totalgrid.reef.api.proto.Measurements.Measurement
import org.totalgrid.reef.api.sapi.impl.OptionalProtos._
import org.totalgrid.reef.api.japi.client.rpc.impl.builders.MeasurementOverrideRequestBuilders

import org.totalgrid.reef.api.sapi.client.rpc.MeasurementOverrideService
import org.totalgrid.reef.api.sapi.client.rpc.framework.HasAnnotatedOperations

trait MeasurementOverrideServiceImpl extends HasAnnotatedOperations with MeasurementOverrideService {

  override def setPointOutOfService(point: Point) = {
    ops.operation("Couldn't set point uuid:" + point.uuid + " name: " + point.name + " out of service") {
      _.put(MeasurementOverrideRequestBuilders.makeNotInService(point)).map(_.one)
    }
  }

  override def setPointOverride(point: Point, measurement: Measurement) = {
    ops.operation("Couldn't override point uuid:" + point.uuid + " name: " + point.name + " to: " + measurement) {
      _.put(MeasurementOverrideRequestBuilders.makeOverride(point, measurement)).map(_.one)
    }
  }

  override def deleteMeasurementOverride(measOverride: MeasOverride) = {
    // TODO: measurementOverride needs uid - backlog-63
    ops.operation("Couldn't delete measurementOverride: " + measOverride.meas + " on: " + measOverride.point) {
      _.delete(measOverride).map(_.one)
    }
  }

  // TODO: convert interface to return option
  override def clearMeasurementOverridesOnPoint(point: Point) = {
    ops.operation("Couldn't clear measurementOverrides on point uuid: " + point.uuid + " name: " + point.name) {
      _.delete(MeasurementOverrideRequestBuilders.getByPoint(point)).map {
        _.oneOrNone.map(_.orNull)
      }
    }
  }

}