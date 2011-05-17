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
package org.totalgrid.reef.api.request

import org.totalgrid.reef.proto.Model.ReefUUID
import org.totalgrid.reef.proto.FEP.{ CommEndpointConfig, CommEndpointConnection }
import org.totalgrid.reef.api.ReefServiceException
import org.totalgrid.reef.api.javaclient.ISubscriptionResult

/**
 * Communication Endpoints are the "field devices" that reef communicates with using legacy protocols
 * to acquire measurements from the field. Every point and command in the system is associated with
 * at most one endpoint at a time. CommEndpointConfig includes information about the protocol, associated
 * points, associated commands, communication channels, config files.
 *
 * For protocols that have reef front-end support there is an auxiliary service associated with Endpoints that
 * tracks which front-end each endpoint is assigned to. It also tracks the current state of the legacy protocol
 * connection which is how the protocol adapters tell reef if they are successfully communicating with the field
 * devices. We can also disable (and re-enable) the endpoint connection attempts, this is useful for devices that
 * can only talk with one "master" at a time so we can disable reefs protocol adapters temporarily to allow
 * another master to connect.
 *
 */
trait EndpointManagementService {

  /**
   * @return list of all endpoints in the system
   */
  @throws(classOf[ReefServiceException])
  def getAllEndpoints: java.util.List[CommEndpointConfig]

  /**
   * @param name name of endpoint
   * @return the endpoint with that name or throws an exception
   */
  @throws(classOf[ReefServiceException])
  def getEndpointByName(name: String): CommEndpointConfig

  /**
   * @param endpointUuid uuid of endpoint
   * @return the endpoint with that uuid or throws an exception
   */
  @throws(classOf[ReefServiceException])
  def getEndpoint(endpointUuid: ReefUUID): CommEndpointConfig

  /**
   * disables automatic protocol adapter assignment and begins stopping any running protocol adapters.
   * NOTE: service doesn't wait for protocol adapter to report a state change so don't assume state will have changed
   * @param endpointUuid uuid of endpoint
   * @return the connection object representing the current connection state
   */
  @throws(classOf[ReefServiceException])
  def disableEndpointConnection(endpointUuid: ReefUUID): CommEndpointConnection

  /**
   * enables any automatic protocol adapter assignment and begins starting any available protocol adapters.
   * NOTE: service doesn't wait for protocol adapter to report a state change so don't assume state will have changed
   * @param endpointUuid uuid of endpoint
   * @return the connection object representing the current connection state
   */
  @throws(classOf[ReefServiceException])
  def enableEndpointConnection(endpointUuid: ReefUUID): CommEndpointConnection

  /**
   * get all of the objects representing endpoint to protocol adapter connections. Sub protos - Endpoint and frontend
   * will be filled in with name and uuid
   * @return list of all endpoint connection objects
   */
  @throws(classOf[ReefServiceException])
  def getAllEndpointConnections(): java.util.List[CommEndpointConnection]

  /**
   * Same as getAllEndpointConnections but subscribes the user to all changes
   * @see getAllEndpointConnections
   * @return list of all endpoint connection objects
   */
  @throws(classOf[ReefServiceException])
  def subscribeToAllEndpointConnections(): ISubscriptionResult[java.util.List[CommEndpointConnection], CommEndpointConnection]

  /**
   * Get current endpoint connection state for an endpoint
   * @param endpointUuid uuid of endpoint
   * @return the connection object representing the current connection state
   */
  @throws(classOf[ReefServiceException])
  def getEndpointConnection(endpointUuid: ReefUUID): CommEndpointConnection

}