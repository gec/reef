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

import org.totalgrid.reef.sapi.request.EndpointManagementService

import org.totalgrid.reef.proto.Model.ReefUUID
import org.totalgrid.reef.proto.Descriptors
import org.totalgrid.reef.proto.OptionalProtos._

import scala.collection.JavaConversions._
import org.totalgrid.reef.proto.FEP.{ CommChannel, CommEndpointConfig, CommEndpointConnection }

trait EndpointManagementServiceImpl extends ReefServiceBaseClass with EndpointManagementService {

  override def getAllEndpoints() = ops("Couldn't get list of all endpoints") {
    _.get(CommEndpointConfig.newBuilder.setUuid(ReefUUID.newBuilder.setUuid("*")).build).map { _.expectMany() }
  }

  override def getEndpointByName(name: String) = ops("Couldn't get endpoint with name: " + name) {
    _.get(CommEndpointConfig.newBuilder.setName(name).build).map { _.expectOne }
  }

  override def getEndpoint(endpointUuid: ReefUUID) = ops("Couldn't get endpoint with uuid: " + endpointUuid.uuid) {
    _.get(CommEndpointConfig.newBuilder.setUuid(endpointUuid).build).map { _.expectOne }
  }

  override def disableEndpointConnection(endpointUuid: ReefUUID) = alterEndpointEnabled(endpointUuid, false)

  override def enableEndpointConnection(endpointUuid: ReefUUID) = alterEndpointEnabled(endpointUuid, true)

  private def alterEndpointEnabled(endpointUuid: ReefUUID, enabled: Boolean) = {
    // TODO: fix getEndpointConnection futureness
    val connection = getEndpointConnection(endpointUuid).await()
    ops("Couldn't alter endpoint: " + endpointUuid.uuid + " to enabled: " + enabled) {
      _.put(connection.toBuilder.setEnabled(enabled).build).map { _.expectOne }
    }
  }

  override def getAllEndpointConnections() = ops("Couldn't get list of all endpoint connections") {
    _.get(CommEndpointConnection.newBuilder.setUid("*").build).map { _.expectMany() }
  }

  override def subscribeToAllEndpointConnections() = ops("Couldn't subscribe to all endpoint connections") { session =>
    useSubscription(session, Descriptors.commEndpointConnection.getKlass) { sub =>
      session.get(CommEndpointConnection.newBuilder.setUid("*").build, sub).map { _.expectMany() }
    }
  }

  override def getEndpointConnection(endpointUuid: ReefUUID) = ops("Couldn't get endpoint connection uuid: " + endpointUuid.uuid) {
    _.get(CommEndpointConnection.newBuilder.setEndpoint(CommEndpointConfig.newBuilder.setUuid(endpointUuid)).build).map { _.expectOne }
  }

  override def getAllCommunicationChannels = ops("Couldn't get list of all channels") {
    _.get(CommChannel.newBuilder().setName("*").build).map { _.expectMany() }
  }

  override def getCommunicationChannelByName(channelName: String) = ops("Couldn't get channel with name: " + channelName) {
    _.get(CommChannel.newBuilder().setName(channelName).build).map { _.expectOne }
  }

  override def getCommunicationChannel(channelUuid: ReefUUID) = ops("Couldn't get channel with uuid: " + channelUuid) {
    _.get(CommChannel.newBuilder().setUuid(channelUuid).build).map { _.expectOne }
  }

}