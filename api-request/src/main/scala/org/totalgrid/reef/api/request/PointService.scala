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
import org.totalgrid.reef.api.{ ISubscription, ReefServiceException }

/**
 * A Point represents a configured input point for data acquisition. Measurements associated with this
 * point all use the point name and id. Once obtaining a Point object you should use the MeasurementService
 * to read/subscribe to the measurements for that point.
 *
 * Every Point is associated with an Entity of type "Point". The point's location in the system
 * model is determined by this entity. Points are also associated with entities designated as
 * "logical nodes", which represent the communications interface/source.
 */
trait PointService {

  /**
   * get all points in the system
   * @return all points
   */
  def getAllPoints(): java.util.List[Point]

  /**
   * retrieve a point by name, throws exception if point is unknown
   * @param name of the Point we are retrieving
   * @return the point object with matching name
   */
  @throws(classOf[ReefServiceException])
  def getPointByName(name: String): Point

  /**
   * retrieve a point by uuid, throws exception if point is unknown
   * @param name of the Point we are retrieving
   * @return the point object with matching name
   */
  @throws(classOf[ReefServiceException])
  def getPointByUid(uuid: ReefUUID): Point

  /**
   * retrieve all points that are have the relationship "owns" to the parent entity
   * @param parentEntity parent we are looking for children of
   * @return the point object with matching name
   */
  @throws(classOf[ReefServiceException])
  def getPointsOwnedByEntity(parentEntity: Entity): java.util.List[Point]

  /**
   * @return all points that are currently marked as abnormal (last measurement had abnormal
   * flag set)
   */
  @throws(classOf[ReefServiceException])
  def getAbnormalPoints(): java.util.List[Point]

  /**
   *
   * @param sub a subscriber to recieve all future abnormal updates. Updates will all have event code
   * MODIFIED and an event will be recieved for every transition to or from the abnormal state.
   * @return all points that are currently marked as abnormal (last measurement had abnormal
   * flag set)
   */
  @throws(classOf[ReefServiceException])
  def getAbnormalPoints(sub: ISubscription[Point]): java.util.List[Point]

}