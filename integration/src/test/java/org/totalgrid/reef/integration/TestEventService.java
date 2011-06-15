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

import org.junit.Test;
import org.totalgrid.reef.japi.ReefServiceException;
import org.totalgrid.reef.japi.client.SubscriptionCreationListener;
import org.totalgrid.reef.japi.request.*;
import org.totalgrid.reef.japi.request.builders.EventConfigRequestBuilders;
import org.totalgrid.reef.japi.request.builders.EventRequestBuilders;
import org.totalgrid.reef.integration.helpers.BlockingQueue;
import org.totalgrid.reef.integration.helpers.ReefConnectionTestBase;
import org.totalgrid.reef.integration.helpers.MockSubscriptionEventAcceptor;
import org.totalgrid.reef.japi.client.Subscription;
import org.totalgrid.reef.japi.client.SubscriptionResult;
import org.totalgrid.reef.proto.Alarms;
import org.totalgrid.reef.proto.Alarms.Alarm;
import org.totalgrid.reef.proto.Events;
import org.totalgrid.reef.proto.Model;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

public class TestEventService extends ReefConnectionTestBase
{
    @Test
    public void validateLongEventConfigStrings() throws ReefServiceException
    {
        EventConfigService configService = (EventConfigService)helpers;
        EventCreationService es = (EventCreationService)helpers;

        StringBuffer sb = new StringBuffer();
        for ( int i = 0; i < 1000; i++ )
            sb.append( "a" );
        String longString = sb.toString();

        Alarms.EventConfig config = configService.setEventConfig( "Test.EventSuperLong", 1, Alarms.EventConfig.Designation.EVENT, false, longString );

        assertEquals( longString, config.getResource() );

        //Events.Event event = es.publishEvent( "Test.EventSuperLong", "Tests" );
        //assertEquals( longString, event.getRendered() );
    }

    @Test
    public void prepareEvents() throws ReefServiceException
    {
        EventCreationService es = (EventCreationService)helpers;
        EntityService entityService = (EntityService)helpers;
        EventConfigService configService = (EventConfigService)helpers;

        // make an event type for our test events
        configService.setEventConfig( "Test.Event", 1, Alarms.EventConfig.Designation.EVENT, false, "Event" );

        Model.Entity entity = entityService.getEntityByName( "StaticSubstation.Line02.Current" );

        // populate some events
        for ( int i = 0; i < 15; i++ )
        {
            Events.Event e = es.publishEvent( "Test.Event", "Tests", entity.getUuid() );
            assertTrue( e.hasUid() );
            assertNotSame( 0, e.getUid().length() );

            assertTrue( e.hasTime() );
            // assertTrue(e.getTime() > 0);
        }
    }

    @Test
    public void getRecentEvents() throws ReefServiceException
    {
        EventService es = (EventService)helpers;
        List<Events.Event> events = es.getRecentEvents( 10 );
        assertEquals( events.size(), 10 );
    }

    @Test
    public void subscribeEvents() throws ReefServiceException, InterruptedException
    {

        MockSubscriptionEventAcceptor<Events.Event> mock = new MockSubscriptionEventAcceptor<Events.Event>( true );

        EventService es = (EventService)helpers;

        SubscriptionResult<List<Events.Event>, Events.Event> events = es.subscribeToRecentEvents( 10 );
        assertEquals( events.getResult().size(), 10 );

        EventCreationService pub = (EventCreationService)helpers;

        pub.publishEvent( EventRequestBuilders.makeNewEventForEntityByName( "Test.Event", "StaticSubstation.Line02.Current" ) );

        events.getSubscription().start( mock );

        mock.pop( 1000 );


    }

    @Test
    public void prepareAlarms() throws ReefServiceException
    {

        // make an event type for our test alarms
        client.put( EventConfigRequestBuilders.makeAudibleAlarm( "Test.Alarm", "Alarm", 1 ) ).await().expectOne();

        EventCreationService es = (EventCreationService)helpers;

        // populate some alarms
        for ( int i = 0; i < 5; i++ )
        {
            es.publishEvent( EventRequestBuilders.makeNewEventForEntityByName( "Test.Alarm", "StaticSubstation.Line02.Current" ) );
        }
    }

    @Test
    public void subscribeAlarms() throws ReefServiceException, InterruptedException
    {

        MockSubscriptionEventAcceptor<Alarm> mock = new MockSubscriptionEventAcceptor<Alarm>( true );

        EventService es = (EventService)helpers;
        AlarmService as = (AlarmService)helpers;

        SubscriptionResult<List<Alarm>, Alarm> result = as.subscribeToActiveAlarms( 2 );
        List<Alarm> events = result.getResult();
        assertEquals( events.size(), 2 );

        EventCreationService pub = (EventCreationService)helpers;

        pub.publishEvent( EventRequestBuilders.makeNewEventForEntityByName( "Test.Alarm", "StaticSubstation.Line02.Current" ) );

        result.getSubscription().start( mock );
        mock.pop( 1000 );
    }

    @Test
    public void subscriptionCreationCallback() throws ReefServiceException, InterruptedException
    {

        final BlockingQueue<Subscription<?>> callback = new BlockingQueue<Subscription<?>>();

        EventService es = (EventService)helpers;

        es.addSubscriptionCreationListener( new SubscriptionCreationListener() {
            @Override
            public void onSubscriptionCreated( Subscription<?> sub )
            {
                callback.push( sub );
            }
        } );

        SubscriptionResult<List<Events.Event>, Events.Event> result = es.subscribeToRecentEvents( 1 );
        List<Events.Event> events = result.getResult();
        assertEquals( events.size(), 1 );

        assertEquals( callback.pop( 1000 ), result.getSubscription() );
    }

}