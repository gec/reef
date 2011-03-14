package org.totalgrid.reef.api.request.impl

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
import scala.collection.JavaConversions._
import org.totalgrid.reef.proto.Model.Point
import org.totalgrid.reef.api.{ ExpectationException, ISubscription }
import org.totalgrid.reef.api.ISubscription.convertISubToRequestEnv
import org.totalgrid.reef.proto.Measurements.{ Measurement }
import org.totalgrid.reef.api.request.MeasurementService
import org.totalgrid.reef.api.request.builders.{ MeasurementHistoryRequestBuilders, MeasurementBatchRequestBuilders, MeasurementSnapshotRequestBuilders }

trait MeasurementServiceImpl extends ReefServiceBaseClass with MeasurementService {

  def getMeasurementByName(name: String): Measurement = {
    val measSnapshot = ops.getOneOrThrow(MeasurementSnapshotRequestBuilders.getByName(name))
    val meas = checkAndReturnByNames(name :: Nil, measSnapshot.getMeasurementsList)
    meas.get(0)
  }
  def getMeasurementByPoint(point: Point): Measurement = getMeasurementByName(point.getName)

  def getMeasurementsByNames(names: java.util.List[String]): java.util.List[Measurement] = {
    val measSnapshot = ops.getOneOrThrow(MeasurementSnapshotRequestBuilders.getByNames(names))
    checkAndReturnByNames(names, measSnapshot.getMeasurementsList)
  }
  def getMeasurementsByPoints(points: java.util.List[Point]): java.util.List[Measurement] = {
    val measSnapshot = ops.getOneOrThrow(MeasurementSnapshotRequestBuilders.getByPoints(points))
    checkAndReturn(points, measSnapshot.getMeasurementsList)
  }

  def getMeasurementsByNames(names: java.util.List[String], subscription: ISubscription): java.util.List[Measurement] = {
    val measSnapshot = ops.getOneOrThrow(MeasurementSnapshotRequestBuilders.getByNames(names), subscription)
    checkAndReturnByNames(names, measSnapshot.getMeasurementsList)
  }
  def getMeasurementsByPoints(points: java.util.List[Point], subscription: ISubscription): java.util.List[Measurement] = {
    val measSnapshot = ops.getOneOrThrow(MeasurementSnapshotRequestBuilders.getByPoints(points), subscription)
    checkAndReturn(points, measSnapshot.getMeasurementsList)
  }

  def publishMeasurements(meases: java.util.List[Measurement]) {
    ops.postOne(MeasurementBatchRequestBuilders.makeBatch(meases))
  }

  private def checkAndReturn(points: java.util.List[Point], retrievedMeas: java.util.List[Measurement]): java.util.List[Measurement] = {
    checkAndReturnByNames(points.map { _.getName }, retrievedMeas)
  }
  private def checkAndReturnByNames(names: java.util.List[String], retrievedMeas: java.util.List[Measurement]): java.util.List[Measurement] = {
    // TODO: measurement snapshot service should except on unknown point
    if (names.length != retrievedMeas.length) {
      val retrievedNames = retrievedMeas.map { _.getName }
      val missing = names.diff(retrievedNames)
      throw new ExpectationException("Measurement service didn't have values for: " + missing.toList)
    }

    retrievedMeas
  }

  def getMeasurementHistory(point: Point, limit: Int): java.util.List[Measurement] = {
    val history = ops.getOneOrThrow(MeasurementHistoryRequestBuilders.getByPoint(point, limit))
    // TODO: default history query should give us the most recent measurements
    history.getMeasurementsList.reverse
  }

  def getMeasurementHistory(point: Point, since: Long, limit: Int): java.util.List[Measurement] = {
    val history = ops.getOneOrThrow(MeasurementHistoryRequestBuilders.getByPointSince(point, since, limit))
    history.getMeasurementsList.reverse
  }

  def getMeasurementHistory(point: Point, since: Long, before: Long, limit: Int): java.util.List[Measurement] = {
    val history = ops.getOneOrThrow(MeasurementHistoryRequestBuilders.getByPointBetween(point, since, before, limit))
    history.getMeasurementsList.reverse
  }

  def getMeasurementHistory(point: Point, limit: Int, sub: ISubscription): java.util.List[Measurement] = {
    val history = ops.getOneOrThrow(MeasurementHistoryRequestBuilders.getByPoint(point, limit), sub)
    history.getMeasurementsList.reverse
  }

  def getMeasurementHistory(point: Point, since: Long, limit: Int, sub: ISubscription) = {
    val history = ops.getOneOrThrow(MeasurementHistoryRequestBuilders.getByPointSince(point, since, limit), sub)
    history.getMeasurementsList.reverse
  }
}

