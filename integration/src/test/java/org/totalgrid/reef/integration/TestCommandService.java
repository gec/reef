/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Green Energy Corp licenses this file
 * to you under the GNU Affero General Public License Version 3.0
 * (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.integration;

import org.junit.*;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import org.totalgrid.reef.api.Envelope;
import org.totalgrid.reef.proto.Commands.*;
import org.totalgrid.reef.proto.Model.Command;

import java.util.List;

import org.totalgrid.reef.api.ReefServiceException;


import org.totalgrid.reef.integration.helpers.*;

@SuppressWarnings("unchecked")
public class TestCommandService extends JavaBridgeTestBase {

    /**
     * Test that some command names are returned from the commands service
     */
    @Test
    public void someCommandsReturned() throws ReefServiceException {
        List<Command> commands = SampleRequests.getAllCommands(client);
        assertTrue(commands.size() > 0);
    }

    /**
     * Clear out all accesses if any exist
     */
    @Test
    public void clearAllAccesses() throws ReefServiceException {
        SampleRequests.clearAllCommandAccess(client);
        List<CommandAccess> noAccess = SampleRequests.findCommandAccess(client, "*");
        assertEquals(noAccess.size(), 0);
    }

    /**
     * Test that some command names are returned from the commands service
     */
    @Test
    public void testCommandFailsWithoutSelect() throws ReefServiceException {
        Command c = SampleRequests.getAllCommands(client).get(0);
        try {
            SampleRequests.executeControl(client, c);
            fail("should throw exception");
        } catch (ReefServiceException pse) {
            assertEquals(Envelope.Status.BAD_REQUEST, pse.getStatus());
        }
    }

    /**
     * Test that command access request can be get, put, and deleted
     */
    @Test
    public void testGetPutDeleteCommandAccess() throws ReefServiceException {
        Command cmd = SampleRequests.getAllCommands(client).get(0);

        // would allow user "user1" to exclusively execute commands for 5000 ms.
        SampleRequests.putCommandAccess(client, cmd, 5000, true);
        // removes the command access request by name
        SampleRequests.deleteCommandAccess(client, cmd.getName());
    }

    /**
     * Test that some command names are returned from the commands service
     */
    @Test
    public void testCommandSelectAndExecute() throws ReefServiceException {
        Command cmd = SampleRequests.getAllCommands(client).get(0);

        CommandAccess accessResponse = SampleRequests.putCommandAccess(client, cmd, 5000, true);
        assertTrue(accessResponse.getExpireTime() > 0);
        UserCommandRequest cmdResponse = SampleRequests.executeControl(client, cmd);
        assertEquals(cmdResponse.getStatus(), CommandStatus.EXECUTING);

        // delete select by reference (UID)
        SampleRequests.deleteCommandAccess(client, accessResponse);
    }

    /**
     * Test that you cannot select the same command (or subset of commands) at the same time
     */
    @Test
    public void testMultiSelect() throws ReefServiceException {

        List<Command> cmds = SampleRequests.getAllCommands(client);

        CommandAccess accessResponse1 = SampleRequests.putCommandAccess(client, cmds.subList(0, 3), 5000, true);
        assertTrue(accessResponse1.getExpireTime() > 0);

        try {
            SampleRequests.putCommandAccess(client, cmds.subList(0, 2), 5000, true);
            fail("should have failed because we already selected");
        } catch (ReefServiceException pse) {
            assertEquals(Envelope.Status.UNAUTHORIZED, pse.getStatus());
        }

        SampleRequests.deleteCommandAccess(client, accessResponse1);
    }

    /**
     * Test that we can both search and delete by command name (new since 0.2.1)
     */
    @Test
    public void testSearchingAndDeletingSelectByCommandName() throws ReefServiceException {

        List<Command> cmds = SampleRequests.getAllCommands(client);
        Command cmd1 = cmds.get(0);
        Command cmd2 = cmds.get(1);

        List<CommandAccess> noAccess = SampleRequests.findCommandAccess(client, cmd1.getName());
        assertEquals(noAccess.size(), 0);

        SampleRequests.putCommandAccess(client, cmd1, 5000, true);
        SampleRequests.putCommandAccess(client, cmd2, 5000, true);

        List<CommandAccess> foundAccesses = SampleRequests.findCommandAccess(client, cmd1.getName());
        assertEquals(foundAccesses.size(), 1);
        assertEquals(foundAccesses.get(0).getCommandsCount(), 1);
        assertEquals(foundAccesses.get(0).getCommands(0), cmd1.getName());

        SampleRequests.deleteCommandAccess(client, cmd1.getName());

        List<CommandAccess> foundAccesses2 = SampleRequests.findCommandAccess(client, cmd2.getName());
        assertEquals(foundAccesses2.size(), 1);
        assertEquals(foundAccesses2.get(0).getCommandsCount(), 1);
        assertEquals(foundAccesses2.get(0).getCommands(0), cmd2.getName());

        SampleRequests.deleteCommandAccess(client, cmd2.getName());

        List<CommandAccess> noAccess2 = SampleRequests.findCommandAccess(client, cmd2.getName());
        assertEquals(noAccess2.size(), 0);
    }

    /**
     * Test to recreate issue discovered by denver
     */
    @Test
    public void testCommandSelectExecuteDeleteExecuteDelete() throws ReefServiceException {

        Command cmd = SampleRequests.getAllCommands(client).get(0);

        // select
        CommandAccess accessResponse = SampleRequests.putCommandAccess(client, cmd, 5000, true);
        assertTrue(accessResponse.getExpireTime() > 0);

        // execute
        UserCommandRequest cmdResponse = SampleRequests.executeControl(client, cmd);
        assertEquals(cmdResponse.getStatus(), CommandStatus.EXECUTING);

        // delete
        SampleRequests.deleteCommandAccess(client, cmd.getName());

        try {
            // second execute fails
            SampleRequests.executeControl(client, cmd);
            fail("should throw exception");
        } catch (ReefServiceException pse) {
            assertEquals(Envelope.Status.BAD_REQUEST, pse.getStatus());
        }

        try {
            // second delete fails
            SampleRequests.deleteCommandAccess(client, cmd.getName());
            fail("should throw exception");
        } catch (ReefServiceException pse) {
            assertEquals(Envelope.Status.UNEXPECTED_RESPONSE, pse.getStatus());
        }
    }
}
