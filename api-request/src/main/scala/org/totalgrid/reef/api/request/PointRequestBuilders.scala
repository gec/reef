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
package org.totalgrid.reef.api.request

import org.totalgrid.reef.proto.Model.{ Entity, Point }

object PointRequestBuilders {
  def getAll = Point.newBuilder.setUid("*").build

  def getByUid(uid: String): Point = Point.newBuilder.setUid(uid).build
  def getByUid(point: Point): Point = getByUid(point.getUid)

  def getByName(name: String) = Point.newBuilder.setName(name).build

  def getByEntity(entity: Entity) = Point.newBuilder.setEntity(entity).build

  def getOwnedByEntity(entity: Entity) = {
    Point.newBuilder.setEntity(entity).build
  }
  def getOwnedByEntityWithName(name: String) = {
    getOwnedByEntity(EntityRequestBuilders.getOwnedChildrenOfTypeFromRootName(name, "Point"))
  }
}