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
package org.totalgrid.reef.client.rpc;

import org.totalgrid.reef.clientapi.SubscriptionCreator;
import org.totalgrid.reef.clientapi.SubscriptionResult;
import org.totalgrid.reef.clientapi.exceptions.ReefServiceException;
import org.totalgrid.reef.proto.Alarms.Alarm;
import org.totalgrid.reef.proto.Model.Entity;

import java.util.List;

/**
 * A service interface for managing and retrieving Alarms. Alarms are special
 * system events that require operator intervention. Each alarm has an
 * associated event object, but not all events are alarms.
 * <p/>
 * In contrast to events, alarms have persistent state. The three principal alarm states are unacknowledged,
 * acknowledged, and removed. The transitions between these states constitute the alarm lifecycle, and
 * manipulation of the states involves user workflow.
 * <p/>
 * Transitions in alarm state may themselves be events, as they are part of the record of user operations.
 * <p/>
 * During the configuration process, the system designer decides what events trigger alarms. The primary consumers of
 * alarms are operators tasked with monitoring the system in real-time and responding to abnormal conditions.
 *
 * Tag for api-enhancer, do not delete: !api-definition!
 */
public interface AlarmService extends SubscriptionCreator
{

    /**
     * Get a single alarm
     *
     * @param uid uid of alarm
     */
    Alarm getAlarm( String uid ) throws ReefServiceException;

    /**
     * Get the most recent alarms
     *
     * @param limit the number of incoming alarms
     */
    List<Alarm> getActiveAlarms( int limit ) throws ReefServiceException;

    /**
     * Get the most recent alarms and setup a subscription to all future alarms
     *
     * @param recentAlarmLimit the number of recent alarms.
     */
    SubscriptionResult<List<Alarm>, Alarm> subscribeToActiveAlarms( int recentAlarmLimit ) throws ReefServiceException;

    /**
     * Get the most recent alarms
     *
     * @param types event type names
     * @param recentAlarmLimit the number of recent alarms
     */
    List<Alarm> getActiveAlarms( List<String> types, int recentAlarmLimit ) throws ReefServiceException;

    /**
     * Get the most recent alarms
     *
     * @param entityTree entity proto that describes which entities we want to see related alarms for
     * @param types event type names
     * @param recentAlarmLimit the number of recent alarms
     */
    List<Alarm> getActiveAlarmsByEntity( Entity entityTree, List<String> types, int recentAlarmLimit ) throws ReefServiceException;

    /**
     * Silences an audible alarm
     */
    Alarm silenceAlarm( Alarm alarm ) throws ReefServiceException;

    /**
     * Acknowledge the alarm (silences if not already silenced)
     */
    Alarm acknowledgeAlarm( Alarm alarm ) throws ReefServiceException;

    /**
     * Change the Alarm state to REMOVED. Alarms are not deleted from the
     * database unless they roll off due to database pruning of old
     * information.
     */
    Alarm removeAlarm( Alarm alarm ) throws ReefServiceException;

}
