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

import org.totalgrid.reef.client.proto.Envelope;

/**
 * Allows implementers of services to publish subscription events when service objects are added/modified/removed.
 */
public interface EventPublisher
{
    /**
     * Performs the work of binding a client subscriber's listening queue to the exchange where the
     * subscription events are being published.
     *
     * @param subQueue Queue name to bind to subscription exchange
     * @param key AMQP routing key that determines the "filter" of messages the client is subscribed to.
     * @param klass Class used to look-up the exchange name from the type descriptor
     * @param <T> Class of subscription event type
     */
    <T> void bindQueueByClass( String subQueue, String key, Class<T> klass );

    /**
     * Publishes a service subscription event.
     *
     * @param eventType Event type (added/modified/removed)
     * @param eventMessage Payload message for the event
     * @param routingKey AMQP routing key that determines what filters the event matches
     * @param <T>
     */
    <T> void publishEvent( Envelope.SubscriptionEventType eventType, T eventMessage, String routingKey );
}
