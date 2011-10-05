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
package org.totalgrid.reef.japi.request.impl

import scala.collection.JavaConversions._

import org.totalgrid.reef.sapi.request.PointService
import org.totalgrid.reef.proto.Model.{ Entity, ReefUUID }
import org.totalgrid.reef.japi.request.builders.PointRequestBuilders

trait PointServiceImpl extends ReefServiceBaseClass with PointService {

  override def getAllPoints() = ops("Failed getting all points in system") {
    _.get(PointRequestBuilders.getAll).map { _.expectMany() }
  }

  override def getPointByName(name: String) = ops("Point not found with name: " + name) {
    _.get(PointRequestBuilders.getByName(name)).map { _.expectOne }
  }

  override def getPointByUid(uuid: ReefUUID) = ops("Point not found with uuid: " + uuid) {
    _.get(PointRequestBuilders.getByUid(uuid)).map { _.expectOne }
  }

  override def getPointsOwnedByEntity(parentEntity: Entity) = {
    ops("Couldn't find points owned by parent entity: " + parentEntity) {
      _.get(PointRequestBuilders.getOwnedByEntity(parentEntity)).map { _.expectMany() }
    }
  }

  override def getPointsBelongingToEndpoint(endpointUuid: ReefUUID) = {
    ops("Couldn't find points belong to endpoint: " + endpointUuid.getUuid) {
      _.get(PointRequestBuilders.getSourcedByEndpoint(endpointUuid)).map { _.expectMany() }
    }
  }
}