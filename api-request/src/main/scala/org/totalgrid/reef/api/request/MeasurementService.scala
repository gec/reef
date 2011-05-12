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

import org.totalgrid.reef.proto.Measurements.Measurement
import org.totalgrid.reef.proto.Model.Point
import org.totalgrid.reef.api.ReefServiceException
import org.totalgrid.reef.api.javaclient.ISubscriptionResult

/**
 * Non-exhaustive API for using the reef Measurement services. This API allows the client code to read current measurement
 * values for many points at a time, read historical values for a single measuremnt at a time or publish measurements in
 * batches. For current and historical value functions you can also pass in an ISubscription object which will receive
 * all future measurement changes for those points. Asking for unknown points/ measurements will result in an exception
 */
trait MeasurementService {

  /**
   * gets the most recent measurement for a point
   */
  @throws(classOf[ReefServiceException])
  def getMeasurementByPoint(point: Point): Measurement

  /**
   * gets the current value for a point (specified by name)
   */
  @throws(classOf[ReefServiceException])
  def getMeasurementByName(name: String): Measurement

  /**
   * gets the most recent measurement for a set of points. If any points are unknown the call will throw a bad request
   * exception.
   */
  @throws(classOf[ReefServiceException])
  def getMeasurementsByNames(names: java.util.List[String]): java.util.List[Measurement]

  /**
   * gets the most recent measurement for a set of points (specified by names). If any points are unknown the
   * call will throw a bad request exception.
   */
  @throws(classOf[ReefServiceException])
  def getMeasurementsByPoints(points: java.util.List[Point]): java.util.List[Measurement]

  /**
   * gets the most recent measurement for a set of points and configure a subscription to receive updates for every
   * measurement change
   */
  @throws(classOf[ReefServiceException])
  def subscribeToMeasurementsByPoints(points: java.util.List[Point]): ISubscriptionResult[java.util.List[Measurement], Measurement]

  /**
   * gets the most recent measurement for a set of points and configure a subscription to receive updates for every
   * measurement change
   */
  @throws(classOf[ReefServiceException])
  def subscribeToMeasurementsByNames(points: java.util.List[String]): ISubscriptionResult[java.util.List[Measurement], Measurement]

  /**
   * get a subset of the recent measurements for a point
   * @param limit - max number of measurements returned
   */
  @throws(classOf[ReefServiceException])
  def getMeasurementHistory(point: Point, limit: Int): java.util.List[Measurement]

  /**
   * get a subset of the recent measurements for a point
   * @param since - dont return measurements older than this, inclusive (millis)
   * @param limit - max number of measurements returned
   */
  @throws(classOf[ReefServiceException])
  def getMeasurementHistory(point: Point, since: Long, limit: Int): java.util.List[Measurement]

  /**
   * get a subset of the recent measurements for a point. Note that setting the endTime and subscribing is incompatible
   * and will result in an exception since its dangerous to ask for a fixed time period and also be getting new
   * measurements since this can lead to missed measurements.
   * @param since - don't return measurements older than this, inclusive (millis)
   * @param before - don't return measurements newer than this, inclusive (millis)
   * @param returnNewest - if there are more measurements in the range the range than limit return the newest measurements
   * @param limit - max number of measurements returned
   */
  @throws(classOf[ReefServiceException])
  def getMeasurementHistory(point: Point, since: Long, before: Long, returnNewest: Boolean, limit: Int): java.util.List[Measurement]

  /**
   * get the most recent measurements for a point and setup a subscription for new measurements
   * @param limit - max number of measurements returned
   */
  @throws(classOf[ReefServiceException])
  def subscribeToMeasurementHistory(point: Point, limit: Int): ISubscriptionResult[java.util.List[Measurement], Measurement]

  /**
   * get the most recent measurements for a point and setup a subscription for new measurements
   * @param since - don't return measurements older than this, inclusive (millis)
   * @param limit - max number of measurements returned
   */
  @throws(classOf[ReefServiceException])
  def subscribeToMeasurementHistory(point: Point, since: Long, limit: Int): ISubscriptionResult[java.util.List[Measurement], Measurement]

  /**
   * publish a batch of measurements as if the client was a protocol adapter. Can fail for many reasons and most clients
   * should not use this function. If any point is not publishable the whole group should fail.
   * Preconditions for success:
   *   - the points listed in the measurements all need to exist
   *   - the points must be configured to use an appropriate protocol (benchmark or manual) to maintain message stream
   *     TODO: implement protocol checking on publishMeasurements
   *   - measurement processors must be available to process the measurement (only question during startup)
   */
  @throws(classOf[ReefServiceException])
  def publishMeasurements(measurements: java.util.List[Measurement])
}
