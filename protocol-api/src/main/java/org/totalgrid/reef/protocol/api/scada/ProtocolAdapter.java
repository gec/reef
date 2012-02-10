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
package org.totalgrid.reef.protocol.api.scada;


import org.totalgrid.reef.client.service.proto.FEP;
import org.totalgrid.reef.client.service.proto.Model;
import org.totalgrid.reef.protocol.api.CommandHandler;

import java.util.List;

/**
 * Defines a simple protocol interface for use with basic protocol adapters
 */
public interface ProtocolAdapter
{

    /**
     * @return Unique name, i.e. 'dnp3'
     */
    String name();

    /**
     *
     * @param endpoint Unique name of the endpoint
     * @param channel Configuration object that identifies and describes the communication channel
     * @param config Set of configuration files associated with the Endpoint
     * @param resources Feedback publishers for updating measurements and state of the system
     * @return   A command handler object
     */
    CommandHandler addEndpoint( String endpoint, FEP.CommChannel channel, List<Model.ConfigFile> config, Resources resources );

    /**
     *
     * @param endpoint
     */
    void removeEndpoint( String endpoint );

}
