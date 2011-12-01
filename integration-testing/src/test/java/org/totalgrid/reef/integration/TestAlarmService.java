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
package org.totalgrid.reef.integration;

import org.junit.*;
import static org.junit.Assert.*;

import org.totalgrid.reef.client.exception.ReefServiceException;
import org.totalgrid.reef.client.service.AlarmService;
import org.totalgrid.reef.client.service.EntityService;
import org.totalgrid.reef.client.service.EventConfigService;
import org.totalgrid.reef.client.service.EventPublishingService;

import org.totalgrid.reef.client.sapi.rpc.impl.builders.EntityRequestBuilders;
import org.totalgrid.reef.proto.Alarms.*;
import org.totalgrid.reef.proto.Model.Entity;

import java.util.LinkedList;
import java.util.List;

import org.totalgrid.reef.integration.helpers.*;

public class TestAlarmService extends ReefConnectionTestBase
{

    /**
     * insert an event config and post an event of that type to generate the alarm
     */
    @Test
    public void prepareAlarms() throws ReefServiceException
    {
        EventConfigService configService = helpers;
        EventPublishingService pub = helpers;

        configService.setEventConfigAsAlarm( "Test.Alarm", 1, "Alarm", true );

        EntityService entityService = helpers;
        Entity e = entityService.getEntityByName( "StaticSubstation.Line02.Current" );

        // add an alarm for a point we know is not changing
        pub.publishEvent( "Test.Alarm", "Tests", e.getUuid() );
    }

    /** Test that some alarms are returned from the AlarmQuery service */
    @Test
    public void simpleQueries() throws ReefServiceException
    {
        AlarmService as = helpers;
        // Get all alarms that are not removed.
        List<Alarm> alarms = as.getActiveAlarms( 10 );
        assertTrue( alarms.size() > 0 );

    }

    /** Test getting alarms for a whole substation or individual device */
    @Test
    public void entityQueries() throws ReefServiceException
    {
        EntityService entityService = helpers;
        AlarmService as = helpers;

        // Get the first substation
        Entity substation = entityService.getEntityByName( "StaticSubstation" );

        // Get all the points in the substation. Alarms are associated with individual points.
        Entity eqRequest = EntityRequestBuilders.getOwnedChildrenOfTypeFromRootId( substation, "Point" );

        // Get the alarms on both the substation and devices under the substation.
        List<String> alarmTypes = new LinkedList<String>();
        alarmTypes.add( "Test.Alarm" );

        List<Alarm> alarms = as.getActiveAlarmsByEntity( eqRequest, alarmTypes, 10 );
        assertTrue( alarms.size() > 0 );
    }

    /** Test alarm state update. */
    @Test
    public void updateAlarms() throws ReefServiceException
    {
        AlarmService as = helpers;
        // Get unacknowledged alarms.
        List<Alarm> alarms = as.getActiveAlarms( 50 );
        assertTrue( alarms.size() > 0 );

        // Grab the first unacknowledged alarm and acknowledge it.
        for ( Alarm alarm : alarms )
        {
            if ( alarm.getState() == Alarm.State.UNACK_AUDIBLE || alarm.getState() == Alarm.State.UNACK_SILENT )
            {
                Alarm result = as.acknowledgeAlarm( alarm );
                assertTrue( result.getState() == Alarm.State.ACKNOWLEDGED );
                break;
            }
        }
    }

}