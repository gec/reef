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

import org.totalgrid.reef.proto.Model.Point
import org.totalgrid.reef.api.ExpectationException

import org.totalgrid.reef.proto.Measurements.{ Measurement }
import org.totalgrid.reef.api.request.MeasurementService
import org.totalgrid.reef.api.request.builders.{ MeasurementHistoryRequestBuilders, MeasurementBatchRequestBuilders, MeasurementSnapshotRequestBuilders }

import org.totalgrid.reef.proto.Descriptors

import scala.collection.JavaConversions._

trait MeasurementServiceImpl extends ReefServiceBaseClass with MeasurementService {

  override def getMeasurementByName(name: String): Measurement = {
    ops { session =>
      val measSnapshot = session.get(MeasurementSnapshotRequestBuilders.getByName(name)).await().expectOne
      val meas = checkAndReturnByNames(name :: Nil, measSnapshot.getMeasurementsList)
      meas.get(0)
    }
  }
  override def getMeasurementByPoint(point: Point): Measurement = getMeasurementByName(point.getName)

  override def getMeasurementsByNames(names: java.util.List[String]): java.util.List[Measurement] = {
    ops { session =>
      val measSnapshot = session.get(MeasurementSnapshotRequestBuilders.getByNames(names)).await().expectOne
      checkAndReturnByNames(names, measSnapshot.getMeasurementsList)
    }
  }
  override def getMeasurementsByPoints(points: java.util.List[Point]): java.util.List[Measurement] = {
    ops { session =>
      val measSnapshot = session.get(MeasurementSnapshotRequestBuilders.getByPoints(points)).await().expectOne
      checkAndReturn(points, measSnapshot.getMeasurementsList)
    }
  }

  override def subscribeToMeasurementsByNames(names: java.util.List[String]) = {
    ops { session =>
      useSubscription(session, Descriptors.measurementSnapshot.getKlass) { sub =>
        val measSnapshot = session.get(MeasurementSnapshotRequestBuilders.getByNames(names), sub).await().expectOne
        checkAndReturnByNames(names, measSnapshot.getMeasurementsList)
      }
    }
  }
  override def subscribeToMeasurementsByPoints(points: java.util.List[Point]) = {
    ops { session =>
      useSubscription(session, Descriptors.measurementSnapshot.getKlass) { sub =>
        val measSnapshot = session.get(MeasurementSnapshotRequestBuilders.getByPoints(points), sub).await().expectOne
        checkAndReturn(points, measSnapshot.getMeasurementsList)
      }
    }
  }

  override def publishMeasurements(measurements: java.util.List[Measurement]) {
    ops { _.put(MeasurementBatchRequestBuilders.makeBatch(measurements)).await().expectOne }
  }

  private def checkAndReturn(points: java.util.List[Point], retrievedMeas: java.util.List[Measurement]): java.util.List[Measurement] = {
    checkAndReturnByNames(points.map { _.getName }, retrievedMeas)
  }
  private def checkAndReturnByNames(names: java.util.List[String], retrievedMeas: java.util.List[Measurement]): java.util.List[Measurement] = {
    // TODO: measurement snapshot service should except on unknown point	 reef_techdebt-6
    if (names.length != retrievedMeas.length) {
      val retrievedNames = retrievedMeas.map { _.getName }
      val missing = names.diff(retrievedNames)
      throw new ExpectationException("Measurement service didn't have values for: " + missing.toList)
    }

    retrievedMeas
  }

  override def getMeasurementHistory(point: Point, limit: Int): java.util.List[Measurement] = {
    ops { _.get(MeasurementHistoryRequestBuilders.getByPoint(point, limit)).await().expectOne.getMeasurementsList }
  }

  override def getMeasurementHistory(point: Point, since: Long, limit: Int): java.util.List[Measurement] = {
    ops { _.get(MeasurementHistoryRequestBuilders.getByPointSince(point, since, limit)).await().expectOne.getMeasurementsList }
  }

  override def getMeasurementHistory(point: Point, since: Long, before: Long, returnNewest: Boolean, limit: Int): java.util.List[Measurement] = {
    ops { _.get(MeasurementHistoryRequestBuilders.getByPointBetween(point, since, before, returnNewest, limit)).await().expectOne.getMeasurementsList }
  }

  override def subscribeToMeasurementHistory(point: Point, limit: Int) = {
    ops { session =>
      useSubscription(session, Descriptors.measurementHistory.getKlass) { sub =>
        session.get(MeasurementHistoryRequestBuilders.getByPoint(point, limit), sub).await().expectOne.getMeasurementsList
      }
    }
  }

  override def subscribeToMeasurementHistory(point: Point, since: Long, limit: Int) = {
    ops { session =>
      useSubscription(session, Descriptors.measurementHistory.getKlass) { sub =>
        session.get(MeasurementHistoryRequestBuilders.getByPointSince(point, since, limit), sub).await().expectOne.getMeasurementsList
      }
    }
  }
}

