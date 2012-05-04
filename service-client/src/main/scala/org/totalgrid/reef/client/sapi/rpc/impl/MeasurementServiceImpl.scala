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

import org.totalgrid.reef.client.service.proto.Model.{ ReefUUID, Point }
import org.totalgrid.reef.client.sapi.rpc.impl.builders.{ MeasurementHistoryRequestBuilders, MeasurementBatchRequestBuilders, MeasurementSnapshotRequestBuilders }
import org.totalgrid.reef.client.sapi.rpc.MeasurementService
import org.totalgrid.reef.client.service.proto.Descriptors

import org.totalgrid.reef.client.exception.ExpectationException

import org.totalgrid.reef.client.operations.scl.UsesServiceOperations
import org.totalgrid.reef.client.operations.scl.ScalaServiceOperations._

import scala.collection.JavaConversions._
import org.totalgrid.reef.client.sapi.client.BasicRequestHeaders
import org.totalgrid.reef.client.service.proto.Measurements._
import org.totalgrid.reef.client.operations.RestOperations
import org.totalgrid.reef.client.{ SubscriptionBinding, Promise, Routable }

trait MeasurementServiceImpl extends UsesServiceOperations with MeasurementService {

  override def getMeasurementByName(name: String) = {
    ops.operation("Couldn't get measurement with name: " + name) { client =>
      expectSingle(getMeasSnapshot(client, MeasurementSnapshotRequestBuilders.getByName(name)))
    }
  }
  override def getMeasurementByPoint(point: Point) = {
    ops.operation("Couldn't get measurement with point: " + point) { client =>
      expectSingle(getMeasSnapshot(client, MeasurementSnapshotRequestBuilders.getByPoint(point)))
    }
  }

  override def getMeasurementByUuid(pointUuid: ReefUUID) = {
    ops.operation("Couldn't get measurement with uuid: " + pointUuid.getValue) { client =>
      expectSingle(getMeasSnapshot(client, MeasurementSnapshotRequestBuilders.getByUuid(pointUuid)))
    }
  }

