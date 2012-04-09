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
import org.totalgrid.reef.client.service.proto.FEP;
import org.totalgrid.reef.protocol.api.impl.ProtocolEndpointResources;

/**
 * Provides an implementation of the ProtocolResources interface.
 */
public class ProtocolResourcesFactory
{
    private ProtocolResourcesFactory()
    {
    }

    /**
     * Provides an implementation of the ProtocolResources interface
     *
     * @param client Client logged-in with permissions for the protocol
     * @param endpointConnection Endpoint connection object for the protocol
     * @return
     */
    public static ProtocolResources buildResources( Client client, FEP.EndpointConnection endpointConnection )
    {
        return new ProtocolEndpointResources( client, endpointConnection );
    }
}
