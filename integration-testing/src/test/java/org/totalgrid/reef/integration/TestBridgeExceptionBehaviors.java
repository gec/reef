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

import static org.junit.Assert.fail;

import org.junit.Test;


public class TestBridgeExceptionBehaviors
{
    @Test
    public void throwsExceptionWhenNotStarted()
    {
        // TODO: re-implement test
        /*ConnectionSettings settings = new AMQPConnectionSettingImpl( "127.0.0.1", 5672, "guest", "guest", "test", false, "", "" );
        Connection connection = new AMQPConnection( settings, 5000 );

        try
        {
            connection.newSession();
            fail( "Should throw exception when not connected" );
        }
        catch ( Exception ex )
        {
        }*/
    }
}
