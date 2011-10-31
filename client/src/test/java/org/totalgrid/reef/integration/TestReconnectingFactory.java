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
import org.totalgrid.reef.client.ReefReconnectingFactory;
import org.totalgrid.reef.client.rpc.MeasurementService;
import org.totalgrid.reef.clientapi.Client;
import org.totalgrid.reef.clientapi.Connection;
import org.totalgrid.reef.clientapi.ConnectionWatcher;
import org.totalgrid.reef.clientapi.ReconnectingConnectionFactory;
import org.totalgrid.reef.clientapi.settings.AmqpSettings;
import org.totalgrid.reef.clientapi.settings.UserSettings;
import org.totalgrid.reef.clientapi.settings.util.PropertyReader;
import org.totalgrid.reef.util.EmptySyncVar;
import org.totalgrid.reef.util.SyncVar;

public class TestReconnectingFactory
{

    @Test
    public void testConnect() throws Exception
    {
        final AmqpSettings s = new AmqpSettings( PropertyReader.readFromFile( "../org.totalgrid.reef.test.cfg" ) );
        final UserSettings userSettings = new UserSettings( PropertyReader.readFromFile( "../org.totalgrid.reef.test.cfg" ) );

        ReconnectingConnectionFactory factory = new ReefReconnectingFactory( s, 100, 500 );

        final SyncVar closed = new SyncVar<Boolean>( Boolean.FALSE );
        final SyncVar opened = new SyncVar<Boolean>( Boolean.FALSE );
        final SyncVar conn = new EmptySyncVar<Connection>();

        factory.addConnectionWatcher( new ConnectionWatcher() {
            public void onConnectionClosed( boolean expected )
            {
                closed.update( Boolean.TRUE );
            }

            public void onConnectionOpened( Connection connection )
            {
                opened.update( Boolean.TRUE );
                conn.update( connection );

            }
        } );

        factory.start();

        opened.waitUntil( Boolean.TRUE );

        Connection c = (Connection)conn.current();
        Client client = c.login( userSettings );
        client.getRpcInterface( MeasurementService.class );

        factory.stop();

        closed.waitUntil( Boolean.TRUE );
    }
}
