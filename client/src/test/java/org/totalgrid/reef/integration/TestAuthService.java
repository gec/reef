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

import org.totalgrid.reef.api.japi.Envelope;

import org.totalgrid.reef.client.rpc.impl.AllScadaServiceJavaShimWrapper;
import org.totalgrid.reef.integration.helpers.ReefConnectionTestBase;
import org.totalgrid.reef.api.japi.ReefServiceException;
import org.totalgrid.reef.api.japi.UnauthorizedException;
import org.totalgrid.reef.client.rpc.AuthTokenService;
import org.totalgrid.reef.client.rpc.PointService;

public class TestAuthService extends ReefConnectionTestBase
{
    public TestAuthService()
    {
        // disable autoLogin
        super( false );
    }

    @Test
    public void successfulLogin() throws ReefServiceException
    {
        AuthTokenService as = helpers;
        as.createNewAuthorizationToken( "system", "system" );
    }

    @Test
    public void demonstateAuthTokenNeeded() throws ReefServiceException
    {

        try
        {
            // will fail because we don't havent logged in to get auth tokens
            PointService ps = helpers;
            ps.getAllPoints();
            assertTrue( false );
        }
        catch ( UnauthorizedException pse )
        {
            assertEquals( Envelope.Status.UNAUTHORIZED, pse.getStatus() );
        }
        AuthTokenService as = helpers;
        // logon as all permission user
        String token = as.createNewAuthorizationToken( "system", "system" );
        helpers = new AllScadaServiceJavaShimWrapper( connection.login( token ) );

        PointService ps = helpers;
        // request will now not be rejected
        ps.getAllPoints();
    }
}
