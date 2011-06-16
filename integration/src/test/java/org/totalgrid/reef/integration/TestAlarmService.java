/**
 * Copyright 2011 Green Energy Corp.
 * 
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the GNU Affero General Public License
 * Version 3.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 * http://www.gnu.org/licenses/agpl.html
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

import org.totalgrid.reef.japi.ReefServiceException;
import org.totalgrid.reef.japi.request.AlarmService;
import org.totalgrid.reef.japi.request.EntityService;
import org.totalgrid.reef.japi.request.EventConfigService;
import org.totalgrid.reef.japi.request.EventCreationService;
import org.totalgrid.reef.japi.request.builders.EntityRequestBuilders;
import org.totalgrid.reef.japi.request.builders.EventConfigRequestBuilders;
import org.totalgrid.reef.japi.request.builders.EventRequestBuilders;
import org.totalgrid.reef.proto.Alarms.*;
import org.totalgrid.reef.proto.Model.Entity;

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
        EventConfigService configService = (EventConfigService)helpers;
        EventCreationService pub = (EventCreationService)helpers;

        configService.setEventConfigAsAlarm( "Test.Alarm", 1, "Alarm", true );

        EntityService entityService = (EntityService)helpers;
        Entity e = entityService.getEntityByName( "StaticSubstation.Line02.Current" );

        // add an alarm for a point we know is not changing
        pub.publishEvent( "Test.Alarm", "Tests", e.getUuid() );
    }

    /** Test that some alarms are returned from the AlarmQuery service */
    @Test
    public void simpleQueries() throws ReefServiceException
    {
        AlarmService as = (AlarmService)helpers;
        // Get all alarms that are not removed.
        List<Alarm> alarms = as.getActiveAlarms( 10 );
        assertTrue( alarms.size() > 0 );

    }

    /** Test getting alarms for a whole substation or individual device */
    @Test
    public void entityQueries() throws ReefServiceException
    {
        EntityService entityService = (EntityService)helpers;
        // Get the first substation
        Entity substation = entityService.getEntityByName( "StaticSubstation" );

        // Get all the points in the substation. Alarms are associated with individual points.
        Entity eqRequest = EntityRequestBuilders.getOwnedChildrenOfTypeFromRootUid( substation, "Point" );

        //entityService.getEntityRelatedChildrenOfType(substation.getUuid(), "owns", "Point");

        // Get the alarms on both the substation and devices under the substation.
        List<Alarm> alarms = SampleRequests.getAlarmsForEntity( client, eqRequest, "Test.Alarm" );
        assertTrue( alarms.size() > 0 );
    }

    /** Test alarm state update. */
    @Test
    public void updateAlarms() throws ReefServiceException
    {

        // Get unacknowledged alarms.
        List<Alarm> alarms = SampleRequests.getUnRemovedAlarms( client, "Test.Alarm" );
        assertTrue( alarms.size() > 0 );

        // Grab the first unacknowledged alarm and acknowledge it.
        for ( Alarm alarm : alarms )
        {
            if ( alarm.getState() == Alarm.State.UNACK_AUDIBLE || alarm.getState() == Alarm.State.UNACK_SILENT )
            {
                Alarm result = SampleRequests.updateAlarm( client, alarm, Alarm.State.ACKNOWLEDGED );
                assertTrue( result.getState() == Alarm.State.ACKNOWLEDGED );
                break;
            }
        }
    }

}