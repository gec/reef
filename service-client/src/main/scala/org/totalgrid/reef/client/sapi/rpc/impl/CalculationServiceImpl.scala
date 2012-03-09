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

import org.totalgrid.reef.client.sapi.client.rpc.framework.HasAnnotatedOperations
import org.totalgrid.reef.client.sapi.rpc.CalculationService
import org.totalgrid.reef.client.service.proto.Calculations.Calculation
import org.totalgrid.reef.client.service.proto.Model.{ Entity, Point, ReefUUID }
import org.totalgrid.reef.client.service.proto.Descriptors

import org.totalgrid.reef.client.service.proto.OptionalProtos._

trait CalculationServiceImpl extends HasAnnotatedOperations with CalculationService {

  def getCalculations() = ops.operation("Couldn't get calculations") {
    _.get(Calculation.newBuilder.setUuid(ReefUUID.newBuilder.setValue("*")).build).map(_.many)
  }

  def getCalculationByUuid(uuid: ReefUUID) = ops.operation("Couldn't get calculation with uuid: " + uuid.value) {
    _.get(Calculation.newBuilder.setUuid(uuid).build).map(_.one)
  }
  def getCalculationForPointByName(pointName: String) = ops.operation("Couldn't get calculation for point: " + pointName) {
    _.get(Calculation.newBuilder.setOutputPoint(Point.newBuilder.setName(pointName)).build).map(_.one)
  }

  def getCalculationForPointByUuid(uuid: ReefUUID) = ops.operation("Couldn't get calculation for point: " + uuid.value) {
    _.get(Calculation.newBuilder.setOutputPoint(Point.newBuilder.setUuid(uuid)).build).map(_.one)
  }

  def getCalculationsSourcedByEndpointByName(endpointName: String) = ops.operation("Couldn't get calculation for endpoint: " + endpointName) {
    _.get(Calculation.newBuilder.setOutputPoint(Point.newBuilder.setEndpoint(Entity.newBuilder.setName(endpointName))).build).map(_.many)
  }

  def getCalculationsSourcedByEndpointByUuid(uuid: ReefUUID) = ops.operation("Couldn't get calculation for endpoint: " + uuid.value) {
    _.get(Calculation.newBuilder.setOutputPoint(Point.newBuilder.setEndpoint(Entity.newBuilder.setUuid(uuid))).build).map(_.many)
  }

  def subscribeToCalculationsSourcedByEndpointByUuid(uuid: ReefUUID) = {
    ops.subscription(Descriptors.calculation, "Couldn't get calculation for endpoint: " + uuid.value) { (sub, client) =>
      client.get(Calculation.newBuilder.setOutputPoint(Point.newBuilder.setEndpoint(Entity.newBuilder.setUuid(uuid))).build, sub).map(_.many)
    }
  }

  def addCalculation(calculation: Calculation) = ops.operation("Couldn't create calculation for point: " + calculation.outputPoint.name) {
    _.put(calculation).map(_.one)
  }

  def deleteCalculation(uuid: ReefUUID) = ops.operation("Couldn't delete calculation: " + uuid.value) {
    _.delete(Calculation.newBuilder.setUuid(uuid).build).map(_.one)
  }

  def deleteCalculation(calculation: Calculation) = ops.operation("Couldn't delete calculation for point: " + calculation.outputPoint.name) {
    _.delete(calculation).map(_.one)
  }
}
