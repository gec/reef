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
package org.totalgrid.reef.client.service;

import org.totalgrid.reef.client.SubscriptionCreator;
import org.totalgrid.reef.client.SubscriptionResult;
import org.totalgrid.reef.client.exception.ReefServiceException;
import org.totalgrid.reef.proto.Events.Event;
import org.totalgrid.reef.proto.Events.EventSelect;
import org.totalgrid.reef.proto.Model.ReefID;

import java.util.List;

/**
 * This service is used to get and produce Events on the reef system. Events are generated by the system in response
 * to unusual or interesting occurances, usually they are interesting to an operator but do not require immediate action.
 * When an event is published the system may "upgrade" an Event to also generate an alarm.
 *
 * Tag for api-enhancer, do not delete: !api-definition!
 */
public interface EventService extends SubscriptionCreator
{
    /**
     * get a single event
     *
     * @param id event
     */
    Event getEventById( ReefID id ) throws ReefServiceException;

    /**
     * get the most recent events
     *
     * @param limit the number of incoming events
     */
    List<Event> getRecentEvents( int limit );

    /**
     * get the most recent events and setup a subscription to all future events
     *
     * @param limit the number of incoming events
     */
    SubscriptionResult<List<Event>, Event> subscribeToRecentEvents( int limit ) throws ReefServiceException;

    /**
     * get the most recent events and setup a subscription to all future events
     *
     * @param types event type names
     * @param limit the number of incoming events
     */
    SubscriptionResult<List<Event>, Event> subscribeToRecentEvents( List<String> types, int limit ) throws ReefServiceException;

    /**
     * get the most recent events
     *
     * @param types event type names
     * @param limit the number of incoming events
     */
    List<Event> getRecentEvents( List<String> types, int limit ) throws ReefServiceException;

    /**
     * @param selector a selector that allows us to express more specific queries
     * @return all matching events
     */
    List<Event> getEvents( EventSelect selector ) throws ReefServiceException;

    /**
     * Allows querying and subscribing to specific event types
     * not WARNING all event selectors are valid for subscriptions
     *
     * @param selector a selector that allows us to express more specific queries
     * @return all matching events
     */
    SubscriptionResult<List<Event>, Event> subscribeToEvents( EventSelect selector ) throws ReefServiceException;
}