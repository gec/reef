/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the GNU Affero General Public License
 * Version 3.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.services.core

import org.totalgrid.reef.client.service.proto.Descriptors

import org.totalgrid.reef.client.service.proto.Measurements.MeasurementSnapshot

import scala.collection.JavaConversions._

import org.totalgrid.reef.measurementstore.RTDatabase
import org.totalgrid.reef.services.framework.SimpleServiceBehaviors.SimpleReadAndSubscribe
import org.totalgrid.reef.services.framework.{ RequestContext, ServiceEntryPoint }
import org.totalgrid.reef.client.exception.BadRequestException
import org.totalgrid.reef.models.{ UUIDConversions, Entity, EntityBasedModel }

class MeasurementSnapshotService(cm: RTDatabase)
    extends ServiceEntryPoint[MeasurementSnapshot]
    with SimpleReadAndSubscribe {

  private val entityModel = new EntityServiceModel()

  override val descriptor = Descriptors.measurementSnapshot

  override def doGetAndSubscribe(context: RequestContext, req: MeasurementSnapshot): MeasurementSnapshot = {

    val pointEntities = findPointEntities(context, req)
    val searchList = pointEntities.map { _.name }

    context.auth.authorize(context, Descriptors.measurement.id, "read", pointEntities.map { _.id })

    val b = MeasurementSnapshot.newBuilder()
    // clients shouldn't ask for 0 measurements but if they do we should just return a blank rather than an error.
    if (!pointEntities.isEmpty) {

      val measurements = cm.get(searchList)

      // subscribe before reading values
      subscribe(context, searchList.map(_.replace("*", "#")), req.getClass)

      searchList.foreach { name =>
        b.addPointNames(name)
        b.addMeasurements(measurements.get(name).get)
      }
    }

    b.build
  }

  private def findPointEntities(context: RequestContext, req: MeasurementSnapshot): List[Entity] = {

    if (req.getPointCount > 0 && req.getPointNamesCount > 0) {
      throw new BadRequestException("Can't specify both pointNames and point objects")
    }

    if (req.getPointCount > 0) {
      val requests = req.getPointList.toList

      // split up the point objects to remove all of the unique searches first
      val (includesUuid, others) = requests.partition(e => e.hasUuid && e.getUuid.getValue != "*")
      val (includesName, searches) = others.partition(e => e.hasName && e.getName != "*")

      val pointEntities = {
        val points = searches.flatMap { PointServiceConversion.findRecords(context, _) }
        EntityBasedModel.preloadEntities(points)
        points.map { _.entity.value }
      }

      val nameEntities = entityModel.findEntitiesByNames(context, includesName.map { _.getName }, "Point")
      import UUIDConversions._
      val uuidEntities = entityModel.findEntitiesByUuids(context, includesUuid.map { _.getUuid })

      nameEntities ::: uuidEntities ::: pointEntities
    } else {
      val measList = req.getPointNamesList().toList.distinct

      if (measList.size == 1 && measList.head == "*") {
        // subscribe to all measurements (even those who haven't been created yet)
        subscribe(context, List("#"), req.getClass)
        Nil
      } else {
        val foundEntities = entityModel.findEntitiesByNames(context, measList, "Point")
        if (foundEntities.size != measList.size) {
          val missing = measList.diff(foundEntities.map { _.name })
          if (!missing.isEmpty) {
            throw new BadRequestException("Couldn't find points: " + missing.mkString(", "))
          }
        }
        foundEntities
      }
    }
  }
}