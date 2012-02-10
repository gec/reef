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
package org.totalgrid.reef.httpbridge.servlets.apiproviders

import org.totalgrid.reef.client.exception.BadRequestException
import org.totalgrid.reef.client.sapi.rpc.AllScadaService
import org.totalgrid.reef.client.service.proto.Measurements._
import org.totalgrid.reef.client.service.proto.FEP._
import org.totalgrid.reef.client.service.proto.Model._
import org.totalgrid.reef.client.service.proto.Application.ApplicationConfig
import org.totalgrid.reef.httpbridge.servlets.helpers.ApiCallLibrary

/**
 * Implements a large chunk of the AllScadaService api calls whose requests are URI encodable.
 *
 * URI encodable means that all arguments are easily represented as a string, no objects.
 */
class AllScadaServiceApiCallLibrary extends ApiCallLibrary[AllScadaService] {

  override val serviceClass = classOf[AllScadaService]

  // MeasurementService
  optional("findMeasurementByName", classOf[Measurement], args => {
    val a1 = args.getString("pointName")
    (c) => c.findMeasurementByName(a1)
  })

  single("getMeasurementByName", classOf[Measurement], args => {
    val a1 = args.getString("pointName")
    (c) => c.getMeasurementByName(a1)
  })

  multi("getMeasurementsByNames", classOf[Measurement], args => {
    val a1 = args.getStrings("pointNames")
    (c) => c.getMeasurementsByNames(a1)
  })
  single("getMeasurementStatisticsByName", classOf[MeasurementStatistics], args => {
    val a1 = args.getString("pointName")
    (c) => c.getMeasurementStatisticsByName(a1)
  })

  /**
   * handling a function that is overloaded is much more difficult that the straightforward calls
   * because we have to do the job of a compiler and look to see which arguments are provided
   */
  multi("getMeasurementHistoryByName", classOf[Measurement], args => {
    val fromArgument = args.findLong("from")
    val toArgument = args.findLong("to")
    val newestArgument = args.findBoolean("returnNewest")
    val sinceArgument = args.findLong("since")
    val pointName = args.getString("pointName")
    val limit = args.getInt("limit")

    (c) => {
      if (fromArgument.isDefined && toArgument.isDefined && newestArgument.isDefined) {
        c.getMeasurementHistoryByName(pointName, fromArgument.get, toArgument.get, newestArgument.get, limit)
      } else if (sinceArgument.isDefined) {
        c.getMeasurementHistoryByName(pointName, sinceArgument.get, limit)
      } else if (!List(fromArgument, toArgument, newestArgument, sinceArgument).flatten.isEmpty) {
        throw new BadRequestException("Valid arguments are (pointName,from,to,returnNewest,limit) or (pointName,since,limit) or (pointName,limit)")
      } else {
        c.getMeasurementHistoryByName(pointName, limit)
      }
    }
  })

  // PointService
  multi("getPoints", classOf[Point], args => { (c) =>
    c.getPoints()
  })
  single("getPointByName", classOf[Point], args => {
    val a1 = args.getString("name")
    (c) => c.getPointByName(a1)
  })
  optional("findPointByName", classOf[Point], args => {
    val a1 = args.getString("name")
    (c) => c.findPointByName(a1)
  })
  single("getPointByUuid", classOf[Point], args => {
    val a1 = args.getUuid("uuid")
    (c) => c.getPointByUuid(a1)
  })
  multi("getPointsOwnedByEntity", classOf[Point], args => {
    val a1 = args.getUuid("entityUuid")
    (c) => c.getPointsOwnedByEntity(a1)
  })
  multi("getPointsBelongingToEndpoint", classOf[Point], args => {
    val a1 = args.getUuid("endpointUuid")
    (c) => c.getPointsBelongingToEndpoint(a1)
  })
  multi("getPointsThatFeedbackForCommand", classOf[Point], args => {
    val a1 = args.getUuid("commandUuid")
    (c) => c.getPointsThatFeedbackForCommand(a1)
  })

  // EndpointService
  multi("getEndpoints", classOf[Endpoint], args => { (c) =>
    c.getEndpoints()
  })
  single("getEndpointByName", classOf[Endpoint], args => {
    val a1 = args.getString("name")
    (c) => c.getEndpointByName(a1)
  })
  single("getEndpointByUuid", classOf[Endpoint], args => {
    val a1 = args.getUuid("endpointUuid")
    (c) => c.getEndpointByUuid(a1)
  })
  single("disableEndpointConnection", classOf[EndpointConnection], args => {
    val a1 = args.getUuid("endpointUuid")
    (c) => c.disableEndpointConnection(a1)
  })
  single("enableEndpointConnection", classOf[EndpointConnection], args => {
    val a1 = args.getUuid("endpointUuid")
    (c) => c.enableEndpointConnection(a1)
  })
  multi("getEndpointConnections", classOf[EndpointConnection], args => { (c) =>
    c.getEndpointConnections()
  })
  single("getEndpointConnectionByUuid", classOf[EndpointConnection], args => {
    val a1 = args.getUuid("endpointUuid")
    (c) => c.getEndpointConnectionByUuid(a1)
  })
  single("getEndpointConnectionByEndpointName", classOf[EndpointConnection], args => {
    val a1 = args.getString("endpointName")
    (c) => c.getEndpointConnectionByEndpointName(a1)
  })

  // ApplicationService
  multi("getApplications", classOf[ApplicationConfig], args => { (c) =>
    c.getApplications()
  })

  // EntityService
  multi("getEntities", classOf[Entity], args => { (c) =>
    c.getEntities()
  })
  single("getEntityByUuid", classOf[Entity], args => {
    val a1 = args.getUuid("uuid")
    (c) => c.getEntityByUuid(a1)
  })
  single("getEntityByName", classOf[Entity], args => {
    val a1 = args.getString("name")
    (c) => c.getEntityByName(a1)
  })
  optional("findEntityByName", classOf[Entity], args => {
    val a1 = args.getString("name")
    (c) => c.findEntityByName(a1)
  })
  multi("getEntitiesWithType", classOf[Entity], args => {
    val a1 = args.getString("typeName")
    (c) => c.getEntitiesWithType(a1)
  })
  multi("getEntitiesWithTypes", classOf[Entity], args => {
    val a1 = args.getStrings("types")
    (c) => c.getEntitiesWithTypes(a1)
  })
  multi("getEntityRelatedChildrenOfType", classOf[Entity], args => {
    val a1 = args.getUuid("parent")
    val a2 = args.getString("relationship")
    val a3 = args.getString("typeName")
    (c) => c.getEntityRelatedChildrenOfType(a1, a2, a3)
  })

  single("getEntityAttributes", classOf[EntityAttributes], args => {
    val a1 = args.getUuid("uuid")
    (c) => c.getEntityAttributes(a1)
  })

}