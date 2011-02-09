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

import org.totalgrid.reef.proto.Envelope;
import org.totalgrid.reef.proto.Commands.*;
import org.totalgrid.reef.proto.Model.Command;
import java.util.List;

import org.totalgrid.reef.protoapi.ProtoServiceTypes.*;
import org.totalgrid.reef.protoapi.ProtoServiceException;


import org.totalgrid.reef.integration.helpers.*;
import org.totalgrid.reef.messaging.javabridge.*;
import org.totalgrid.reef.messaging.Descriptors;

@SuppressWarnings("unchecked")
public class TestCommandService extends JavaBridgeTestBase {

	/** Test that some command names are returned from the commands service */
	@Test
	public void someCommandsReturned() {
		List<Command> commands = SampleRequests.getAllCommands(client);
		assertTrue(commands.size() > 0);
	}

	/** Test that some command names are returned from the commands service */
	@Test
	public void testCommandFailsWithoutSelect() {
		Command c = SampleRequests.getAllCommands(client).get(0);
		try {
			SampleRequests.executeControl(client, "user", c);
			fail("should throw exception");
		} catch (ProtoServiceException pse) {
			assertEquals(Envelope.Status.NOT_ALLOWED, pse.getStatus());
		}
	}

	/** Test that command access request can be get, put, and deleted */
	@Test
	public void testGetPutDeleteCommandAccess() {
		Command cmd = SampleRequests.getAllCommands(client).get(0);

		// clear any existing command access entries on this command, this
		// this just wraps delete in a try/catch block
		SampleRequests.clearCommandAccess(client, cmd.getName());
		// would allow user "user1" to exclusively execute commands for 5000 ms.
		SampleRequests.putCommandAccess(client, "user1", cmd, 5000, true);
		// removes the command access request
		SampleRequests.deleteCommandAccess(client, cmd.getName());
	}

	/** Test that some command names are returned from the commands service */
	@Test
	public void testCommandSelectAndExecute() {
		Command cmd = SampleRequests.getAllCommands(client).get(0);
		SampleRequests.clearCommandAccess(client, cmd.getName());
		CommandAccess accessResponse = SampleRequests.putCommandAccess(client, "user", cmd, 5000, true);
		assertTrue(accessResponse.getExpireTime() > 0);
		UserCommandRequest cmdResponse = SampleRequests.executeControl(client, "user", cmd);
		assertEquals(cmdResponse.getStatus(), CommandStatus.EXECUTING);
	}

	/**
	 * The "EXECUTING" status just tells us that the command was received and allowed The response
	 * from the field device / endpoint comes back via an asynchronous subscription
	 */
	@Test
	public void testCommandSelectExecuteAndSubscribe() throws InterruptedException {
		Command cmd = SampleRequests.getAllCommands(client).get(0);
		SampleRequests.clearCommandAccess(client, cmd.getName());
		CommandAccess accessResponse = SampleRequests.putCommandAccess(client, "user", cmd, 5000, true);
		assertTrue(accessResponse.getExpireTime() > 0);

		// create infrastructure to execute a control with subscription to
		// result

		MockEventAcceptor<UserCommandRequest> mock = new MockEventAcceptor<UserCommandRequest>();
		UserCommandRequest request = SampleProtos.makeControlRequest(cmd, "user");
		Subscription sub = client.addSubscription(Descriptors.userCommandRequest(), mock);
		client.putOne(request, sub);

		// We get 2 events here. Since the subscription is bound before the request is made,
		// we see the ADDED/EXECUTING and then the MODIFIED/SUCCESS
		{
			Event<UserCommandRequest> rsp = mock.pop(5000);
			assertEquals(Envelope.Event.ADDED, rsp.getEvent());
			assertEquals(CommandStatus.EXECUTING, rsp.getResult().getStatus());
		}
		{
			Event<UserCommandRequest> rsp = mock.pop(5000);
			assertEquals(Envelope.Event.MODIFIED, rsp.getEvent());
			assertEquals(CommandStatus.SUCCESS, rsp.getResult().getStatus());
		}

		// cancel the subscription
		sub.cancel();
	}

}
