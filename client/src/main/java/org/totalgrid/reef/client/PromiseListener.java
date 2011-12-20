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
 * callback interface for listening to a promise when writing asynchronous code
 * @param <A>
 */
public interface PromiseListener<A>
{
    /**
     * when a promise has completed (successfully or not) all PromiseListeners will be called
     * back with a reference to a new promise that has a result set. This is so the client
     * can call the await() method to retrieve the value and be notified by exception if the
     * call failed.
     *
     * This callback will come in on the connection thread and should be handled like a subscription
     * event, and not block for extended periods because it may stop other Promises or subscriptions
     * from being completed.
     *
     * @param promise a complete promise whose value (or error) is retrievable by calling await.
     */
    void onComplete( Promise<A> promise );
}
