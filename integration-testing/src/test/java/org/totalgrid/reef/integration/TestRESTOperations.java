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
import static org.junit.Assert.*;

import com.google.protobuf.ByteString;

import org.totalgrid.reef.client.Promise;
import org.totalgrid.reef.client.SubscriptionResult;
import org.totalgrid.reef.client.exception.ReefServiceException;
import org.totalgrid.reef.client.proto.Envelope;
import org.totalgrid.reef.client.service.RESTOperations;
import org.totalgrid.reef.client.service.async.RESTOperationsAsync;
import org.totalgrid.reef.client.service.proto.Model;
import org.totalgrid.reef.integration.helpers.MockSubscriptionEventAcceptor;
import org.totalgrid.reef.integration.helpers.ReefConnectionTestBase;

import java.lang.reflect.Array;
import java.util.LinkedList;
import java.util.List;

public class TestRESTOperations extends ReefConnectionTestBase
{

    @Test
    public void testGetPutDelete() throws Exception
    {
        RESTOperations ops = client.getService( RESTOperations.class );

        Model.ConfigFile cf =
            Model.ConfigFile.newBuilder().setName( "TestFile" ).setMimeType( "test" ).setFile( ByteString.copyFromUtf8( "test" ) ).build();

        // put the config file
        Model.ConfigFile withUuid = ops.putOne( cf );
        assertTrue( withUuid.hasUuid() );

        // check that if we try to retrieve for that config file we get one we just put
        assertEquals( ops.getOne( cf ), withUuid );

        // ask for a list of matching files (will be only one)
        List<Model.ConfigFile> files = ops.getMany( cf );
        assertEquals( 1, files.size() );
        assertEquals( withUuid, files.get( 0 ) );

        // delete the config file
        ops.deleteOne( withUuid );

        // a search will now return null
        Model.ConfigFile notFound = ops.findOne( cf );
        assertNull( notFound );

        try
        {
            ops.getOne( cf );
            fail( "Should throw exception if getOne returns 0" );
        }
        catch ( ReefServiceException e )
        {
            assertTrue( true );
        }
    }

    @Test
    public void testGetAsync() throws Exception
    {
        RESTOperationsAsync ops = client.getService( RESTOperationsAsync.class );

        Model.ConfigFile cf =
            Model.ConfigFile.newBuilder().setName( "TestFile2" ).setMimeType( "test2" ).setFile( ByteString.copyFromUtf8( "test2" ) ).build();

        Promise<Model.ConfigFile> promise = ops.getOne( cf );
        try
        {
            // notice exception isn't throw until await is called
            promise.await();
            fail( "Should throw exception if getOne returns 0" );
        }
        catch ( ReefServiceException e )
        {
            assertTrue( true );
        }
    }

    @Test
    public void testSubscribe() throws Exception
    {
        RESTOperations ops = client.getService( RESTOperations.class );

        Model.ConfigFile cf =
            Model.ConfigFile.newBuilder().setName( "TestFile3" ).setMimeType( "test2" ).setFile( ByteString.copyFromUtf8( "test2" ) ).build();

        // subscribe to config files with name TestFile3
        SubscriptionResult<List<Model.ConfigFile>, Model.ConfigFile> subResult = ops.subscribeMany( cf );
        assertEquals( 0, subResult.getResult().size() );

        // start the subscription and store the results
        MockSubscriptionEventAcceptor<Model.ConfigFile> mock = new MockSubscriptionEventAcceptor<Model.ConfigFile>( true );
        subResult.getSubscription().start( mock );

        // do an add and a delete
        ops.putOne( cf );
        ops.deleteOne( cf );

        // check that we get both subscription updates
        assertEquals( Envelope.SubscriptionEventType.ADDED, mock.pop( 5000 ).getEventType() );
        assertEquals( Envelope.SubscriptionEventType.REMOVED, mock.pop( 5000 ).getEventType() );
    }
}
