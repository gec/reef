package org.totalgrid.reef.api.sapi.client.rpc.impl

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
import org.totalgrid.reef.proto.Model.Point
import org.totalgrid.reef.api.japi.client.rpc.impl.builders.{ MeasurementHistoryRequestBuilders, MeasurementBatchRequestBuilders, MeasurementSnapshotRequestBuilders }
import org.totalgrid.reef.proto.Measurements.{ MeasurementBatch, MeasurementHistory, MeasurementSnapshot, Measurement }
import org.totalgrid.reef.api.sapi.client.rpc.MeasurementService
import org.totalgrid.reef.client.sapi.Descriptors
import org.totalgrid.reef.api.japi.client.Routable
import org.totalgrid.reef.api.japi.ExpectationException
import org.totalgrid.reef.api.sapi.client.rest.RestOperations
import org.totalgrid.reef.api.sapi.client.{ Subscription, BasicRequestHeaders }
import org.totalgrid.reef.api.sapi.client.rpc.framework.HasAnnotatedOperations

import scala.collection.JavaConversions._
import net.agileautomata.executor4s.{ Failure, Success, Future, Result }

trait MeasurementServiceImpl extends HasAnnotatedOperations with MeasurementService {

  override def getMeasurementByName(name: String) = {
    ops.operation("Couldn't get measurement with name: " + name) { client =>
      expectSingle(getMeasSnapshot(client, MeasurementSnapshotRequestBuilders.getByName(name)))
    }
  }
  override def getMeasurementByPoint(point: Point) = getMeasurementByName(point.getName)

  override def getMeasurementsByNames(names: List[String]) = {
    ops.operation("Couldn't get measurements with names: " + names) { session =>
      getMeasSnapshot(session, MeasurementSnapshotRequestBuilders.getByNames(names))
    }
  }
  override def getMeasurementsByPoints(points: List[Point]) = {
    ops.operation("Couldn't get measurements by points: " + points) { session =>
      getMeasSnapshot(session, MeasurementSnapshotRequestBuilders.getByPoints(points))
    }
  }

  override def subscribeToMeasurementsByNames(names: List[String]) = {
    ops.subscription(Descriptors.measurement, "Couldn't subscribe to measurements by names: " + names) { (sub, client) =>
      getMeasSnapshot(client, MeasurementSnapshotRequestBuilders.getByNames(names), sub)
    }
  }

  override def subscribeToMeasurementsByPoints(points: List[Point]) = {
    ops.subscription(Descriptors.measurement, "Couldn't subscribe to measurements by points: " + points) { (sub, client) =>
      getMeasSnapshot(client, MeasurementSnapshotRequestBuilders.getByPoints(points), sub)
    }
  }

  override def publishMeasurements(measurements: List[Measurement]) = {
    ops.operation("Couldn't publish measurements. size: " + measurements.size) {
      _.put(MeasurementBatchRequestBuilders.makeBatch(measurements)).map(_.one.map(_.getMeasList.toList))
    }
  }

  override def publishMeasurements(mBatch: MeasurementBatch, dest: Routable) = {
    ops.operation("Couldn't publish mearurement batch. size: " + mBatch.getMeasCount) {
      _.put(mBatch, BasicRequestHeaders.empty.setDestination(dest)).map(_.one)
    }
  }

  override def getMeasurementHistory(point: Point, limit: Int) = {
    ops.operation("Couldn't get measurement history for point: " + point + " limit: " + limit) {
      measHistoryList(_, MeasurementHistoryRequestBuilders.getByPoint(point, limit))
    }
  }

  override def getMeasurementHistory(point: Point, since: Long, limit: Int) = {
    ops.operation("Couldn't get measurement history for point: " + point + " since: " + since + " limit: " + limit) {
      measHistoryList(_, MeasurementHistoryRequestBuilders.getByPointSince(point, since, limit))
    }
  }

  override def getMeasurementHistory(point: Point, since: Long, before: Long, returnNewest: Boolean, limit: Int) = {
    ops.operation("Couldn't get measurement history for point: " + point + " between: " + since + " and: " + before + " limit: " + limit + " returnNewest: " + returnNewest) {
      measHistoryList(_, MeasurementHistoryRequestBuilders.getByPointBetween(point, since, before, returnNewest, limit))
    }
  }

  override def subscribeToMeasurementHistory(point: Point, limit: Int) = {
    ops.subscription(Descriptors.measurement, "Couldn't subscibe to measurement history for point: " + point + " limit: " + limit) { (sub, client) =>
      measHistoryList(client, MeasurementHistoryRequestBuilders.getByPoint(point, limit), sub)
    }
  }

  override def subscribeToMeasurementHistory(point: Point, since: Long, limit: Int) = {
    ops.subscription(Descriptors.measurement, "Couldn't subscibe to measurement history for point: " + point + " since: " + since + " limit: " + limit) { (sub, client) =>
      measHistoryList(client, MeasurementHistoryRequestBuilders.getByPointSince(point, since, limit), sub)
    }
  }

  private def expectSingle(f: Future[Result[List[Measurement]]]): Future[Result[Measurement]] = f.map {
    _.flatMap { list =>
      list match {
        case List(x) => Success(x)
        case x: List[_] => Failure(new ExpectationException("Expected a list of size 1, but got: " + x))
      }
    }
  }

  private def getMeasSnapshot(session: RestOperations, meas: MeasurementSnapshot): Future[Result[List[Measurement]]] = {
    session.get(meas).map(_.one.map(_.getMeasurementsList.toList))
  }
  private def getMeasSnapshot(session: RestOperations, meas: MeasurementSnapshot, sub: Subscription[Measurement]) = {
    session.get(meas, sub).map(_.one.map(_.getMeasurementsList.toList))
  }

  private def measHistoryList(session: RestOperations, request: MeasurementHistory): Future[Result[List[Measurement]]] = {
    session.get(request).map(_.one.map(_.getMeasurementsList.toList))
  }

  private def measHistoryList(session: RestOperations, request: MeasurementHistory, sub: Subscription[Measurement]) = {
    session.get(request, sub).map(_.one.map(_.getMeasurementsList.toList))
  }
}

