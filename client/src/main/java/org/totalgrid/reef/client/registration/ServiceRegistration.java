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
package org.totalgrid.reef.client.registration;

import org.totalgrid.reef.client.Routable;
import org.totalgrid.reef.client.SubscriptionBinding;
import org.totalgrid.reef.client.types.TypeDescriptor;

/**
 * Client-based interface for implementers of services. Provides the ability to bind services (listen for service
 * requests) and publish service subscription events.
 *
 */
public interface ServiceRegistration
{
    /**
     * Gets interface for publishing service subscription events.
     *
     * @return
     */
    EventPublisher getEventPublisher();

    /**
     * Provides the ability to bind services with the broker.
     *
     * @param service Service implementation requests will be forwarded to.
     * @param descriptor Type descriptor for service request/response type.
     * @param destination Destination type that determines request forwarding (AddressableDestination/AnyNodeDestination).
     * @param competing
     * @param <T> Service request/response type.
     * @return Handle to manage the lifecycle of the binding.
     */
    <T> SubscriptionBinding bindService( Service service, TypeDescriptor<T> descriptor, Routable destination, boolean competing );
}
