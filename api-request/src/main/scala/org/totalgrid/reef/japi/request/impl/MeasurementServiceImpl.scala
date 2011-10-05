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

import org.totalgrid.reef.proto.Model.Point
import org.totalgrid.reef.japi.ExpectationException
import org.totalgrid.reef.sapi.request.MeasurementService
import org.totalgrid.reef.japi.request.builders.{ MeasurementHistoryRequestBuilders, MeasurementBatchRequestBuilders, MeasurementSnapshotRequestBuilders }

import org.totalgrid.reef.proto.Descriptors

import scala.collection.JavaConversions._
import org.totalgrid.reef.sapi.client.{ Subscription, RestOperations }
import org.totalgrid.reef.sapi.Destination
import org.totalgrid.reef.proto.Measurements.{ MeasurementBatch, MeasurementHistory, MeasurementSnapshot, Measurement }

trait MeasurementServiceImpl extends ReefServiceBaseClass with MeasurementService {

  override def getMeasurementByName(name: String) = {
    ops("Couldn't get measurement with name: " + name) { session =>
      getMeasSnapshot(session, MeasurementSnapshotRequestBuilders.getByName(name)).map { _.get(0) }
    }
  }
  override def getMeasurementByPoint(point: Point) = getMeasurementByName(point.getName)

  override def getMeasurementsByNames(names: List[String]) = {
    ops("Couldn't get measurements with names: " + names) { session =>
      getMeasSnapshot(session, MeasurementSnapshotRequestBuilders.getByNames(names))
    }
  }
  override def getMeasurementsByPoints(points: List[Point]) = {
    ops("Couldn't get measurements by points: " + points) { session =>
      getMeasSnapshot(session, MeasurementSnapshotRequestBuilders.getByPoints(points))
    }
  }

  override def subscribeToMeasurementsByNames(names: List[String]) = {
    ops("Couldn't subscribe to measurements by names: " + names) { session =>
      useSubscription(session, Descriptors.measurementSnapshot.getKlass) { sub =>
        getMeasSnapshot(session, MeasurementSnapshotRequestBuilders.getByNames(names), sub)
      }
    }
  }
  override def subscribeToMeasurementsByPoints(points: List[Point]) = {
    ops("Couldn't subscribe to measurements by points: " + points) { session =>
      useSubscription(session, Descriptors.measurementSnapshot.getKlass) { sub =>
        getMeasSnapshot(session, MeasurementSnapshotRequestBuilders.getByPoints(points), sub)
      }
    }
  }

  override def publishMeasurements(measurements: List[Measurement]) = {
    ops("Couldn't publish measurements. size: " + measurements.size) {
      _.put(MeasurementBatchRequestBuilders.makeBatch(measurements)).map { _.expectOne.getMeasList.toList }
    }
  }

  override def publishMeasurements(mBatch: MeasurementBatch, dest: Destination) = {
    ops("Couldn't publish mearurement batch. size: " + mBatch.getMeasCount) {
      _.put(mBatch, destination = dest).map { _.expectOne }
    }
  }

  override def getMeasurementHistory(point: Point, limit: Int) = {
    ops("Couldn't get measurement history for point: " + point + " limit: " + limit) {
      measHistoryList(_, MeasurementHistoryRequestBuilders.getByPoint(point, limit))
    }
  }

  override def getMeasurementHistory(point: Point, since: Long, limit: Int) = {
    ops("Couldn't get measurement history for point: " + point + " since: " + since + " limit: " + limit) {
      measHistoryList(_, MeasurementHistoryRequestBuilders.getByPointSince(point, since, limit))
    }
  }

  override def getMeasurementHistory(point: Point, since: Long, before: Long, returnNewest: Boolean, limit: Int) = {
    ops("Couldn't get measurement history for point: " + point + " between: " + since + " and: " + before + " limit: " + limit + " returnNewest: " + returnNewest) {
      measHistoryList(_, MeasurementHistoryRequestBuilders.getByPointBetween(point, since, before, returnNewest, limit))
    }
  }

  override def subscribeToMeasurementHistory(point: Point, limit: Int) = {
    ops("Couldn't subscibe to measurement history for point: " + point + " limit: " + limit) { session =>
      useSubscription(session, Descriptors.measurementHistory.getKlass) { sub =>
        measHistoryList(session, MeasurementHistoryRequestBuilders.getByPoint(point, limit), sub)
      }
    }
  }

  override def subscribeToMeasurementHistory(point: Point, since: Long, limit: Int) = {
    ops("Couldn't subscibe to measurement history for point: " + point + " since: " + since + " limit: " + limit) { session =>
      useSubscription(session, Descriptors.measurementHistory.getKlass) { sub =>
        measHistoryList(session, MeasurementHistoryRequestBuilders.getByPointSince(point, since, limit), sub)
      }
    }
  }

  private def getMeasSnapshot(session: RestOperations, meas: MeasurementSnapshot) = {
    session.get(meas).map { _.expectOne.getMeasurementsList.toList }
  }
  private def getMeasSnapshot(session: RestOperations, meas: MeasurementSnapshot, sub: Subscription[Measurement]) = {
    session.get(meas, sub).map { _.expectOne.getMeasurementsList.toList }
  }

  private def measHistoryList(session: RestOperations, request: MeasurementHistory) = {
    session.get(request).map { _.expectOne.getMeasurementsList.toList }
  }

  private def measHistoryList(session: RestOperations, request: MeasurementHistory, sub: Subscription[Measurement]) = {
    session.get(request, sub).map { _.expectOne.getMeasurementsList.toList }
  }
}

