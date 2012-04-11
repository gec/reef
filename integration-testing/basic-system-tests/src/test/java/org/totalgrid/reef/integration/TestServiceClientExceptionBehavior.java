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

import org.totalgrid.reef.client.exception.ReefServiceException;
import org.totalgrid.reef.client.exception.ServiceIOException;
import org.totalgrid.reef.client.exception.UnauthorizedException;
import org.totalgrid.reef.integration.helpers.ReefConnectionTestBase;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings("unchecked")
public class TestServiceClientExceptionBehavior extends ReefConnectionTestBase
{

    @Test
    public void getAllEntities()
    {
        // only test terminate functionality when running a remote-test
        if ( System.getProperty( "remote-test" ) != null )
        {
            factory.terminate();

            try
            {
                helpers.getApplications();
                fail( "Closed client should throw exception" );
            }
            catch ( ServiceIOException ex )
            {
                assertTrue( true );
            }
            catch ( ReefServiceException ex )
            {
                fail( "Exception should have been serviceIO" );
            }
        }
    }

    @Test
    public void settingNullAuthTokenThrows()
    {
        try
        {
            client.setHeaders( client.getHeaders().setAuthToken( null ) );
            fail( "Null authToken should cause error" );
        }
        catch ( IllegalArgumentException e )
        {
            assertTrue( true );
        }

    }

    @Test
    public void logout() throws ReefServiceException
    {
        helpers.getAgents();

        client.logout();
        try
        {
            helpers.getAgents();

            fail( "Since we logged out another request should cause auth failure" );
        }
        catch ( UnauthorizedException e )
        {
            assertTrue( true );
        }

    }
}
