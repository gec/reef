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
import org.totalgrid.reef.client.SubscriptionBinding;
import org.totalgrid.reef.client.SubscriptionResult;
import org.totalgrid.reef.client.registration.Service;
import org.totalgrid.reef.client.types.TypeDescriptor;

/**
 * when implementing a client that will be making requests to the server and passing back async requests it is not
 * trivial to get the error handling and subscription management code correct so we have provided the following
 * interfaces to make writing those classes as robust and easy to write as possible.
 */
public interface ServiceOperations
{
    /**
     * Standard requests to the server will use this request. A single request is made using RestOperations and the
     * Promise should be transformed to the extracted type.
     * @param request "functor" object to perform the request and convert the result to correct state.
     * @return Promise returned from request.execute().
     */
    <T> Promise<T> request( BasicRequest<T> request );

    /**
     * nearly identical to request but allows executing multiple requests to the server in one "batch". If the client
     * is not in batchMode (normally it is not) a temporary batch handler is setup, RestOperations requests are queued
     * and after request.execute() returns all of the requests are flushed and handled in order on the server. If the
     * client is already in batchMode then no automatic flushing is done and the requests are done when the application
     * controlled batch is flushed.
     *
     * Common batchRequest functions are scatter/gather like functions where we want to operate on a list of names
     * or uuids in one shot without waiting for each result. Keep in mind that these operations are all done at once
     * and cannot depend on an earlier result in the same batch. The design is very similar to redis's pipelining.
     *
     * Note that we will be making a number of requests to the server, each of which will return its own Promise, we
     * will need to merge those Promises together into a single promise using one of the helper functions to join
     * multiple promises.
     *
     * @param request request object that issues a number of RestOperation requests and fuses the results into a single promise
     * @return single promise that reflects success/failure of the whole batch of requests
     * @see Batching
     */
    <T> Promise<T> batchRequest( BasicRequest<T> request );

    /**
     * When making a subscription from the client we will create a broker queue for our subscription events. When it is
     * created it is not "bound" to anything and will not recieve any updates until we setup some subscription bindings
     * on the server. To do this we send the name of our subscription queue along with our request, the server will then
     * use that name to setup the bindings for the client (if authorized and a legal request of course).
     *
     * @param descriptor type for the subscription events
     * @param request request subscription request handler that makes a request to the server to setup a subscription queue
     * @return subscription result which includes the converted promise and the Subscription object.
     */
    <T, U> Promise<SubscriptionResult<T, U>> subscriptionRequest( TypeDescriptor<U> descriptor, SubscriptionBindingRequest<T> request );

    /**
     * Very similar to the subscription subscriptionRequest operation, a broker queue for our client side service is
     * created and attached to our Service class. It is not going to receive any requests until the server "binds" it to
     * the requests queues with appropriate request keys. Therefore we send our queue name along with a request to the server
     * to have it bind requests on our behalf
     *
     * @param service class that implements the Service interface to handle the local requests
     * @param descriptor type of the payload in the ServieRequest envelope
     * @param request handler that sends our bind request to the server
     * @return binding to the local queue that allows us to cancel our service when we are done with it
     */
    <T, U> Promise<SubscriptionBinding> clientServiceBinding( Service service, TypeDescriptor<U> descriptor, SubscriptionBindingRequest<T> request );

    BindOperations getBindOperations();
}
