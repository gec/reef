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
import org.totalgrid.reef.client.SubscriptionBinding;
import org.totalgrid.reef.client.service.command.CommandRequestHandler;
import org.totalgrid.reef.client.service.command.CommandResultCallback;
import org.totalgrid.reef.client.exception.ExpectationException;
import org.totalgrid.reef.integration.helpers.MockSubscriptionBindingListener;
import org.totalgrid.reef.integration.helpers.ReefConnectionTestBase;
import org.totalgrid.reef.loader.commons.LoaderServices;
import org.totalgrid.reef.client.service.proto.Commands;
import org.totalgrid.reef.client.service.proto.FEP;
import org.totalgrid.reef.client.service.proto.FEP.EndpointOwnership;
import org.totalgrid.reef.client.service.proto.FEP.Endpoint;
import org.totalgrid.reef.client.service.proto.Model;
import org.totalgrid.reef.client.service.proto.Model.Command;
import org.totalgrid.reef.client.service.proto.Model.CommandType;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class TestCommandHandler extends ReefConnectionTestBase
{

    @Test
    public void bindCommandHandler() throws Exception
    {

        Command cmd = null;
        Endpoint endpoint = null;
        FEP.EndpointConnection conn = null;

        MockSubscriptionBindingListener bindings = new MockSubscriptionBindingListener();
        client.addSubscriptionCreationListener( bindings );

        LoaderServices loader = client.getService( LoaderServices.class );
        try
        {
            cmd = Command.newBuilder().setName( "test.command" ).setDisplayName( "test.command" ).setType( CommandType.SETPOINT_STRING ).build();

            EndpointOwnership owners = EndpointOwnership.newBuilder().addCommands( "test.command" ).build();
            Endpoint newEndpoint = Endpoint.newBuilder().setName( "Test.Endpoint" ).setProtocol( "null" ).setOwnerships( owners ).build();

            loader.put( cmd ).await();
            endpoint = loader.put( newEndpoint ).await();

            int i = 0;
            while ( true )
            {
                conn = helpers.getEndpointConnectionByUuid( endpoint.getUuid() );
                if ( !conn.getRouting().getServiceRoutingKey().equals( "" ) )
                    break;
                if ( i++ > 5 )
                    throw new ExpectationException( "meas proc never came online" );
                Thread.sleep( 250 );
            }
            helpers.alterEndpointConnectionState( conn.getId(), FEP.EndpointConnection.State.COMMS_UP );

            doCommandRequest( cmd, endpoint.getUuid() );
        }
        finally
        {
            if ( endpoint != null )
            {
                helpers.disableEndpointConnection( endpoint.getUuid() );
                helpers.alterEndpointConnectionState( conn.getId(), FEP.EndpointConnection.State.COMMS_DOWN );
                loader.delete( endpoint ).await();
            }
            if ( cmd != null )
                loader.delete( cmd ).await();
        }
        assertEquals( 1, bindings.size() );
    }

    private void doCommandRequest( Command cmd, Model.ReefUUID endpointUuid ) throws Exception
    {
        SubscriptionBinding cancelable = null;
        Commands.CommandLock access = null;
        try
        {
            CommandRequestHandler handler = new CommandRequestHandler() {

                public void handleCommandRequest( Commands.CommandRequest cmdRequest, CommandResultCallback resultCallback )
                {
                    assertEquals( cmdRequest.getStringVal(), "TestString" );
                    resultCallback.setCommandResult( Commands.CommandStatus.TOO_MANY_OPS, "Extra Error Message" );
                }
            };

            cancelable = helpers.bindCommandHandler( endpointUuid, handler );

            access = helpers.createCommandExecutionLock( cmd );

            Commands.CommandResult status = helpers.executeCommandAsSetpoint( cmd, "TestString" );

            assertEquals( Commands.CommandStatus.TOO_MANY_OPS, status.getStatus() );

            List<Commands.UserCommandRequest> commands = helpers.getCommandHistory( cmd );
            Commands.CommandResult result = commands.get( 0 ).getResult();
            assertEquals( Commands.CommandStatus.TOO_MANY_OPS, result.getStatus() );
            assertEquals( "Extra Error Message", result.getErrorMessage() );
        }
        finally
        {
            if ( access != null )
                helpers.deleteCommandLock( access );
            if ( cancelable != null )
                cancelable.cancel();
        }
    }

}
