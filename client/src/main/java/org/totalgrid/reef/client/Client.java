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

/**
 * A client represents an authenticated link with a Reef server.
 *
 * Clients are NOT thread-safe as they carry specific state such as the default RequestHeaders
 * and SubscriptionCreationListener instances.
 */
public interface Client
{

    /**
     * @return current immutable RequestHeaders for the Client
     */
    RequestHeaders getHeaders();

    /**
     * Sets the default RequestHeaders for the Client. Typical usage sequence is:
     *
     * 1) Read the current headers
     * 2) Create a new request headers by calling one of the functions on 1)
     * 3) Set the default headers using the modified version in 2)
     *
     * @param headers The new default RequestHeaders for the client to use on all requestss
     */
    void setHeaders( RequestHeaders headers );

    /**
     * Add a listener that is called every time a subscription is created
     * @param listener
     */
    void addSubscriptionCreationListener( SubscriptionCreationListener listener );

    /**
     * Remove a subscription creation listener
     * @param listener
     */
    void removeSubscriptionCreationListener( SubscriptionCreationListener listener );

    /**
     * Get an active service interface by class.
     * @param klass The class of interface to return. Valid interfaces are found in org.totalgrid.reef.client.service
     * @param <A>
     * @return
     * @throws org.totalgrid.reef.client.exception.ReefServiceException If the interface can not be found
     */
    <A> A getService( Class<A> klass );

    /**
     * adds a factory for an RpcClass
     * @param info defines the impl and the interfaces it implements
     */
    void addServiceProvider( ServiceProviderInfo info );

    /**
     * Delete the authToken associated with this client, all future requests will fail with an
     * UnauthorizedException
     */
    void logout();
}
