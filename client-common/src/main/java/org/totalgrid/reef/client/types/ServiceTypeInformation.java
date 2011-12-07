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
package org.totalgrid.reef.client.types;

/**
 * <p>Each object type that is used for a service needs a bit of meta data to describe the
 * type of any generated subscription events.</p>
 *
 * <p>In most cases the subscription type will be identical to the request type but
 * for services with multiple ways of requesting the same data the subscription type
 * will be the underlying data type. In the default system the MeasurementSnapshot and
 * MeasurementHistory services both provide subscriptions that deliver raw measurements
 * not MeasurementHistory or MeasurementSnapshot protos.</p>
 *
 * @param <A> type of the request/response
 * @param <B> type of the subscription events
 */
public interface ServiceTypeInformation<A, B>
{
    /**
     * type descriptor for the request/response object
     */
    TypeDescriptor<A> getDescriptor();

    /**
     * the descriptor for the objects that will be sent to any subscriptions
     */
    TypeDescriptor<B> getSubscriptionDescriptor();

    /**
     * amqp level exchange name (TypeDescriptor.id()) for subscription events, usually the
     * name of the request/response exchange appeneded with "_events"
     */
    String getEventExchange();
}
