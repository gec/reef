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

import org.totalgrid.reef.client.exception.ReefServiceException;
import org.totalgrid.reef.proto.Alarms.EventConfig;
import org.totalgrid.reef.proto.Alarms.EventConfig.Designation;

import java.util.List;

/**
 *  // TODO: find flint's good eventconfig documentation and move it here
 *
 *  Tag for api-enhancer, do not delete: !api-definition!
 */
public interface EventConfigService
{

    /**
     * get all of the event handling configurations
     */
    List<EventConfig> getEventConfigurations() throws ReefServiceException;

    /**
     * @param builtIn event configurations fall into two categories, either builtIn or custom.
     *                users can only delete custom configurations
     * @return get a subset of the event configurations
     */
    List<EventConfig> getEventConfigurations( boolean builtIn ) throws ReefServiceException;

    /**
     * get a single event handling configuration or throw an exception it doesn't exist
     * @param eventType get a single
     */
    EventConfig getEventConfigurationByType( String eventType ) throws ReefServiceException;

    /**
     * Create a new event routing configuration that routes only to log file
     * @param eventType name of the event, usually of the format Application.Event. Ex: Scada.ControlSent, System.UserLogin
     * @param severity severity to attach to event
     * @param resourceString format string to render while replacing the named attributes
     * @return newly generated event routing configuration
     */
    EventConfig setEventConfigAsLogOnly( String eventType, int severity, String resourceString ) throws ReefServiceException;

    /**
     * Create a new event routing configuration that routes to event table and log file
     * @param eventType name of the event, usually of the format Application.Event. Ex: Scada.ControlSent, System.UserLogin
     * @param severity severity to attach to event
     * @param resourceString format string to render while replacing the named attributes
     * @return newly generated event routing configuration
     */
    EventConfig setEventConfigAsEvent( String eventType, int severity, String resourceString ) throws ReefServiceException;

    /**
     * Create a new event routing configuration that routes to event table and log file and makes an alarm lifecycle object
     * @param eventType name of the event, usually of the format Application.Event. Ex: Scada.ControlSent, System.UserLogin
     * @param severity severity to attach to event
     * @param resourceString format string to render while replacing the named attributes
     * @param audibleAlarm should alarm start out in the UNACK_AUDIBLE state, making noise on operator consoles
     * @return newly generated event routing configuration
     */
    EventConfig setEventConfigAsAlarm( String eventType, int severity, String resourceString, boolean audibleAlarm ) throws ReefServiceException;

    /**
     * Create a new event routing configuration
     * @param eventType name of the event, usually of the format Application.Event. Ex: Scada.ControlSent, System.UserLogin
     * @param severity severity to attach to event
     * @param designation determines if the event should be treated as an alarm, an event or a log
     * @param audibleAlarm if designation is ALARM this determines if the alarm starts out making noise
     * @param resourceString format string to render while replacing the named attributes
     * @return newly generated event routing configuration
     */
    EventConfig setEventConfig( String eventType, int severity, Designation designation, boolean audibleAlarm, String resourceString )
        throws ReefServiceException;

    /**
     * deletes an event routing
     * @param config configuration to delete
     * @return copy of deleted record
     */
    EventConfig deleteEventConfig( EventConfig config ) throws ReefServiceException;
}
