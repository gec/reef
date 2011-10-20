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
package org.totalgrid.reef.api.japi.client;

/**
 * Immutable interface that defines what operations can be done to service headers
 */
public interface RequestHeaders
{

    /**
     * Sets the auth token
     * @param token A string representing the auth token
     * @return A new RequestHeaders with the auth token set
     */
    RequestHeaders setAuthToken( String token );

    /**
     * Clears the auth token
     * @return A new RequestHeaders with the auth token cleared
     */
    RequestHeaders clearAuthToken();

    /**
     * overrides the system default for maximum # of results to return at a time
     * @param resultLimit must be a positive integer
     * @reutrn a new RequestHeaders with the result limit set
     */
    RequestHeaders setResultLimit( int resultLimit );

    /**
     * Clears the result limit
     * @return A new RequestHeader with the result limit set to the system default
     */
    RequestHeaders clearResultLimit();


    /**
     * Overrides the system default for the request timeout in milliseconds
     * @param timeoutMillis request timeout in milliseconds
     * @return a new RequestHeaders with the timeout set
     */
    RequestHeaders setTimeout( long timeoutMillis );

    /** Clear the timeout
     * @return   A new RequestHeaders with the timeout cleared to the system default
     */
    RequestHeaders clearTimeout();

    /**
     * Overrides the system default destination
     * @param key The Routable describining the destination
     * @return A new RequestHeaders with the specific destination set
     */
    RequestHeaders setDestination( Routable key );

    /**
     * Clear the destination
     * @return A new RequestHeaders with the destination cleared to the system default
     */
    RequestHeaders clearDestination();

}
