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

import org.junit.Test;
import org.totalgrid.reef.integration.helpers.ReefConnectionTestBase;
import org.totalgrid.reef.japi.ExpectationException;
import org.totalgrid.reef.japi.request.command.CommandRequestHandler;
import org.totalgrid.reef.japi.request.command.CommandResultCallback;
import org.totalgrid.reef.proto.Commands;
import org.totalgrid.reef.proto.FEP;
import org.totalgrid.reef.proto.FEP.EndpointOwnership;
import org.totalgrid.reef.proto.FEP.CommEndpointConfig;
import org.totalgrid.reef.proto.Model;
import org.totalgrid.reef.proto.Model.Command;
import org.totalgrid.reef.proto.Model.CommandType;
import org.totalgrid.reef.util.Cancelable;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class TestCommandHandler extends ReefConnectionTestBase
{

    @Test
    public void bindCommandHandler() throws Exception
    {

        Command cmd = null;
        CommEndpointConfig endpoint = null;

        try
        {
            cmd = Command.newBuilder().setName( "test.command" ).setDisplayName( "test.command" ).setType( CommandType.SETPOINT_INT ).build();

            EndpointOwnership owners = EndpointOwnership.newBuilder().addCommands( "test.command" ).build();
            CommEndpointConfig newEndpoint =
                CommEndpointConfig.newBuilder().setName( "Test.Endpoint" ).setProtocol( "null" ).setOwnerships( owners ).build();

            client.put( cmd ).await();
            endpoint = client.put( newEndpoint ).await().expectOne();

            int i = 0;
            while ( true )
            {
                String key = helpers.getEndpointConnection( endpoint.getUuid() ).getRouting().getServiceRoutingKey();
                if ( !key.equals( "" ) )
                    break;
                if ( i++ > 5 )
                    throw new ExpectationException( "meas proc never came online" );
                Thread.sleep( 250 );
            }
            setCommState( endpoint.getUuid(), FEP.CommEndpointConnection.State.COMMS_UP );

            doCommandRequest( cmd, endpoint.getUuid() );
        }
        finally
        {
            if ( endpoint != null )
            {
                helpers.disableEndpointConnection( endpoint.getUuid() );
                setCommState( endpoint.getUuid(), FEP.CommEndpointConnection.State.COMMS_DOWN );
                client.delete( endpoint ).await().expectOne();
            }
            if ( cmd != null )
                client.delete( cmd ).await().expectOne();
        }
    }

    private void doCommandRequest( Command cmd, Model.ReefUUID endpointUuid ) throws Exception
    {
        Cancelable cancelable = null;
        Commands.CommandAccess access = null;
        try
        {
            CommandRequestHandler handler = new CommandRequestHandler() {
                @Override
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

    private void setCommState( Model.ReefUUID endpointUuid, FEP.CommEndpointConnection.State state ) throws Exception
    {
        FEP.CommEndpointConnection conn =
            helpers.getEndpointConnection( endpointUuid ).toBuilder().clearEnabled().clearEndpoint().clearFrontEnd().clearRouting().setState( state )
                    .build();
        client.put( conn ).await().expectOne();
    }
}
