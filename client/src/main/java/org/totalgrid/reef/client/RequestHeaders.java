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
package org.totalgrid.reef.client;

import org.totalgrid.reef.client.proto.Envelope;

/**
 * <p>Headers that are sent along with each request to reef, very similar in usage and design to HTTP headers.</p>
 *
 * <p>Each RequestHeaders is attached to one client and if a client is copied the only header that is
 * included in the copy is the authToken.</p>
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
     * whether an authToken is attached to headers
     */
    boolean hasAuthToken();

    /**
     * AuthToken associated with this client, can be used to create other clients that have
     * the same authorization. Must be protected as well as the username/password combo.
     * @return auth token string
     */
    String getAuthToken();

    /**
     * overrides the system default for maximum # of results to return at a time
     * @param resultLimit must be a positive integer
     * @return a new RequestHeaders with the result limit set
     */
    RequestHeaders setResultLimit( int resultLimit );

    /**
     * Clears the result limit
     * @return A new RequestHeader with the result limit set to the system default
     */
    RequestHeaders clearResultLimit();

    /**
     * whether a custom resultLimit is set
     */
    boolean hasResultLimit();

    /**
     * current result limit, a -1 indicates we will use server default
     */
    int getResultLimit();

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
     * whether a custom timeout is set
     */
    boolean hasTimeout();

    /**
     * current timeout, a -1 indicates we will use connection default
     */
    long getTimeout();

    /**
     * Overrides the system default destination
     * @param key The Routable describing the destination
     * @return A new RequestHeaders with the specific destination set
     */
    RequestHeaders setDestination( Routable key );

    /**
     * Clear the destination
     * @return A new RequestHeaders with the destination cleared to the system default
     */
    RequestHeaders clearDestination();

    /**
     * whether a custom destination is set
     */
    boolean hasDestination();

    /**
     * the current destination or AnyNodeDestination if unset
     */
    Routable getDestination();

    /**
     * This is the id of the subscription queue to use for this request, should only ever be set
     * for a single request at a time.
     * @param queueId id of the application owned queue
     * @see SubscriptionBinding
     */
    RequestHeaders setSubscribeQueue( String queueId );

    /**
     * clear the subscription queue setting
     */
    RequestHeaders clearSubscribeQueue();

    /**
     * whether a subscription is set
     */
    boolean hasSubscribeQueue();

    /**
     * get the subscribe queue if one is set or an empty string if not set
     */
    String getSubscribeQueue();

    /**
     * merge two request headers togther, the settings in other overwrite this objects values on conflicts
     * @param other another RequestHeaders object
     * @return the merged RequestHeaders object
     */
    RequestHeaders merge( RequestHeaders other );

    /**
     * Get all of the headers we would attach to a service request envelope.
     */
    java.lang.Iterable<Envelope.RequestHeader> toEnvelopeRequestHeaders();
}
