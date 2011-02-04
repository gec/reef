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
import org.totalgrid.reef.proto.Auth.*;

import org.totalgrid.reef.integration.helpers.JavaBridgeTestBase;
import org.totalgrid.reef.protoapi.ProtoServiceException;

public class TestAuthService extends JavaBridgeTestBase {
	public TestAuthService() {
		// disable autoLogin
		super(false);
	}

	@Test
	public void successfulLogin() {
		AuthToken t = SampleRequests.logonAs(client, "core", "core", false);
		assertTrue(t.getToken().length() > 0);
	}

	@Test
	public void demonstateAuthTokenNeeded() {
		try {
			// will fail because we don't havent logged in to get auth tokens
			SampleRequests.getAllPoints(client);
			assertTrue(false);
		} catch (ProtoServiceException pse) {
			assertEquals(pse.getStatus(), Envelope.Status.BAD_REQUEST);
		}
		// logon as all permission user
		SampleRequests.logonAs(client, "core", "core", true);
		// request will now not be rejected
		SampleRequests.getAllPoints(client);
	}
}
