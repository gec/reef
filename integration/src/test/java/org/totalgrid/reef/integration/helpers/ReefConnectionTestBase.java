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
package org.totalgrid.reef.integration.helpers;

import org.junit.*;

import org.totalgrid.reef.japi.ReefServiceException;
import org.totalgrid.reef.japi.client.AMQPConnectionSettingImpl;
import org.totalgrid.reef.japi.client.AMQPConnectionSettings;
import org.totalgrid.reef.japi.client.Session;
import org.totalgrid.reef.japi.request.AllScadaService;
import org.totalgrid.reef.japi.request.impl.AllScadaServicePooledWrapper;
import org.totalgrid.reef.japi.request.impl.AuthTokenServicePooledWrapper;
import org.totalgrid.reef.japi.client.SessionExecutionPool;
import org.totalgrid.reef.messaging.javaclient.AMQPConnection;

import org.totalgrid.reef.japi.client.Connection;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Base class for JUnit based integration tests run against the "live" system
 */
public class ReefConnectionTestBase
{

    private final boolean autoLogon;

    /**
     * connector to the bus, restarted for every test connected for
     */
    protected final Connection connection = new AMQPConnection( getConnectionInfo(), 5000 );
    protected Session client;
    protected AllScadaService helpers;

    /**
     * Baseclass for junit integration tests, provides a Connection that is started and stopped with
     * every test case.
     * 
     * @param autoLogon
     *            If set we automatically acquire and set auth tokens for the client on every
     *            request
     */
    protected ReefConnectionTestBase( boolean autoLogon )
    {
        this.autoLogon = autoLogon;
    }

    /**
     * defaults autoLogon to true
     */
    protected ReefConnectionTestBase()
    {
        this( true );
    }

    /**
     * gets the ip of the qpid server, defaults to 127.0.0.1 but can be override with java property
     * -Dreef_node_ip=192.168.100.10
     */
    private AMQPConnectionSettings getConnectionInfo()
    {
        Properties props = new Properties();

        try
        {
            FileInputStream fis = new FileInputStream( "../org.totalgrid.reef.amqp.cfg" );
            props.load( fis );
            fis.close();
        }
        catch ( IOException e )
        {
            e.printStackTrace();
            // we'll then throw an exception when trying to load from emtpy properties file
        }

        return new AMQPConnectionSettingImpl( props );
    }

    @Before
    public void startBridge() throws InterruptedException, ReefServiceException
    {
        connection.connect( 5000 );
        client = connection.newSession();
        SessionExecutionPool pool = connection.newSessionPool();
        String authToken = new AuthTokenServicePooledWrapper( pool ).createNewAuthorizationToken( "system", "system" );
        if ( autoLogon )
        {
            client.getDefaultHeaders().setAuthToken( authToken );
            helpers = new AllScadaServicePooledWrapper( pool, authToken );
        }
        else
        {
            helpers = new AllScadaServicePooledWrapper( pool, "" );
        }
    }

    @After
    public void stopBridge() throws InterruptedException, ReefServiceException
    {
        client.close();
        connection.disconnect( 5000 );
    }
}
