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

import org.totalgrid.reef.japi.Envelope;
import org.totalgrid.reef.japi.request.CommandService;
import org.totalgrid.reef.japi.ReefServiceException;
import org.totalgrid.reef.proto.Commands.*;
import org.totalgrid.reef.proto.Model.Command;

import java.util.List;


import org.totalgrid.reef.integration.helpers.*;

@SuppressWarnings("unchecked")
public class TestCommandService extends ReefConnectionTestBase {


    /**
     * Test that some command names are returned from the commands service
     */
    @Test
    public void someCommandsReturned() throws ReefServiceException {

        CommandService cs = helpers;
        List<Command> commands = cs.getCommands();
        assertTrue(commands.size() > 0);
    }

    /**
     * Clear out all accesses if any exist
     */
    @Test
    public void clearAllAccesses() throws ReefServiceException {

        CommandService cs = helpers;

        cs.clearCommandLocks();

        List<CommandAccess> noAccess = cs.getCommandLocks();
        assertEquals(noAccess.size(), 0);
    }

    /**
     * Test that some command names are returned from the commands service
     */
    @Test
    public void testCommandFailsWithoutSelect() throws ReefServiceException {

        CommandService cs = helpers;

        Command c = cs.getCommands().get(0);
        try {
            cs.executeCommandAsControl(c);
            fail("should throw exception");
        } catch (ReefServiceException rse) {
            assertEquals(Envelope.Status.BAD_REQUEST, rse.getStatus());
        }
    }

    /**
     * Test that command access request can be get, put, and deleted
     */
    @Test
    public void testGetPutDeleteCommandAccess() throws ReefServiceException {

        CommandService cs = helpers;

        Command cmd = cs.getCommands().get(0);

        // would allow user "user1" to exclusively execute commands
        CommandAccess ca = cs.createCommandExecutionLock(cmd);
        // removes the command access request by name
        cs.deleteCommandLock(ca);
    }

    /**
     * Test that some command names are returned from the commands service
     */
    @Test
    public void testCommandSelectAndExecute() throws ReefServiceException {

        CommandService cs = helpers;
        cs.clearCommandLocks();
        List<Command> commands = cs.getCommands();

        for(Command cmd: commands)
        {
            CommandAccess accessResponse = cs.createCommandExecutionLock(cmd);
            assertTrue(accessResponse.getExpireTime() > 0);
            CommandStatus cmdResponse = cs.executeCommandAsControl(cmd);
            assertEquals(cmdResponse, CommandStatus.SUCCESS);

            // delete select by reference (UID)
            cs.deleteCommandLock(accessResponse);
        }
    }

    /**
     * Test that you cannot select the same command (or subset of commands) at the same time
     */
    @Test
    public void testMultiSelect() throws ReefServiceException {

        CommandService cs = helpers;
        List<Command> cmds = cs.getCommands();

        CommandAccess accessResponse1 = cs.createCommandExecutionLock(cmds.subList(0, 3));
        assertTrue(accessResponse1.getExpireTime() > 0);

        try {
            cs.createCommandExecutionLock(cmds.subList(0, 2));
            fail("should have failed because we already selected");
        } catch (ReefServiceException pse) {
            assertEquals(Envelope.Status.UNAUTHORIZED, pse.getStatus());
        }

        cs.deleteCommandLock(accessResponse1);
    }

    /**
     * Test that we can both search and delete by command name (new since 0.2.1)
     */
    @Test
    public void testSearchingAndDeletingSelectByCommandName() throws ReefServiceException {

        CommandService cs = helpers;

        List<Command> cmds = cs.getCommands();
        Command cmd1 = cmds.get(0);
        Command cmd2 = cmds.get(1);

        CommandAccess noAccess = cs.getCommandLockOnCommand(cmd1);
        assertNull(noAccess);

        cs.createCommandExecutionLock(cmd1);
        cs.createCommandExecutionLock(cmd2);

        CommandAccess foundAccesses = cs.getCommandLockOnCommand(cmd1);
        assertNotNull(foundAccesses);
        assertEquals(foundAccesses.getCommandsCount(), 1);
        assertEquals(foundAccesses.getCommands(0), cmd1.getName());

        cs.deleteCommandLock(foundAccesses);

        CommandAccess foundAccesses2 = cs.getCommandLockOnCommand(cmd2);
        assertNotNull(foundAccesses2);
        assertEquals(foundAccesses2.getCommandsCount(), 1);
        assertEquals(foundAccesses2.getCommands(0), cmd2.getName());

        cs.deleteCommandLock(foundAccesses2);

        CommandAccess noAccess2 = cs.getCommandLockOnCommand(cmd2);
        assertNull(noAccess2);
    }

    /**
     * Test to recreate issue discovered by denver
     */
    @Test
    public void testCommandSelectExecuteDeleteExecuteDelete() throws ReefServiceException {

        CommandService cs = helpers;

        Command cmd = cs.getCommands().get(0);

        // select
        CommandAccess accessResponse = cs.createCommandExecutionLock(cmd);
        assertTrue(accessResponse.getExpireTime() > 0);

        // execute
        CommandStatus cmdResponse = cs.executeCommandAsControl(cmd);
        assertEquals(CommandStatus.SUCCESS, cmdResponse);

        // delete
        cs.deleteCommandLock(accessResponse);

        try {
            // second execute fails
            cs.executeCommandAsControl(cmd);
            fail("should throw exception");
        } catch (ReefServiceException pse) {
            assertEquals(Envelope.Status.BAD_REQUEST, pse.getStatus());
        }
    }
}
