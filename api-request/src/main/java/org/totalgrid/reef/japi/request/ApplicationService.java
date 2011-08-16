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
package org.totalgrid.reef.japi.request;

import org.totalgrid.reef.japi.ReefServiceException;
import org.totalgrid.reef.japi.client.NodeSettings;
import org.totalgrid.reef.proto.Application.ApplicationConfig;
import org.totalgrid.reef.proto.ProcessStatus.StatusSnapshot;

import java.util.List;

public interface ApplicationService
{
    /**
     * Register an application with capabilites on this node
     * @param nodeSettings description of "where" software is running (computer name and network)
     * @param instanceName name of this application, used to determine if a registering application is new or
     *                     is a restart of an existing application. IMPORTANT: should use node name, using the same
     *                     instance name for multiple versions of an application running on different nodes will cause
     *                     undefined behavior in many cases.
     * @param capabilities What "capabilities" an application offers, these capabilities define a contract with the
     *                     server on what work this application will be trying to perform.
     * @return The configuration from the server, includes heartbeat settings
     * @throws ReefServiceException
     */
    public ApplicationConfig registerApplication( NodeSettings nodeSettings, String instanceName, List<String> capabilities )
        throws ReefServiceException;

    /**
     * Performs a heartbeat service call with the services.
     * @param ss
     * @return
     * @throws ReefServiceException
     */
    public StatusSnapshot sendHeartbeat( StatusSnapshot ss ) throws ReefServiceException;

}
