package org.totalgrid.reef.api.javaclient;

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

/**
 * A container class that wraps the response to a subscription request and the subscription interface itself
 *
 * @param <T> The type of result
 * @param <U> The type of the subscription
 */
public interface SubscriptionResult<T, U> {

   /**
     * @return The value of response
     */
   T getResult();

   /**
     * @return The interface used for starting/stopping the actual subscription
     */
   Subscription<U> getSubscription();

}
