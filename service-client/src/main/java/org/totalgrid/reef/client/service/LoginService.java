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
import org.totalgrid.reef.client.service.proto.Auth.AuthToken;
import org.totalgrid.reef.client.service.proto.Model.ReefID;

import java.util.List;

/**
 * This service provides functions for inspecting which agents are logged in or examining the history of who
 * logged in when. This information is obviously sensitive and unless operating as a "high-level" user most
 * users will only see their own logins.
 *
 * NOTE: the "token" field on the AuthToken object will never be filled by requests to this service. That token
 * is only returned to the user _once_ during login and set in the headers on every request.
 *
 * Tag for api-enhancer, do not delete: !api-definition!
 */
public interface LoginService
{
    /**
     * Get list of logins for the agent making the request.
     * @param includeRevoked return only active logins (false) or all logins (true)
     * @return all of the current agents logins upto RESULT_LIMIT
     */
    List<AuthToken> getOwnLogins( boolean includeRevoked ) throws ReefServiceException;

    /**
     * Revoke all of our own tokens (except for our current one). This is similar to a "global logout". To
     * revoke our current login call logout() on the client
     * @return list of all logins that were revoked upto RESULT_LIMIT
     */
    List<AuthToken> revokeOwnLogins() throws ReefServiceException;

    /**
     * @param includeRevoked return only active logins (false) or all logins (true)
     * @return all of the logins in the system upto RESULT_LIMIT
     */
    List<AuthToken> getLogins( boolean includeRevoked ) throws ReefServiceException;

    /**
     * Search for all logins by a particular user.
     * @param includeRevoked return only active logins (false) or all logins (true)
     * @param agentName name of the agent to search with, returns blank list of agent is unknown
     * @return all of the logins for the agent upto RESULT_LIMIT
     */
    List<AuthToken> getLoginsByAgent( boolean includeRevoked, String agentName ) throws ReefServiceException;

    /**
     * Search by clientVersion, useful for finding older applications that need to be updated
     * @param includeRevoked return only active logins (false) or all logins (true)
     * @param clientVersion version string to search by
     * @return all of the logins made with a particular client library upto RESULT_LIMIT
     */
    List<AuthToken> getLoginsByClientVersion( boolean includeRevoked, String clientVersion ) throws ReefServiceException;

    /**
     * revoke a particular login by id.
     * @param id id of login to revoke
     * @return token if it was deleted or an exception if not found/unauthorized
     */
    AuthToken revokeLoginById( ReefID id ) throws ReefServiceException;

    /**
     * Try to revoke all of the auth tokens for a user (useful when removing a user from the system)
     * @param agentName name of the agent to search with, returns blank list of agent is unknown
     * @return all tokens that have just been revoked
     */
    List<AuthToken> revokeLoginByAgent( String agentName ) throws ReefServiceException;


}
