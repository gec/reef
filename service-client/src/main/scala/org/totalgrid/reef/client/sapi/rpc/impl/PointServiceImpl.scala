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

import scala.collection.JavaConversions._

import org.totalgrid.reef.client.sapi.rpc.PointService
import org.totalgrid.reef.client.service.proto.Model.{ Entity, ReefUUID, Point }
import org.totalgrid.reef.client.sapi.rpc.impl.builders._
import org.totalgrid.reef.client.operations.scl.UsesServiceOperations
import org.totalgrid.reef.client.operations.scl.ScalaServiceOperations._

trait PointServiceImpl extends UsesServiceOperations with PointService {

  override def getPoints() = ops.operation("Failed getting all points in system") {
    _.get(PointRequestBuilders.getAll).map(_.many)
  }

  override def getPointByName(name: String) = ops.operation("Point not found with name: " + name) {
    _.get(PointRequestBuilders.getByName(name)).map(_.one)
  }

  override def findPointByName(name: String) = ops.operation("Point not found with name: " + name) {
    _.get(PointRequestBuilders.getByName(name)).map(_.oneOrNone)
  }

  override def getPointByUuid(uuid: ReefUUID) = ops.operation("Point not found with uuid: " + uuid) {
    _.get(PointRequestBuilders.getById(uuid)).map(_.one)
  }

  override def getPointsByNames(names: List[String]) = batchGets("Points not found with names: " + names) {
    names.map { PointRequestBuilders.getByName(_) }
  }

  override def getPointsByUuids(uuids: List[ReefUUID]) = batchGets("Points not found with uuids: " + uuids) {
    uuids.map { PointRequestBuilders.getById(_) }
  }

  override def getPointsOwnedByEntity(parentEntity: Entity) = {
    ops.operation("Couldn't find points owned by parent entity: " + parentEntity) {
      _.get(PointRequestBuilders.getOwnedByEntityWithUuid(parentEntity.getUuid)).map(_.many)
    }
  }

  override def getPointsOwnedByEntity(parentUuid: ReefUUID) = {
    ops.operation("Couldn't find points owned by parent entity: " + parentUuid.getValue) {
      _.get(PointRequestBuilders.getOwnedByEntityWithUuid(parentUuid)).map(_.many)
    }
  }

  override def getPointsBelongingToEndpoint(endpointUuid: ReefUUID) = {
    ops.operation("Couldn't find points belong to endpoint: " + endpointUuid.getValue) {
      _.get(PointRequestBuilders.getSourcedByEndpoint(endpointUuid)).map(_.many)
    }
  }

  override def getPointsThatFeedbackForCommand(commandUuid: ReefUUID) = {
    ops.operation("Couldn't find points that are feedback to endpoint: " + commandUuid.getValue) { session =>

      val request = Point.newBuilder.setEntity(EntityRequestBuilders.getCommandsFeedbackPoints(commandUuid)).build

      session.get(request).map(_.many)
    }
  }
}

