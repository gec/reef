/**
 * Copyright 2011 Green Energy Corp.
 *
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
package org.totalgrid.reef.api.request.impl

import org.totalgrid.reef.api.request.MeasurementOverrideService
import org.totalgrid.reef.proto.Model.Point
import org.totalgrid.reef.proto.Processing.MeasOverride
import org.totalgrid.reef.proto.Measurements.Measurement
import org.totalgrid.reef.api.ReefServiceException
import org.totalgrid.reef.api.request.builders.MeasurementOverrideRequestBuilders

trait MeasurementOverrideServiceImpl extends ReefServiceBaseClass with MeasurementOverrideService {

  def setPointOutOfService(point: Point): MeasOverride = ops {
    _.put(MeasurementOverrideRequestBuilders.makeNotInService(point)).await().expectOne
  }

  def setPointOverriden(point: Point, measurement: Measurement) = ops {
    _.put(MeasurementOverrideRequestBuilders.makeOverride(point, measurement)).await().expectOne
  }

  def deleteMeasurementOverride(measOverride: MeasOverride) = ops {
    _.delete(measOverride).await().expectOne
  }

  def clearMeasurementOverridesOnPoint(point: Point) = ops {
    _.delete(MeasurementOverrideRequestBuilders.getByPoint(point)).await().expectMany() match {
      case Nil => false
      case _ => true
    }
  }

}