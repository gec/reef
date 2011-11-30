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

import net.agileautomata.executor4s.Cancelable;
import org.junit.Test;
import org.totalgrid.reef.client.service.commands.CommandRequestHandler;
import org.totalgrid.reef.client.service.commands.CommandResultCallback;
import org.totalgrid.reef.clientapi.exceptions.ExpectationException;
import org.totalgrid.reef.integration.helpers.ReefConnectionTestBase;
import org.totalgrid.reef.loader.commons.LoaderServices;
import org.totalgrid.reef.proto.Commands;
import org.totalgrid.reef.proto.FEP;
import org.totalgrid.reef.proto.FEP.EndpointOwnership;
import org.totalgrid.reef.proto.FEP.Endpoint;
import org.totalgrid.reef.proto.Model;
import org.totalgrid.reef.proto.Model.Command;
import org.totalgrid.reef.proto.Model.CommandType;

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

        LoaderServices loader = client.getService( LoaderServices.class );
        try
        {
            cmd = Command.newBuilder().setName( "test.command" ).setDisplayName( "test.command" ).setType( CommandType.SETPOINT_INT ).build();

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
    }

    private void doCommandRequest( Command cmd, Model.ReefUUID endpointUuid ) throws Exception
    {
        Cancelable cancelable = null;
        Commands.CommandLock access = null;
        try
        {
            CommandRequestHandler handler = new CommandRequestHandler() {

                public void handleCommandRequest( Commands.CommandRequest cmdRequest, CommandResultCallback resultCallback )
                {
                    resultCallback.setCommandResult( Commands.CommandStatus.TOO_MANY_OPS );
                }
            };

            cancelable = helpers.bindCommandHandler( endpointUuid, handler );

            access = helpers.createCommandExecutionLock( cmd );

            Commands.CommandStatus status = helpers.executeCommandAsSetpoint( cmd, 100 );

            assertEquals( status, Commands.CommandStatus.TOO_MANY_OPS );

            List<Commands.UserCommandRequest> commands = helpers.getCommandHistory( cmd );
            assertEquals( commands.get( 0 ).getStatus(), Commands.CommandStatus.TOO_MANY_OPS );
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
