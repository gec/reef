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
package org.totalgrid.reef.api.japi.client.rpc;

import org.totalgrid.reef.api.japi.ReefServiceException;
import org.totalgrid.reef.api.proto.Events.Event;
import org.totalgrid.reef.api.proto.Model.ReefUUID;
import org.totalgrid.reef.api.proto.Utils.Attribute;

import java.util.List;

/**
 * When an application wants to publish an Event they should only supply the interesting
 * information they have on hand. The server will set most of the fields when the event is posted
 * including:
 *  - user_id that created an event (based on the posters auth token)
 *  - time when the event occured (actually records when the event service processes the put, if a very specific
 *    time is desired use device_time)
 *  - severity, rendered, alarm fields are all set based on the matching EventConfig record
 *  - uid is the Event id, if the returned event doesn't have this field set it means it was logged or dropped
 *
 */
public interface EventCreationService
{
    /**
     * publish a custom built Event proto to the event service. The severity, alarm, rendered, time, uid and user_id fields
     * will be overwritten by server so they do not need to be set.
     * @param event fully constructed event proto
     * @return created Event which will have uid, alarm, severity, rendered etc all set correctly
     */
    Event publishEvent( Event event ) throws ReefServiceException;

    /**
     * publish the simplest type of event which has no interesting details
     * @param eventType string name of the event type, must match an EventConfig entry
     * @param subsystem name of subsystem instance that generated message
     * @return created Event
     */
    Event publishEvent( String eventType, String subsystem ) throws ReefServiceException;

    /**
     * publish an event with given type and a set of details about the event
     * @param eventType string name of the event type, must match an EventConfig entry
     * @param subsystem name of subsystem instance that generated message
     * @param arguments a set of name-value pairs that are used during rendering of the event
     * @return created Event
     */
    Event publishEvent( String eventType, String subsystem, List<Attribute> arguments ) throws ReefServiceException;

    /**
     * publish an event with given type and a set of details about the event
     * @param eventType string name of the event type, must match an EventConfig entry
     * @param subsystem name of subsystem instance that generated message
     * @param deviceTime time in millis when the event should be regarded as occurring
     * @param arguments a set of name-value pairs that are used during rendering of the event
     * @return created Event
     */
    Event publishEvent( String eventType, String subsystem, long deviceTime, List<Attribute> arguments ) throws ReefServiceException;

    /**
     * publish an event that should be linked to a particular entity
     * @param eventType string name of the event type, must match an EventConfig entry
     * @param subsystem name of subsystem instance that generated message
     * @param entityUuid uuid of the entity most closely related to this event
     * @return created Event
     */
    Event publishEvent( String eventType, String subsystem, ReefUUID entityUuid ) throws ReefServiceException;

    /**
     * publish an event that should be linked to a particular entity
     * @param eventType string name of the event type, must match an EventConfig entry
     * @param subsystem name of subsystem instance that generated message
     * @param deviceTime time in millis when the event should be regarded as occurring
     * @param entityUuid uuid of the entity most closely related to this event
     * @return created Event
     */
    Event publishEvent( String eventType, String subsystem, long deviceTime, ReefUUID entityUuid ) throws ReefServiceException;

    /**
     * publish an event that should be linked to a particular entity
     * @param eventType string name of the event type, must match an EventConfig entry
     * @param subsystem name of subsystem instance that generated message
     * @param entityUuid uuid of the entity most closely related to this event
     * @param arguments a set of name-value pairs that are used during rendering of the event
     * @return created Event
     */
    Event publishEvent( String eventType, String subsystem, ReefUUID entityUuid, List<Attribute> arguments ) throws ReefServiceException;

    /**
     * publish an event that should be linked to a particular entity
     * @param eventType string name of the event type, must match an EventConfig entry
     * @param subsystem name of subsystem instance that generated message
     * @param deviceTime time in millis when the event should be regarded as occurring
     * @param entityUuid uuid of the entity most closely related to this event
     * @param arguments a set of name-value pairs that are used during rendering of the event
     * @return created Event
     */
    Event publishEvent( String eventType, String subsystem, long deviceTime, ReefUUID entityUuid, List<Attribute> arguments )
        throws ReefServiceException;

}
