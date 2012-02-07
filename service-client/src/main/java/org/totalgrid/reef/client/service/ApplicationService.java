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
package org.totalgrid.reef.client.service;

import java.util.List;

import org.totalgrid.reef.client.service.proto.Model.ReefUUID;
import org.totalgrid.reef.client.settings.NodeSettings;
import org.totalgrid.reef.client.exception.ReefServiceException;
import org.totalgrid.reef.client.service.proto.Application.ApplicationConfig;
import org.totalgrid.reef.client.service.proto.ProcessStatus.StatusSnapshot;

/**
 * Tag for api-enhancer, do not delete: !api-definition!
 */
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
    ApplicationConfig registerApplication( NodeSettings nodeSettings, String instanceName, List<String> capabilities ) throws ReefServiceException;

    /**
     * Unregisters the application from the system, this shouldn't be called if we expect this application be
     * restarted again in the near future.
     *
     * @param appConfig registration object from registerApplication
     * @return The configuration we just deleted
     * @throws ReefServiceException
     */
    ApplicationConfig unregisterApplication( ApplicationConfig appConfig ) throws ReefServiceException;


    /**
     * Performs a heartbeat service call with the services.
     * @param statusSnapshot a preconstructed snapshot proto
     * @return proto used to indicate when the application will timeout
     * @deprecated use sendHeartbeat(appConfig) instead
     */
    StatusSnapshot sendHeartbeat( StatusSnapshot statusSnapshot ) throws ReefServiceException;

    /**
     * Performs a heartbeat service call with the services.
     * @param appConfig the configuration for the application
     * @return proto used to indicate when the application will timeout
     */
    StatusSnapshot sendHeartbeat( ApplicationConfig appConfig ) throws ReefServiceException;

    /**
     * When an application is shutting down, but expects to come back online later we will send an
     * "offline heartbeat" to the server. This will cause the services to reallocate any work that
     * application was doing to other nodes (if it does "coordinated work")
     */
    StatusSnapshot sendApplicationOffline( ApplicationConfig appConfig ) throws ReefServiceException;

    /**
     * Gets list of all currently registered applications
     * @throws ReefServiceException
     */
    List<ApplicationConfig> getApplications() throws ReefServiceException;

    /**
     * find a particular application by name
     * @param name name of the application
     * @return application proto or null if not found
     */
    ApplicationConfig findApplicationByName( String name ) throws ReefServiceException;

    /**
     * retrieve an application by name
     * @param name name of the application
     * @return application if found or exception will be thrown
     */
    ApplicationConfig getApplicationByName( String name ) throws ReefServiceException;

    /**
     * retrieve an application by uuid
     * @param uuid uuid of the application
     * @return application if found or exception will be thrown
     */
    ApplicationConfig getApplicationByUuid( ReefUUID uuid ) throws ReefServiceException;
}
