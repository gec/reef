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
import org.totalgrid.reef.app.ApplicationConnectionListener;
import org.totalgrid.reef.app.ApplicationConnectionManager;
import org.totalgrid.reef.app.impl.ApplicationConnectionManagerImpl;
import org.totalgrid.reef.client.Client;
import org.totalgrid.reef.client.Connection;
import org.totalgrid.reef.client.exception.ServiceIOException;
import org.totalgrid.reef.client.service.ApplicationService;
import org.totalgrid.reef.client.settings.AmqpSettings;
import org.totalgrid.reef.client.settings.NodeSettings;
import org.totalgrid.reef.client.settings.UserSettings;
import org.totalgrid.reef.client.settings.util.PropertyReader;
import org.totalgrid.reef.util.EmptySyncVar;
import org.totalgrid.reef.util.SyncVar;

import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("unchecked")
public class TestApplicationConnectionManagerFactory
{

    class TestListener implements ApplicationConnectionListener
    {
        final SyncVar opened = new SyncVar<Boolean>( Boolean.FALSE );
        final SyncVar errors = new EmptySyncVar<String>();

        public void onConnectionStatusChanged( boolean isConnected )
        {
            opened.update( isConnected );
        }

        public void onConnectionError( String msg )
        {
            errors.update( msg );
        }
    }

    @Test
    public void testConnect() throws Exception
    {
        // don't run connect test unless were trying to talk to a real amqp node
        if ( System.getProperty( "remote-test" ) == null )
            return;

        Properties properties = PropertyReader.readFromFile( "../../org.totalgrid.reef.test.cfg" );
        final AmqpSettings amqpSettings = new AmqpSettings( properties );
        final UserSettings userSettings = new UserSettings( properties );
        final NodeSettings nodeSettings = new NodeSettings( properties );

        ApplicationConnectionManager factory = new ApplicationConnectionManagerImpl( amqpSettings, userSettings, nodeSettings, "test-app", "TEST" );

        TestListener listener = new TestListener();
        factory.addConnectionListener( listener );

        assertEquals( factory.isConnected(), false );

        factory.start();

        listener.opened.waitUntil( Boolean.TRUE );

        assertEquals( factory.isConnected(), true );

        Connection c = factory.getConnection();
        Client client = c.login( userSettings );
        ApplicationService appService = client.getService( ApplicationService.class );

        appService.getApplicationByName( nodeSettings.getDefaultNodeName() + "-test-app" );

        factory.stop();

        try
        {
            factory.getConnection();
            assertTrue( "Can't get connection when disconnected", false );
        }
        catch ( ServiceIOException sie )
        {
            assertTrue( true );
        }

        assertEquals( factory.isConnected(), false );

        assertEquals( listener.opened.current(), Boolean.FALSE );


        try
        {
            factory.start();
            assertTrue( "Can't restart a connection manager", false );
        }
        catch ( IllegalArgumentException iae )
        {
            assertTrue( true );
        }

        try
        {
            appService.getApplicationByName( nodeSettings.getDefaultNodeName() + "-test-app" );
            assertTrue( "Client should fail fast and hard if connection is shutdown", false );
        }
        catch ( ServiceIOException sie )
        {
            assertTrue( true );
        }
    }
}
