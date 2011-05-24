/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  Green Energy Corp licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.api.javaclient;

/**
 *  Interface defining a guaranteed, deferred value.  Value can be retrieved synchronously or asynchronously.
 */
public interface Promise<A> {

   /**
     * Synchronously blocks for some un-specified period of time for the value. Returns immediately if the promise is complete.
     *
     * @return The value-type of the Promise
     */
   A await();

   /**
     *  Asynchronously calls an IResponseListener when the promise is complete from
     *  some unknown thread. Calls back immediately from the calling thread if the
     *  promise is complete.
     *
     * @param listener
     */
   void addListener(ResponseListener<A> listener);

   /**
     * Inquires about the completion state of the promise.
     *
     * @return True if the promise is complete, false otherwise
     */
   boolean isComplete();
}