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
 * A container class that wraps the response to a request with the "initial value" and the newly
 * created subscription.
 *
 * It is generally assumed that the client should process the result to setup the subscription handling
 * <ul>before</ul> starting the subscription. If the client application decides that they don't want
 * to start the subscription (perhaps if the results were not as expected) then they will still need
 * to cancel the subscription. It can be difficult to right this sort of cleanup code correctly (especially
 * when using exceptions) so we recommend using the SubscriptionCreationListener to move all subscription
 * canceling to a common location.
 *
 * @param <T> The type of result
 * @param <U> The type of the subscription
 */
public interface SubscriptionResult<T, U>
{

    /**
     * @return The "integrity poll" value of the request
     */
    T getResult();

    /**
     * @return The interface used for starting/stopping the actual subscription
     */
    Subscription<U> getSubscription();

}
