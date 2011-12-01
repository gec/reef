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

import org.totalgrid.reef.client.exception.ReefServiceException;
import org.totalgrid.reef.proto.Auth.Agent;
import org.totalgrid.reef.proto.Auth.Permission;
import org.totalgrid.reef.proto.Auth.PermissionSet;

import java.util.List;

/**
 * A service interface for managing and retrieving Agents. An Agent has a name,
 * password, and a set of permissions in the Reef system. An Agent can be a
 * real user that logs into the system or a software service that "owns" an
 * agent it uses to access to other services.
 *
 * Tag for api-enhancer, do not delete: !api-definition!
 */
public interface AgentService
{
    /**
     * @param name of agent to find
     * @return the agent requested or throws exception
     */
    Agent getAgentByName( String name ) throws ReefServiceException;

    /**
     * @return list of all agents
     */
    List<Agent> getAgents() throws ReefServiceException;

    /**
     * Creates (or overwrites) an agent and grants them access to the named PermissionSets
     *
     * @param name               agent name
     * @param password           password for agent, must obey systems password rules
     * @param permissionSetNames list of permissions sets we want to assign to the user
     * @return the newly created agent object
     */
    Agent createNewAgent( String name, String password, List<String> permissionSetNames ) throws ReefServiceException;

    /**
     * @param agent the agent to delete
     * @return the deleted agent
     */
    Agent deleteAgent( Agent agent ) throws ReefServiceException;

    /**
     * Updates the agent password
     *
     * @param agent       the agent to update
     * @param newPassword the new password, must obey systems password rules
     */
    Agent setAgentPassword( Agent agent, String newPassword ) throws ReefServiceException;

    /**
     * @return list of all of the possible permission sets
     */
    List<PermissionSet> getPermissionSets() throws ReefServiceException;

    /**
     * @param name of PermissionSet
     * @return the permissionset with matching name or an exception is thrown
     */
    PermissionSet getPermissionSet( String name ) throws ReefServiceException;

    /**
     * @param name        descriptive name for the PermissionSet
     * @param permissions list of allows and denies we want to create
     * @return the created PermissionSet or throws an exception
     */
    PermissionSet createPermissionSet( String name, List<Permission> permissions ) throws ReefServiceException;

    /**
     * @param permission the PermissionSet to delete
     * @return the deleted PermissionSet
     */
    PermissionSet deletePermissionSet( PermissionSet permission ) throws ReefServiceException;
}
