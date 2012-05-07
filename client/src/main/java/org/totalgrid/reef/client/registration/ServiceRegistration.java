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
 * Interface for implementers of services. Provides the ability to bind services (listen for service
 * requests) and publish service subscription events.
 *
 * Only should be used by applications wanting to expose services to other clients/applications.
 * Most of capabilities exposed by this class require "high level" broker access with the ability to declare exchanges
 * perform bind operations.
 */
public interface ServiceRegistration
{
    /**
     * Gets interface for publishing service subscription events.
     */
    EventPublisher getEventPublisher();

    /**
     * Provides the ability to bind services with the broker.
     *
     * @param service Service implementation requests will be forwarded to.
     * @param descriptor Type descriptor for service request/response type.
     * @param destination Destination type that determines request forwarding (AddressableDestination/AnyNodeDestination).
     * @param competing whether each message should be consumed once or sent to all listeners
     * @param <T> Service request/response type.
     * @return Handle to manage the lifecycle of the binding.
     */
    <T> SubscriptionBinding bindService( Service service, TypeDescriptor<T> descriptor, Routable destination, boolean competing );

    // TODO: add lateBindService and bindServiceQueue functions


    /**
     * Bind an arbitrary subscription queue to service.
     *
     * NOTE: Requires "services" level access to broker to perform binding operations, most clients
     * do not have the necessary privileges to bind to arbitrary queues.
     */
    <T> void bindServiceQueue( String subscriptionQueue, String key, Class<T> klass );


    // TODO: event and service declaration in same call
    void declareEventExchange( Class<?> klass );
}
