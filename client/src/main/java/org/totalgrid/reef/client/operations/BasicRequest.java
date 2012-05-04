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

/**
 * Each request to a service is fundamentally an asynchronous operation, the request is made and eventually (soon)
 * the result will be returned to the client. By embracing this underlying async nature we can provide a number of
 * very interesting features.
 *
 * - We can issue many requests in parell without waiting for each of them to finish.
 * - We can batch up a large number of requests and perform them in a single roundtrip to the server.
 * - We can return a Promise to the final value and allow our client to determine when/if to wait for a particular
 *   response.
 *
 * This necessiates implementing the requesting and response handling logic soley in terms of Promises. We effectivley
 * are setting up a list of transformations to perform when the result is eventually returned to us by the server. There
 * should never be any use of .await() or .isComplete in a BasicRequest block, only transform and transformError are
 * acceptable. Only the application should be checking the status of a request or awaiting the result.
 *
 * Operating on promises is not terribly difficult but it can be a bit tricky to handle errors correctly so the
 * ServiceOperation class executes the actual request operations (execute call) inside a wrapper function that will
 * catch errors made during request time and attach a helpful error message (using transformError) to the promise incase
 * something goes wrong making the request.
 */
public interface BasicRequest<T>
{

    /**
     * helpful error message that describes the context of the request. This will be attached to the causing
     * ReefServiceException.
     */
    String errorMessage();

    /**
     * Uses the RestOperations to make the real protobuf requests to the server. The response to each request will be
     * of type Promise&lt;Response&gt; and that should generally be checked for errors and only a payload object
     * returned. This can be done with a custom PromiseTransformer or using one of the standard promise transformers in
     * CommonResponseTransformations
     * @param operations lowest level interface to the server to make a single get,put,post or delete request.
     * @return Promise containing only the payload object(s) or an error message
     */
    Promise<T> execute( RestOperations operations );
}
