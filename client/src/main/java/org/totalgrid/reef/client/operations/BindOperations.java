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

import org.totalgrid.reef.client.Subscription;
import org.totalgrid.reef.client.SubscriptionBinding;
import org.totalgrid.reef.client.types.TypeDescriptor;
import org.totalgrid.reef.client.registration.Service;


/**
 * Low-level interface to create a channel to receive events (either subscriptions or service requests).
 *
 * Subscriptions and Service created using this class will be serviced using the clients thread and therefore
 * no service response or subscription handling should block or take a long time. If much processing is necessary
 * a dedicated client (and therefore thread) can be used or the work should be pushed out to seperate worker
 * thread.
 */
public interface BindOperations
{
    /**
     * Creates a subscription queue for objects of a particular type. The id of this queue should be sent
     * to the server so it can do the binding.
     * @param descriptor object that includes the class info and deseralization code.
     * @return subscription reference for starting or canceling.
     */
    <T> Subscription<T> subscribe( TypeDescriptor<T> descriptor );

    /**
     * Similar to a subscription but tracks who sent the message to us so we can send the response back to the
     * correct client. No binding is done in the client, we send the id to the server so it can bind it correctly.
     * @param service service object that takes a typed request and provides a response to the caller
     * @param descriptor object that includes the class info and deseralization code.
     * @return binding that we can then cancel when we are done with the server.
     */
    <T> SubscriptionBinding lateBindService( Service service, TypeDescriptor<T> descriptor );
}
