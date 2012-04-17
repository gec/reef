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

import org.totalgrid.reef.client.service.proto.Model.{ ReefID, ReefUUID }

import org.totalgrid.reef.client.service.proto.OptionalProtos._

import net.agileautomata.executor4s.{ Failure, Success }
import org.totalgrid.reef.client.sapi.rpc.EndpointService
import org.totalgrid.reef.client.sapi.client.Promise
import org.totalgrid.reef.client.service.proto.Descriptors
import org.totalgrid.reef.client.sapi.client.rpc.framework.HasAnnotatedOperations
import org.totalgrid.reef.client.service.proto.FEP.{ FrontEndProcessor, Endpoint, EndpointConnection }
import org.totalgrid.reef.client.service.proto.Application.ApplicationConfig

trait EndpointServiceImpl extends HasAnnotatedOperations with EndpointService {

  override def getEndpoints() = ops.operation("Couldn't get list of all endpoints") {
    _.get(Endpoint.newBuilder.setUuid(ReefUUID.newBuilder.setValue("*")).build).map(_.many)
  }

  override def getEndpointByName(name: String) = ops.operation("Couldn't get endpoint with name: " + name) {
    _.get(Endpoint.newBuilder.setName(name).build).map(_.one)
  }

  override def getEndpointByUuid(endpointUuid: ReefUUID) = ops.operation("Couldn't get endpoint with uuid: " + endpointUuid.getValue) {
    _.get(Endpoint.newBuilder.setUuid(endpointUuid).build).map(_.one)
  }

  override def getEndpointsByNames(names: List[String]) = ops.operation("Couldn't get endpoints with names: " + names) { _ =>
    batchGets(names.map { Endpoint.newBuilder.setName(_).build })
  }

  override def getEndpointsByUuids(endpointUuids: List[ReefUUID]) = ops.operation("Couldn't get endpoint with uuids: " + endpointUuids.map { _.getValue }) { _ =>
    batchGets(endpointUuids.map { Endpoint.newBuilder.setUuid(_).build })
  }

  override def disableEndpointConnection(endpointUuid: ReefUUID) = alterEndpointEnabled(endpointUuid, false)

  override def enableEndpointConnection(endpointUuid: ReefUUID) = alterEndpointEnabled(endpointUuid, true)

  private def alterEndpointEnabled(endpointUuid: ReefUUID, enabled: Boolean): Promise[EndpointConnection] = {
    ops.operation("Couldn't alter endpoint: " + endpointUuid.getValue + " to enabled: " + enabled) { client =>
      val f1 = client.get(EndpointConnection.newBuilder.setEndpoint(Endpoint.newBuilder.setUuid(endpointUuid)).build).map(_.one)

      // this tricky little SOB creates another future based on the result of the last one, either by
      f1.flatMap { r =>
        r match {
          case Success(conn) => client.post(EndpointConnection.newBuilder.setId(conn.getId).setEnabled(enabled).build).map(_.one)
          case Failure(ex) => f1
        }
      }
    }
  }

  override def setEndpointAutoAssigned(endpointUuid: ReefUUID, autoAssigned: Boolean) = {
    ops.operation("Couldn't set endpoint: " + endpointUuid.getValue + " to autoAssigned: " + autoAssigned) {
      val endpoint = Endpoint.newBuilder.setUuid(endpointUuid).setAutoAssigned(autoAssigned)

      _.put(endpoint.build).map { _.one }
    }
  }

  override def setEndpointConnectionAssignedProtocolAdapter(endpointUuid: ReefUUID, applicationUuid: ReefUUID) = {
    ops.operation("Couldn't assign endpoint: " + endpointUuid.getValue + " to application: " + applicationUuid.getValue) {
      val app = ApplicationConfig.newBuilder.setUuid(applicationUuid)
      val fep = FrontEndProcessor.newBuilder.setAppConfig(app)
      val endpoint = Endpoint.newBuilder.setUuid(endpointUuid)

      _.put(EndpointConnection.newBuilder.setEndpoint(endpoint).setFrontEnd(fep).build).map { _.one }
    }
  }

  override def getProtocolAdapters() = {
    ops.operation("Couldn't get all protocol adapters.") {
      _.get(FrontEndProcessor.newBuilder.addProtocols("*").build).map { _.many }
    }
  }

  override def alterEndpointConnectionState(id: ReefID, state: EndpointConnection.State) = {
    ops.operation("Couldn't alter endpoint connection: " + id + " to : " + state) {
      _.post(EndpointConnection.newBuilder.setId(id).setState(state).build).map(_.one)
    }
  }

  override def getEndpointConnections() = ops.operation("Couldn't get list of all endpoint connections") {
    _.get(EndpointConnection.newBuilder.setId(ReefID.newBuilder.setValue("*")).build).map(_.many)
  }

  override def subscribeToEndpointConnections() = {
    ops.subscription(Descriptors.endpointConnection, "Couldn't subscribe to all endpoint connections") { (sub, client) =>
      client.get(EndpointConnection.newBuilder.setId(ReefID.newBuilder.setValue("*")).build, sub).map(_.many)
    }
  }

  override def getEndpointConnectionByUuid(endpointUuid: ReefUUID) = ops.operation("Couldn't get endpoint connection uuid: " + endpointUuid.getValue) {
    _.get(EndpointConnection.newBuilder.setEndpoint(Endpoint.newBuilder.setUuid(endpointUuid)).build).map(_.one)
  }

  override def getEndpointConnectionByEndpointName(endpointName: String) = ops.operation("Couldn't get endpoint connection uuid: " + endpointName) {
    _.get(EndpointConnection.newBuilder.setEndpoint(Endpoint.newBuilder.setName(endpointName)).build).map(_.one)
  }

}