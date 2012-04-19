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
package org.totalgrid.reef.integration.helpers;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;

import org.totalgrid.reef.client.ConnectionFactory;
import org.totalgrid.reef.client.sapi.rpc.impl.util.ModelPreparer;
import org.totalgrid.reef.client.service.list.ReefServices;
import org.totalgrid.reef.client.Client;
import org.totalgrid.reef.client.Connection;
import org.totalgrid.reef.client.SubscriptionBinding;
import org.totalgrid.reef.client.settings.AmqpSettings;
import org.totalgrid.reef.client.exception.ReefServiceException;

import org.totalgrid.reef.client.settings.UserSettings;
import org.totalgrid.reef.client.settings.util.PropertyReader;
import org.totalgrid.reef.client.factory.ReefConnectionFactory;
import org.totalgrid.reef.client.service.AllScadaService;
import org.totalgrid.reef.loader.commons.LoaderServicesList;
import org.totalgrid.reef.standalone.InMemoryNode;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Base class for JUnit based integration tests run against the "live" system
 */
public class ReefConnectionTestBase
{
    private final boolean autoLogon;

    protected ConnectionFactory factory;

    protected Client client;

    protected final MockSubscriptionBindingListener bindingListener = new MockSubscriptionBindingListener();

    protected AllScadaService helpers;

    protected List<SubscriptionBinding> subscriptions = new LinkedList<SubscriptionBinding>();

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
        try
        {
            if ( System.getProperty( "remote-test" ) != null )
            {
                AmqpSettings s = new AmqpSettings( PropertyReader.readFromFile( "../../org.totalgrid.reef.test.cfg" ) );
                this.factory = ReefConnectionFactory.defaultFactory( s, new ReefServices() );

            }
            else
            {

                InMemoryNode.initialize( "../../standalone-node.cfg", true );
                this.factory = InMemoryNode.connectionFactory();
            }
        }
        catch ( Exception ex )
        {
            throw new RuntimeException( ex );
        }
    }

    /**
     * defaults autoLogon to true
     */
    protected ReefConnectionTestBase()
    {
        this( true );
    }

    @AfterClass
    public static void shutdown()
    {
        if ( System.getProperty( "remote-test" ) != null )
        {
            InMemoryNode.startShutdown();
        }
    }

    @Before
    public void startBridge() throws InterruptedException, ReefServiceException, IOException
    {
        Connection connection = factory.connect();

        if ( autoLogon )
        {
            UserSettings userSettings = new UserSettings( PropertyReader.readFromFile( "../../org.totalgrid.reef.test.cfg" ) );
            client = connection.login( userSettings );
        }
        else
        {
            client = connection.createClient( "" );
        }
        connection.addServicesList( new LoaderServicesList() );
        client.addSubscriptionCreationListener( bindingListener );
        helpers = client.getService( AllScadaService.class );

        String fileName = "../../assemblies/assembly-common/filtered-resources/samples/integration/config.xml";
        ModelPreparer.load( fileName, client );
    }

    @After
    public void stopBridge() throws InterruptedException, ReefServiceException
    {
        bindingListener.cancelAll();
        factory.terminate();
    }
}
