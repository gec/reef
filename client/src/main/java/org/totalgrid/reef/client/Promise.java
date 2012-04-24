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

import org.totalgrid.reef.client.exception.ReefServiceException;

/**
 * A Promise is an asynchronous object that can be returned from API functions as a
 * placeholder for a result that will be ready sometime in the future. It is different
 * from a Future because it is not cancelable and provides a callback that will be
 * executed when the value is set. It is also guaranteed to complete eventually with
 * either the expected result or an exception.
 *
 * A Promise can be used in a totally synchronous manner by calling .await() on all
 * promises immediately. The real power in the promises comes from the use of the .listen()
 * callback; this allows the application to be entirely event driven.
 *
 * @param <T>
 */
public interface Promise<T>
{
    /**
     * wait for the result to be set, there is no timeout because the client will
     * timeout the request after RequestHeaders.getTimeout milliseconds and a
     * response time out exception will be thrown. If an error was thrown in the
     * producing code (BadRequestException, ResponseTimeout, ServiceIO, ...) it will
     * be thrown when await is called.
     */
    T await() throws ReefServiceException;

    /**
     * attach a listener to the promise that will be called on the connection thread
     * as soon as the result is set (or a timeout occurs). If the Promise is already
     * complete listen will still be called on the connection thread (not the callers
     * thread).
     */
    void listen( PromiseListener<T> listener );

    /**
     * has the promise got a final value set?
     */
    boolean isComplete();

    <U> Promise<U> transform(PromiseTransform<T, U> trans);

}
