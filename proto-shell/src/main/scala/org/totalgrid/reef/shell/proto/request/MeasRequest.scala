/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Green Energy Corp licenses this file
 * to you under the GNU Affero General Public License Version 3.0
 * (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.shell.proto.request

import org.totalgrid.reef.proto.Model.Point
import org.totalgrid.reef.protoapi.client.SyncServiceClient

import scala.collection.JavaConversions._
import RequestFailure._
import org.totalgrid.reef.proto.Measurements.{ MeasurementHistory, Measurement, MeasurementSnapshot }

object MeasRequest {

  def allMeasurements(client: SyncServiceClient) = {
    val measList = interpretAs("No measurements found.") {
      client.getOne(forPoints(PointRequest.getAllPoints(client))).getMeasurementsList.toList
    }

    if (measList.isEmpty) throw RequestFailure("No measurements found.")
    measList
  }

  def measByName(name: String, client: SyncServiceClient) = {
    val measSnap = interpretAs("Measurement not found.") {
      client.getOne(MeasRequest.forPointName(name))
    }
    val measList = measSnap.getMeasurementsList.toList
    if (measList.isEmpty) throw RequestFailure("Measurement not found.")
    measList.head
  }

  def measForEntity(parentId: String, client: SyncServiceClient) = {
    val points = client.get(PointRequest.underEntity(parentId))
    if (points.isEmpty) throw RequestFailure("No points found.")

    val measSnap = interpretAs("No measurements found.") {
      client.getOne(MeasRequest.forPoints(points))
    }

    val measList = measSnap.getMeasurementsList.toList
    if (measList.isEmpty) throw RequestFailure("No measurements found.")

    measList
  }

  def measHistory(name: String, limit: Int, client: SyncServiceClient) = {
    val req = MeasurementHistory.newBuilder.setPointName(name).setLimit(limit).setAscending(false).build
    val measList = interpretAs("Measurement not found.") {
      client.getOne(req).getMeasurementsList.toList
    }
    if (measList.isEmpty) throw RequestFailure("Measurement not found.")
    measList
  }

  def forPointName(name: String) = forPointNames(List(name))

  def forPoints(points: List[Point]) = {
    forPointNames(points.map(_.getName))
  }
  def forPointNames(names: List[String]) = {
    val req = MeasurementSnapshot.newBuilder
    names.foreach(p => req.addPointNames(p))
    req.build
  }

}