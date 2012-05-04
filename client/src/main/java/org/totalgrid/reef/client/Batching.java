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

import org.totalgrid.reef.client.proto.Envelope.BatchServiceRequest;

/**
 * When using async services (those that return Promises) we can switch the entire client into a batchMode which
 * means that all requests are not sent immediately, they are instead buffered inside the client and a Promise to
 * the eventual value is returned. When flush() is called the queued responses are built into a BatchServiceRequest
 * proto and sent to the BatchServiceRequest service on the server. The server will then handle all of the requests
 * in order and in a single transaction (for core services).
 *
 * After the flush() Promise has been fulfilled, the client can then check Promise.isComplete && Promise.await for
 * each of the queued requests.
 *
 * *NOTE* Do not attempt to use a sync api while using batching. Internally the sync apis are just wrappers around the
 * async implementations that immediatley call await. That await cannot possibly complete since we haven't called flush
 * an exception will be immediatley thrown.
 *
 * <pre>
 *     client.getBatching().start();
 *
 *     Promise<Entity> prom1 = services.getEntityByName("Test1");
 *
 *     assert(prom1.isComplete() == false);
 *
 *     client.getBatching().flush().await();
 *
 *     assert(prom1.isComplete() == true);
 * </pre>
 *
 * *IMPORTANT* only "core" services can be batched because the main BatchServiceRequest service needs to know about all
 * protos that may be batched.
 */
public interface Batching
{
    // TODO: add test and checking to make sure batching is being done correctly start->flush->exit
    // TODO: add hook to promise to determine if promise.await is called before flush and throw error
    /**
     * begin a batch request, all serviceOperations.requests will be queued for later flushing
     */
    void start();

    /**
     * end the batch mode and discard any queued requests
     */
    void exit();

    // TODO: flush should return Boolean not BatchServiceRequest
    /**
     * flush all of the queued requests in a single large batch requets
     * @return a promise indicating whether the overall batch request succeeded or failed
     */
    Promise<BatchServiceRequest> flush();

    /**
     * flush the queued requests in a number of batches with upto a max chunkSize.
     * @param chunkSize max number of requests to put in a single batch query
     * @return a promise indicating all of the overall batch requests
     */
    Promise<Boolean> flush( int chunkSize );
}
