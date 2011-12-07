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

import org.junit.Test;
import org.totalgrid.reef.client.SubscriptionBinding;
import org.totalgrid.reef.client.SubscriptionCreationListener;
import org.totalgrid.reef.client.exception.ReefServiceException;
import org.totalgrid.reef.client.service.*;
import org.totalgrid.reef.integration.helpers.BlockingQueue;
import org.totalgrid.reef.integration.helpers.ReefConnectionTestBase;
import org.totalgrid.reef.integration.helpers.MockSubscriptionEventAcceptor;
import org.totalgrid.reef.client.SubscriptionResult;
import org.totalgrid.reef.client.service.proto.Alarms;
import org.totalgrid.reef.client.service.proto.Alarms.Alarm;
import org.totalgrid.reef.client.service.proto.Events;
import org.totalgrid.reef.client.service.proto.Model;
import org.totalgrid.reef.client.service.proto.Utils;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

public class TestEventService extends ReefConnectionTestBase
{
    // name of the entity we'll attach all of the test events to
    private final String entityForEvents = "StaticSubstation.Line02.Current";

    @Test
    public void validateLongEventConfigStrings() throws ReefServiceException
    {
        EventConfigService configService = helpers;
        EventPublishingService es = helpers;

        StringBuilder sb = new StringBuilder();
        for ( int i = 0; i < 1000; i++ )
            sb.append( "a" );
        String longString = sb.toString();

        Alarms.EventConfig config = configService.setEventConfigAsEvent( "Test.EventSuperLong", 1, longString );
        assertEquals( longString, config.getResource() );

        Events.Event event = es.publishEvent( "Test.EventSuperLong", "Tests", new LinkedList<Utils.Attribute>() );
        assertEquals( longString, event.getRendered() );
        assertTrue( event.hasId() );
        assertNotSame( 0, event.getId().getValue().length() );

        assertTrue( event.hasTime() );
        assertTrue( event.getTime() > 0 );

        configService.deleteEventConfig( config );
    }

    @Test
    public void prepareEvents() throws ReefServiceException
    {
        EventPublishingService es = helpers;
        EntityService entityService = helpers;
        EventConfigService configService = helpers;

        EventService eventService = helpers;

        // make an event type for our test events
        configService.setEventConfigAsEvent( "Test.Event", 1, "Event" );

        Model.Entity entity = entityService.getEntityByName( entityForEvents );

        // populate some events
        for ( int i = 0; i < 15; i++ )
        {
            Events.Event e = es.publishEvent( "Test.Event", "Tests", entity.getUuid() );
            assertTrue( e.hasId() );
            assertNotSame( 0, e.getId().getValue().length() );
            assertTrue( e.hasEntity() );
            assertTrue( e.getEntity().hasUuid() );
            assertNotSame( 0, e.getEntity().getUuid().getValue().length() );

            Events.Event e2 = eventService.getEventById( e.getId() );
            assertEquals( e2, e );

            assertTrue( e.hasTime() );
            assertTrue( e.getTime() > 0 );
        }
    }

    @Test
    public void getRecentEvents() throws ReefServiceException
    {
        EventService es = helpers;
        List<Events.Event> events = es.getRecentEvents( 10 );
        assertEquals( events.size(), 10 );
    }

    @Test
    public void subscribeEvents() throws ReefServiceException, InterruptedException
    {

        MockSubscriptionEventAcceptor<Events.Event> mock = new MockSubscriptionEventAcceptor<Events.Event>( true );

        EventService es = helpers;

        List<String> types = Arrays.asList( "Test.Event" );

        SubscriptionResult<List<Events.Event>, Events.Event> events = es.subscribeToRecentEvents( types, 10 );
        assertEquals( events.getResult().size(), 10 );

        EventPublishingService pub = helpers;

        Events.Event pubEvent = pub.publishEvent( "Test.Event", "Tests", getUUID( entityForEvents ) );

        events.getSubscription().start( mock );

        Events.Event subEvent = mock.pop( 1000 ).getValue();

        assertTrue( subEvent.hasEntity() );
        assertTrue( subEvent.getEntity().hasUuid() );
        assertNotSame( 0, subEvent.getEntity().getUuid().getValue().length() );
        assertEquals( pubEvent, subEvent );
    }

    private Model.ReefUUID getUUID( String name ) throws ReefServiceException
    {
        EntityService es = helpers;
        Model.Entity e = es.getEntityByName( name );
        return e.getUuid();
    }

    @Test
    public void prepareAlarms() throws ReefServiceException
    {

        // make an event type for our test alarms
        EventConfigService configService = helpers;
        configService.setEventConfigAsAlarm( "Test.Alarm", 1, "Alarm", true );

        EventPublishingService es = helpers;

        // populate some alarms
        for ( int i = 0; i < 5; i++ )
        {
            es.publishEvent( "Test.Alarm", "Tests", getUUID( entityForEvents ) );
        }
    }

    @Test
    public void subscribeAlarms() throws ReefServiceException, InterruptedException
    {

        MockSubscriptionEventAcceptor<Alarm> mock = new MockSubscriptionEventAcceptor<Alarm>( true );

        AlarmService as = helpers;

        SubscriptionResult<List<Alarm>, Alarm> result = as.subscribeToActiveAlarms( 2 );
        List<Alarm> events = result.getResult();
        assertEquals( events.size(), 2 );

        EventPublishingService pub = helpers;

        pub.publishEvent( "Test.Alarm", "Tests", getUUID( entityForEvents ) );

        result.getSubscription().start( mock );
        mock.pop( 1000 );
    }

    @Test
    public void subscriptionCreationCallback() throws ReefServiceException, InterruptedException
    {

        final BlockingQueue<SubscriptionBinding> callback = new BlockingQueue<SubscriptionBinding>();

        EventService es = helpers;

        es.addSubscriptionCreationListener( new SubscriptionCreationListener() {
            @Override
            public void onSubscriptionCreated( SubscriptionBinding sub )
            {
                callback.push( sub );
            }
        } );

        SubscriptionResult<List<Events.Event>, Events.Event> result = es.subscribeToRecentEvents( 1 );
        List<Events.Event> events = result.getResult();
        assertEquals( events.size(), 1 );

        assertEquals( callback.pop( 1000 ).getId(), result.getSubscription().getId() );
    }


    @Test
    public void cleanupEventConfigs() throws ReefServiceException
    {
        EventConfigService configService = helpers;
        configService.deleteEventConfig( configService.getEventConfigurationByType( "Test.Event" ) );
        configService.deleteEventConfig( configService.getEventConfigurationByType( "Test.Alarm" ) );
    }

}