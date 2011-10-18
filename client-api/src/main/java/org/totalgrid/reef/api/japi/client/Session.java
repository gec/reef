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


import org.totalgrid.reef.api.japi.ReefServiceException;
import org.totalgrid.reef.api.japi.ServiceIOException;
import org.totalgrid.reef.api.japi.TypeDescriptor;

/**
 *  Session are provides access to REST service calls and subscriptions
 */
public interface Session
{

    /**
     * Make a service request using 'GET'
     *
     * @param request The request value to use
     * @param <A> The type of the request/response
     * @return A promise to the Response<A>
     * @throws ReefServiceException if the service request cannot be made
     */
    <A> Promise<Response<A>> get( A request ) throws ReefServiceException;

    /**
     * Make a service request using 'DELETE'
     *
     * @param request The request value to use
     * @param <A> The type of the request/response
     * @return A promise to the Response<A>
     * @throws ReefServiceException if the service request cannot be made
     */
    <A> Promise<Response<A>> delete( A request ) throws ReefServiceException;

    /**
     * Make a service request using 'POST'
     *
     * @param request The request value to use
     * @param <A> The type of the request/response
     * @return A promise to the Response<A>
     * @throws ReefServiceException if the service request cannot be made
     */
    <A> Promise<Response<A>> post( A request ) throws ReefServiceException;

    /**
     * Make a service request using 'PUT'
     *
     * @param request The request value to use
     * @param <A> The type of the request/response
     * @return A promise to the Response<A>
     * @throws ReefServiceException if the service request cannot be made
     */
    <A> Promise<Response<A>> put( A request ) throws ReefServiceException;

    /**
     * Make a service request using 'GET' that establishes a subscription
     *
     * @param request The request value to use
     * @param subscription Subscription object to use
     * @param <A> The type of the request/response
     * @return A promise to the Response<A>
     * @throws ReefServiceException if the service request cannot be made
     */
    <A> Promise<Response<A>> get( A request, Subscription<A> subscription ) throws ReefServiceException;

    /**
     * Make a service request using 'DELETE' that establishes a subscription
     *
     * @param request The request value to use
     * @param subscription Subscription object to use
     * @param <A> The type of the request/response
     * @return A promise to the Response<A>
     * @throws ReefServiceException if the service request cannot be made
     */
    <A> Promise<Response<A>> delete( A request, Subscription<A> subscription ) throws ReefServiceException;

    /**
     * Make a service request using 'POST' that establishes a subscription
     *
     * @param request The request value to use
     * @param subscription Subscription object to use
     * @param <A> The type of the request/response
     * @return A promise to the Response<A>
     * @throws ReefServiceException if the service request cannot be made
     */
    <A> Promise<Response<A>> post( A request, Subscription<A> subscription ) throws ReefServiceException;

    /**
     * Make a service request using 'PUT' that establishes a subscription
     *
     * @param request The request value to use
     * @param subscription Subscription object to use
     * @param <A> The type of the request/response
     * @return A promise to the Response<A>
     * @throws ReefServiceException if the service request cannot be made
     */
    <A> Promise<Response<A>> put( A request, Subscription<A> subscription ) throws ReefServiceException;


    /**
     * Creates a subscription object that can be used during a request. After a successful request, the subscription can be started.
     *
     * @param descriptor TypeDescriptor corresponding to the type of the Subscription
     * @param <A> Type of the subscription
     * @return A Subscription object to use in a subsequent request
     * @throws ServiceIOException If the broker is not available, the descriptor is not recognized
     */
    <A> Subscription<A> addSubscription( TypeDescriptor<A> descriptor ) throws ServiceIOException;

    @Deprecated
    <A> Subscription<A> addSubscription( TypeDescriptor<A> descriptor, SubscriptionEventAcceptor<A> acceptor ) throws ServiceIOException;

    /**
     * Returns the immutable RequestHeaders
     * @return A reference to the immutable RequestHeaders object currently being used by the client.
     */
    RequestHeaders getHeaders();

    /**
     * Changes the
     * @param headers New RequestHeaders object to use as a default
     */
    void setHeaders( RequestHeaders headers );

    /**
     * Close the session, releasing the underlying resource. The session is dead and can never be used again.
     */
    void close();

}
