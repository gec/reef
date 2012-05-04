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
package org.totalgrid.reef.client.operations;

import org.totalgrid.reef.client.Promise;
import org.totalgrid.reef.client.RequestHeaders;
import org.totalgrid.reef.client.SubscriptionBinding;
import org.totalgrid.reef.client.proto.Envelope;

/**
 * Lowest level interface to the client requests. The request object is packaged up with a verb and any extra
 * request headers and sent to the server (or queued in a batch). A promise of a Response is returned from all functions
 * and should be converted to a more usable object by a PromiseTransfomer.
 */
public interface RestOperations
{
    /**
     * low level function to make a request.
     * @param verb  what REST verb we are using
     * @param payload object we will seralize and send to server (protobuf)
     * @param subscriptionBinding local queue name to send to the server for binding
     * @param headers extra headers we will merge with the client level headers (overwriting on conflicts)
     * @return a Promise containing the evental response or error
     */
    <T> Promise<Response<T>> request( Envelope.Verb verb, T payload, SubscriptionBinding subscriptionBinding, RequestHeaders headers );

    /**
     * low level function to make a request.
     * @param verb  what REST verb we are using
     * @param payload object we will seralize and send to server (protobuf)
     * @param subscriptionBinding local queue name to send to the server for binding
     * @return a Promise containing the evental response or error
     */
    <T> Promise<Response<T>> request( Envelope.Verb verb, T payload, SubscriptionBinding subscriptionBinding );

    /**
     * helper just calls request(verb, payload, null)
     */
    <T> Promise<Response<T>> request( Envelope.Verb verb, T payload, RequestHeaders headers );

    /**
     * helper just calls request(verb, payload, null)
     */
    <T> Promise<Response<T>> request( Envelope.Verb verb, T payload );

    /**
     * helper just calls request(GET, payload, subscriptionBinding)
     */
    <T> Promise<Response<T>> get( T payload, SubscriptionBinding subscriptionBinding );

    /**
     * helper just calls request(DELETE, payload, subscriptionBinding)
     */
    <T> Promise<Response<T>> delete( T payload, SubscriptionBinding subscriptionBinding );

    /**
     * helper just calls request(PUT, payload, subscriptionBinding)
     */
    <T> Promise<Response<T>> put( T payload, SubscriptionBinding subscriptionBinding );

    /**
     * helper just calls request(POST, payload, subscriptionBinding)
     */
    <T> Promise<Response<T>> post( T payload, SubscriptionBinding subscriptionBinding );

    /**
     * helper just calls request(GET, payload, headers)
     */
    <T> Promise<Response<T>> get( T payload, RequestHeaders headers );

    /**
     * helper just calls request(DELETE, payload, headers)
     */
    <T> Promise<Response<T>> delete( T payload, RequestHeaders headers );

    /**
     * helper just calls request(PUT, payload, headers)
     */
    <T> Promise<Response<T>> put( T payload, RequestHeaders headers );

    /**
     * helper just calls request(POST, payload, headers)
     */
    <T> Promise<Response<T>> post( T payload, RequestHeaders headers );

    /**
     * helper just calls request(GET, payload)
     */
    <T> Promise<Response<T>> get( T payload );

    /**
     * helper just calls request(DELETE, payload)
     */
    <T> Promise<Response<T>> delete( T payload );

    /**
     * helper just calls request(PUT, payload)
     */
    <T> Promise<Response<T>> put( T payload );

    /**
     * helper just calls request(POST, payload)
     */
    <T> Promise<Response<T>> post( T payload );
}
