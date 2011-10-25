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
import org.junit.Before;

import org.totalgrid.reef.api.japi.settings.AmqpSettings;
import org.totalgrid.reef.api.japi.ReefServiceException;

import org.totalgrid.reef.api.japi.settings.util.PropertyReader;
import org.totalgrid.reef.api.sapi.client.rest.Client;
import org.totalgrid.reef.client.ReefFactory;
import org.totalgrid.reef.client.rpc.AllScadaService;

import org.totalgrid.reef.api.sapi.client.rest.Connection;

/**
 * Base class for JUnit based integration tests run against the "live" system
 */
public class ReefConnectionTestBase
{
    private final boolean autoLogon;

    protected ReefFactory factory;

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
        try
        {
            AmqpSettings s = new AmqpSettings( PropertyReader.readFromFile( "../org.totalgrid.reef.test.cfg" ) );
            this.factory = new ReefFactory( s );
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

    @Before
    public void startBridge() throws InterruptedException, ReefServiceException
    {
        Connection connection = factory.connect();

        Client client;

        if ( autoLogon )
        {
            client = connection.login( "system", "system" ).await();
        }
        else
        {
            client = connection.login( "" );
        }
        helpers = client.getRpcInterface( AllScadaService.class );
    }

    @After
    public void stopBridge() throws InterruptedException, ReefServiceException
    {
        factory.terminate();
    }
}
