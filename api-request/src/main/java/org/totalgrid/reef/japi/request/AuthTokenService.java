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
import org.totalgrid.reef.proto.Auth;
import org.totalgrid.reef.proto.Auth.AuthToken;

/**
 * A service for retrieving and deleting authorization tokens needed to access all other Reef services.
 * All requests (except for authorization token requests) require that the agent has acquired an
 * "Auth Token" and is sending it with every request. This auth token contains the user name and
 * all of the permissions available to that user. The token itself is a cryptographically secure string
 * that is an unguessable, unforgettable and must be kept secret by the client. Anyone with access to
 * that token will have the full capabilities of that user until it expires or is revoked. When a user
 * is finished using an auth token it should be deleted to minimize this danger.
 * <p/>
 * Every request to the services needs to have an auth token in the headers. They can be sent with each
 * client request, the Session interface has overloads to attach headers to each request. It is easiest
 * to attach the auth token to the underlying session using the setAuthToken function.
 * <p/>
 * add TODO setAuthToken function on Session
 */
public interface AuthTokenService
{
    /**
     * Create an authorization token for the specified Agent with all available
     * permissions "checked out". If the password or agentName is wrong this
     * method will throw an exception without indicating which was wrong.
     *
     * @param agentName
     * @param passwordUnencrypted
     * @return authToken string
     */
    String createNewAuthorizationToken( String agentName, String passwordUnencrypted ) throws ReefServiceException;

    /**
     * Revoke the specified authorization token. This means all future requests using this authorization token will fail.
     * @param authorizationToken
     */
    AuthToken deleteAuthorizationToken( String authorizationToken ) throws ReefServiceException;
}