  override def findMeasurementByName(name: String) = {
    ops.operation("Couldn't find measurement with name: " + name) {
      _.get(MeasurementSnapshotRequestBuilders.getByName(name)).map(_.one).map(_.getMeasurementsList.toList.headOption)
    }
  }

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
  override def getMeasurementsByUuids(uuids: List[ReefUUID]) = {
    ops.operation("Couldn't get measurements by uuids: " + uuids) { session =>
      getMeasSnapshot(session, MeasurementSnapshotRequestBuilders.getByUuids(uuids))
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

  override def subscribeToMeasurementsByUuids(uuids: List[ReefUUID]) = {
    ops.subscription(Descriptors.measurement, "Couldn't subscribe to measurements by uuids: " + uuids) { (sub, client) =>
      getMeasSnapshot(client, MeasurementSnapshotRequestBuilders.getByUuids(uuids), sub)
    }
  }

  override def publishMeasurements(measurements: List[Measurement]) = {
    ops.operation("Couldn't publish measurements: " + measurements.map { _.getName }.distinct) {
      _.put(MeasurementBatchRequestBuilders.makeBatch(measurements)).map(_.one).map(a => true)
    }
  }

  override def publishMeasurements(measurements: List[Measurement], dest: Routable) = {
    ops.operation("Couldn't publish measurements: " + measurements.map { _.getName }.distinct + " dest: " + dest.getKey) {
      _.put(MeasurementBatchRequestBuilders.makeBatch(measurements), BasicRequestHeaders.empty.setDestination(dest)).map(_.one).map(a => true)
    }
  }

  override def publishMeasurements(mBatch: MeasurementBatch, dest: Routable) = {
    ops.operation("Couldn't publish measurement batch. size: " + mBatch.getMeasCount + " dest: " + dest.getKey) {
      _.put(mBatch, BasicRequestHeaders.empty.setDestination(dest)).map(_.one).map(a => true)
    }
  }

  override def getMeasurementHistory(point: Point, limit: Int) = {
    ops.operation("Couldn't get measurement history for point: " + point.getName + " limit: " + limit) {
      measHistoryList(_, MeasurementHistoryRequestBuilders.getByName(point.getName, limit))
    }
  }

  override def getMeasurementHistory(point: Point, since: Long, limit: Int) = {
    ops.operation("Couldn't get measurement history for point: " + point.getName + " since: " + since + " limit: " + limit) {
      measHistoryList(_, MeasurementHistoryRequestBuilders.getByNameSince(point.getName, since, limit))
    }
  }

  override def getMeasurementHistory(point: Point, since: Long, before: Long, returnNewest: Boolean, limit: Int) = {
    ops.operation("Couldn't get measurement history for point: " + point.getName + " between: " + since + " and: " + before + " limit: " + limit + " returnNewest: " + returnNewest) {
      measHistoryList(_, MeasurementHistoryRequestBuilders.getByNameBetween(point.getName, since, before, returnNewest, limit))
    }
  }

  override def getMeasurementHistoryByName(pointName: String, limit: Int) = {
    ops.operation("Couldn't get measurement history for point: " + pointName + " limit: " + limit) {
      measHistoryList(_, MeasurementHistoryRequestBuilders.getByName(pointName, limit))
    }
  }

  override def getMeasurementHistoryByName(pointName: String, since: Long, limit: Int) = {
    ops.operation("Couldn't get measurement history for point: " + pointName + " since: " + since + " limit: " + limit) {
      measHistoryList(_, MeasurementHistoryRequestBuilders.getByNameSince(pointName, since, limit))
    }
  }

  override def getMeasurementHistoryByName(pointName: String, since: Long, before: Long, returnNewest: Boolean, limit: Int) = {
    ops.operation("Couldn't get measurement history for point: " + pointName + " between: " + since + " and: " + before + " limit: " + limit + " returnNewest: " + returnNewest) {
      measHistoryList(_, MeasurementHistoryRequestBuilders.getByNameBetween(pointName, since, before, returnNewest, limit))
    }
  }

  override def getMeasurementHistoryByUuid(pointUuid: ReefUUID, limit: Int) = {
    ops.operation("Couldn't get measurement history for point: " + pointUuid.getValue + " limit: " + limit) {
      measHistoryList(_, MeasurementHistoryRequestBuilders.getByUuid(pointUuid, limit))
    }
  }

  override def getMeasurementHistoryByUuid(pointUuid: ReefUUID, since: Long, limit: Int) = {
    ops.operation("Couldn't get measurement history for point: " + pointUuid.getValue + " since: " + since + " limit: " + limit) {
      measHistoryList(_, MeasurementHistoryRequestBuilders.getByUuidSince(pointUuid, since, limit))
    }
  }

  override def getMeasurementHistoryByUuid(pointUuid: ReefUUID, since: Long, before: Long, returnNewest: Boolean, limit: Int) = {
    ops.operation("Couldn't get measurement history for point: " + pointUuid.getValue + " between: " + since + " and: " + before + " limit: " + limit + " returnNewest: " + returnNewest) {
      measHistoryList(_, MeasurementHistoryRequestBuilders.getByUuidBetween(pointUuid, since, before, returnNewest, limit))
    }
  }

  override def subscribeToMeasurementHistory(point: Point, limit: Int) = {
    ops.subscription(Descriptors.measurement, "Couldn't subscibe to measurement history for point: " + point.getName + " limit: " + limit) { (sub, client) =>
      measHistoryList(client, MeasurementHistoryRequestBuilders.getByName(point.getName, limit), sub)
    }
  }

  override def subscribeToMeasurementHistoryByName(pointName: String, limit: Int) = {
    ops.subscription(Descriptors.measurement, "Couldn't subscibe to measurement history for point: " + pointName + " limit: " + limit) { (sub, client) =>
      measHistoryList(client, MeasurementHistoryRequestBuilders.getByName(pointName, limit), sub)
    }
  }

  override def subscribeToMeasurementHistory(point: Point, since: Long, limit: Int) = {
    ops.subscription(Descriptors.measurement, "Couldn't subscibe to measurement history for point: " + point.getName + " since: " + since + " limit: " + limit) { (sub, client) =>
      measHistoryList(client, MeasurementHistoryRequestBuilders.getByNameSince(point.getName, since, limit), sub)
    }
  }

  override def subscribeToMeasurementHistoryByName(pointName: String, since: Long, limit: Int) = {
    ops.subscription(Descriptors.measurement, "Couldn't subscibe to measurement history for point: " + pointName + " since: " + since + " limit: " + limit) { (sub, client) =>
      measHistoryList(client, MeasurementHistoryRequestBuilders.getByNameSince(pointName, since, limit), sub)
    }
  }

  override def subscribeToMeasurementHistoryByUuid(pointUuid: ReefUUID, limit: Int) = {
    ops.subscription(Descriptors.measurement, "Couldn't subscibe to measurement history for point: " + pointUuid.getValue + " limit: " + limit) { (sub, client) =>
      measHistoryList(client, MeasurementHistoryRequestBuilders.getByUuid(pointUuid, limit), sub)
    }
  }

  override def subscribeToMeasurementHistoryByUuid(pointUuid: ReefUUID, since: Long, limit: Int) = {
    ops.subscription(Descriptors.measurement, "Couldn't subscibe to measurement history for point: " + pointUuid.getValue + " since: " + since + " limit: " + limit) { (sub, client) =>
      measHistoryList(client, MeasurementHistoryRequestBuilders.getByUuidSince(pointUuid, since, limit), sub)
    }
  }

  override def getMeasurementStatisticsByPoint(point: Point): Promise[MeasurementStatistics] = {
    ops.operation("Couldn't get measurement statistics for point: " + point) { c =>
      c.get(MeasurementStatistics.newBuilder.setPoint(point).build).map(_.one)
    }
  }

  override def getMeasurementStatisticsByName(name: String): Promise[MeasurementStatistics] = {
    ops.operation("Couldn't get measurement statistics for point name: " + name) { c =>
      c.get(MeasurementStatistics.newBuilder.setPoint(Point.newBuilder.setName(name)).build).map(_.one)
    }
  }

  override def getMeasurementStatisticsByUuid(uuid: ReefUUID): Promise[MeasurementStatistics] = {
    ops.operation("Couldn't get measurement statistics for point name: " + uuid.getValue) { c =>
      c.get(MeasurementStatistics.newBuilder.setPoint(Point.newBuilder.setUuid(uuid)).build).map(_.one)
    }
  }

  private def expectSingle(f: Promise[List[Measurement]]): Promise[Measurement] = {
    f.map {
      case List(x) => x
      case other => throw new ExpectationException("Expected a list of size 1, but got: " + other)
    }
  }
  /*f.map {
    _.flatMap { list =>
      list match {
        case List(x) => Success(x)
        case x: List[_] => Failure(new ExpectationException("Expected a list of size 1, but got: " + x))
      }
    }
  }*/

  private def getMeasSnapshot(session: RestOperations, meas: MeasurementSnapshot): Promise[List[Measurement]] = {
    session.get(meas).map(_.one).map(_.getMeasurementsList.toList)
  }
  private def getMeasSnapshot(session: RestOperations, meas: MeasurementSnapshot, sub: SubscriptionBinding) = {
    session.get(meas, sub).map(_.one).map(_.getMeasurementsList.toList)
  }

  private def measHistoryList(session: RestOperations, request: MeasurementHistory): Promise[List[Measurement]] = {
    session.get(request).map(_.one).map(_.getMeasurementsList.toList)
  }

  private def measHistoryList(session: RestOperations, request: MeasurementHistory, sub: SubscriptionBinding) = {
    session.get(request, sub).map(_.one).map(_.getMeasurementsList.toList)
  }

}

