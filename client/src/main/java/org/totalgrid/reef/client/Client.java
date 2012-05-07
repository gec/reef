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

import org.totalgrid.reef.client.operations.RequestListenerManager;
import org.totalgrid.reef.client.operations.ServiceOperations;

/**
 * A client represents an authenticated link with a Reef server.
 *
 * Each client is fundamentally a container of state:
 *
 * - reference to the underlying connection
 * - authToken
 * - common RequestHeaders sent with every request
 * - a background thread that promises and subscription events are executed
 * - the current batching state
 * - RequestListener maps and managers
 * - SubscriptionCreation listener
 *
 * If there are multiple threads (or contexts) in the application each thread should get its own client
 * to control its own state. The only state copied over to a new client is the authToken.
 *
 * Clients are NOT thread-safe as they carry specific state such as the RequestHeaders
 * and SubscriptionCreationListener instances.
 */
public interface Client
{

    /**
     * Get current common RequestHeaders object.
     * @return current immutable RequestHeaders for the Client
     */
    RequestHeaders getHeaders();

    /**
     * Sets the default RequestHeaders for the Client. Usage sequence is:
     *
     * 1) Read the current headers
     * 2) Create a new request headers by calling one of the functions on 1)
     * 3) Set the default headers using the modified version in 2)
     *
     * @param headers The new RequestHeaders for the client to use on all requests
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
     * @throws org.totalgrid.reef.client.exception.ReefServiceException If the interface can not be found
     */
    <A> A getService( Class<A> klass );

    /**
     * Delete the authToken associated with this client, all future requests will fail with an
     * UnauthorizedException. This will also logout all other clients created using the same authToken including
     * all sibling clients generated with spawn.
     */
    void logout();

    /**
     * create a new client based on this one that has seperate state and subscription threading.
     * @return a new client with just the authToken and connection reference copied
     */
    Client spawn();

    ClientInternal getInternal();

    /**
     * interface for making low-level requests to the server when creating a new "service" class
     */
    ServiceOperations getServiceOperations();

    /**
     * a controller for the BatchMode of the client
     */
    Batching getBatching();

    /**
     * controller for adding/removing RequestListeners to this client
     */
    RequestListenerManager getRequestListenerManager();

    /**
     * shortcut to connection level service registry, shared by all clients on same connection
     */
    ServiceRegistry getServiceRegistry();
}
