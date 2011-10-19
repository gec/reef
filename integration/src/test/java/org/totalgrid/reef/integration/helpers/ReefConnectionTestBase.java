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

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import net.agileautomata.executor4s.ExecutorService;
import net.agileautomata.executor4s.Executors;
import org.junit.After;
import org.junit.Before;

import org.totalgrid.reef.api.japi.ReefServiceException;
import org.totalgrid.reef.api.japi.client.impl.AMQPConnectionSettingImpl;

import org.totalgrid.reef.api.japi.client.rpc.AllScadaService;
import org.totalgrid.reef.api.japi.client.rpc.impl.AllScadaServiceJavaShimWrapper;
import org.totalgrid.reef.api.sapi.client.rest.Connection;
import org.totalgrid.reef.api.sapi.client.rest.impl.DefaultConnection;
import org.totalgrid.reef.api.sapi.impl.ReefServicesList;
import org.totalgrid.reef.broker.BrokerConnection;
import org.totalgrid.reef.broker.BrokerConnectionFactory;
import org.totalgrid.reef.broker.qpid.QpidBrokerConnectionFactory;
import org.totalgrid.reef.broker.qpid.QpidBrokerConnectionInfo;

/**
 * Base class for JUnit based integration tests run against the "live" system
 */
public class ReefConnectionTestBase
{
    private final boolean autoLogon;

    /**
     * connector to the bus, restarted for every test connected for
     */
    protected final BrokerConnectionFactory factory = new QpidBrokerConnectionFactory( getConnectionInfo() );

    protected final ExecutorService exe = Executors.newScheduledThreadPool( 4 );

    protected BrokerConnection broker;
    protected Connection connection;

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
    private QpidBrokerConnectionInfo getConnectionInfo()
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
            // we'll then throw an exception when trying to load from emtpy properties file
            throw new RuntimeException( e );
        }

        AMQPConnectionSettingImpl settings = new AMQPConnectionSettingImpl( props );

        return settings.asInfo();

    }

    @Before
    public void startBridge() throws InterruptedException, ReefServiceException
    {
        broker = factory.connect();

        connection = new DefaultConnection( ReefServicesList.getInstance(), broker, exe, 20000 );

        if ( autoLogon )
        {
            helpers = new AllScadaServiceJavaShimWrapper( connection.login( "system", "system" ).await() );
        }
        else
        {
            helpers = new AllScadaServiceJavaShimWrapper( connection.login( "" ) );
        }
    }

    @After
    public void stopBridge() throws InterruptedException, ReefServiceException
    {
        if ( broker != null )
            broker.disconnect();
        exe.shutdown();
    }
}
