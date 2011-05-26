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
package org.totalgrid.reef.api.request;

import org.totalgrid.reef.japi.ReefServiceException;
import org.totalgrid.reef.japi.client.SubscriptionResult;
import org.totalgrid.reef.proto.FEP.CommEndpointConfig;
import org.totalgrid.reef.proto.FEP.CommEndpointConnection;
import org.totalgrid.reef.proto.Model.ReefUUID;

import java.util.List;

/**
 * Communication Endpoints are the "field devices" that reef communicates with using legacy protocols
 * to acquire measurements from the field. Every point and command in the system is associated with
 * at most one endpoint at a time. CommEndpointConfig includes information about the protocol, associated
 * points, associated commands, communication channels, config files.
 * <p/>
 * For protocols that have reef front-end support there is an auxiliary service associated with Endpoints that
 * tracks which front-end each endpoint is assigned to. It also tracks the current state of the legacy protocol
 * connection which is how the protocol adapters tell reef if they are successfully communicating with the field
 * devices. We can also disable (and re-enable) the endpoint connection attempts, this is useful for devices that
 * can only talk with one "master" at a time so we can disable reefs protocol adapters temporarily to allow
 * another master to connect.
 */
public interface EndpointManagementService {

    /**
     * @return list of all endpoints in the system
     */
    List<CommEndpointConfig> getAllEndpoints() throws ReefServiceException;

    /**
     * @param name name of endpoint
     * @return the endpoint with that name or throws an exception
     */
    CommEndpointConfig getEndpointByName( String name ) throws ReefServiceException;

    /**
     * @param endpointUuid uuid of endpoint
     * @return the endpoint with that uuid or throws an exception
     */
    CommEndpointConfig getEndpoint( ReefUUID endpointUuid ) throws ReefServiceException;

    /**
     * disables automatic protocol adapter assignment and begins stopping any running protocol adapters.
     * service NOTE doesn't wait for protocol adapter to report a state change so don't assume state will have changed
     *
     * @param endpointUuid uuid of endpoint
     * @return the connection object representing the current connection state
     */
    CommEndpointConnection disableEndpointConnection( ReefUUID endpointUuid ) throws ReefServiceException;

    /**
     * enables any automatic protocol adapter assignment and begins starting any available protocol adapters.
     * service NOTE doesn't wait for protocol adapter to report a state change so don't assume state will have changed
     *
     * @param endpointUuid uuid of endpoint
     * @return the connection object representing the current connection state
     */
    CommEndpointConnection enableEndpointConnection( ReefUUID endpointUuid ) throws ReefServiceException;

    /**
     * get all of the objects representing endpoint to protocol adapter connections. Sub protos - Endpoint and frontend
     * will be filled in with name and uuid
     *
     * @return list of all endpoint connection objects
     */
    List<CommEndpointConnection> getAllEndpointConnections() throws ReefServiceException;

    /**
     * Same as getAllEndpointConnections but subscribes the user to all changes
     *
     * @return list of all endpoint connection objects
     * @see getAllEndpointConnections
     */
    SubscriptionResult<List<CommEndpointConnection>, CommEndpointConnection> subscribeToAllEndpointConnections() throws ReefServiceException;

    /**
     * Get current endpoint connection state for an endpoint
     *
     * @param endpointUuid uuid of endpoint
     * @return the connection object representing the current connection state
     */
    CommEndpointConnection getEndpointConnection( ReefUUID endpointUuid ) throws ReefServiceException;

}