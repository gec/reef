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
package org.totalgrid.reef.protocol.api;

import org.totalgrid.reef.client.Client;
import org.totalgrid.reef.client.service.command.CommandRequestHandler;
import org.totalgrid.reef.client.service.proto.FEP;

/**
 * Represents an implementation of a protocol. Handed to the FEP, which uses callbacks to manage its
 * lifecycle.
 *
 * Protocols are distinguished from other client applications through their interaction with the
 * Endpoint system. The addEndpoint and removeEndpoint methods are callbacks the FEP uses to notify
 * protocol implementations that they need to initiate or terminate communications with that endpoint.
 * The EndpointConnection, Endpoint, and ConfigFile objects are all key to determining protocol behavior
 * (the latter two are available from EndpointConnection).
 *
 * The ProtocolResourcesFactory class can be used to acquire a ProtocolResources object, which contains
 * utility methods for performing protocol-related workflow.
 */
public interface ProtocolManager
{
    /**
     * Called when the FEP wants to add an communications endpoint of the protocol implementation.
     *
     * See the ProtocolResources/ProtocolResourcesFactory for performing protocol-related operations.
     *
     * @param client Reef client logged in with protocol's permissions.
     * @param endpointConnection Configuration object representing a "live" instance of an endpoint.
     * @return Callback provided to FEP, called when a command request is directed towards this endpoint.
     */
    CommandRequestHandler addEndpoint( Client client, FEP.EndpointConnection endpointConnection );

    /**
     * Called when FEP wants to remove a communications endpoint.
     *
     * @param endpointConnection Configuration object representing a "live" instance of an endpoint.
     */
    void removeEndpoint( FEP.EndpointConnection endpointConnection );
}
