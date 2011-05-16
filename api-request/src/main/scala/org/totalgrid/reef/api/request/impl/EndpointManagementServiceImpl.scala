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
package org.totalgrid.reef.api.request.impl

import org.totalgrid.reef.api.request.EndpointManagementService

import org.totalgrid.reef.proto.Model.ReefUUID
import org.totalgrid.reef.proto.FEP.{ CommEndpointConfig, CommEndpointConnection }
import org.totalgrid.reef.proto.Descriptors

import scala.collection.JavaConversions._
import org.totalgrid.reef.api.Subscription.convertSubscriptionToRequestEnv

trait EndpointManagementServiceImpl extends ReefServiceBaseClass with EndpointManagementService {
  def getAllEndpoints() = {
    ops { _.getOrThrow(CommEndpointConfig.newBuilder.setUuid(ReefUUID.newBuilder.setUuid("*")).build) }
  }

  def getEndpointByName(name: String) = {
    ops { _.getOneOrThrow(CommEndpointConfig.newBuilder.setName(name).build) }
  }

  def getEndpoint(endpointUuid: ReefUUID) = {
    ops { _.getOneOrThrow(CommEndpointConfig.newBuilder.setUuid(endpointUuid).build) }
  }

  def disableEndpointConnection(endpointUuid: ReefUUID) = alterEndpointEnabled(endpointUuid, false)

  def enableEndpointConnection(endpointUuid: ReefUUID) = alterEndpointEnabled(endpointUuid, true)

  private def alterEndpointEnabled(endpointUuid: ReefUUID, enabled: Boolean) = {
    val connection = getEndpointConnection(endpointUuid)
    ops { _.putOneOrThrow(connection.toBuilder.setEnabled(enabled).build) }
  }

  def getAllEndpointConnections() = {
    ops { _.getOrThrow(CommEndpointConnection.newBuilder.setUid("*").build) }
  }

  def subscribeToAllEndpointConnections() = {
    ops { session =>
      useSubscription(session, Descriptors.commEndpointConnection.getKlass) { sub =>
        session.getOrThrow(CommEndpointConnection.newBuilder.setUid("*").build, sub)
      }
    }
  }

  def getEndpointConnection(endpointUuid: ReefUUID) = {
    ops { _.getOneOrThrow(CommEndpointConnection.newBuilder.setEndpoint(CommEndpointConfig.newBuilder.setUuid(endpointUuid)).build) }
  }

}