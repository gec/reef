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

import org.totalgrid.reef.messaging.BrokerConnectionInfo;
import org.totalgrid.reef.proto.ReefServicesList;
import org.totalgrid.reef.messaging.javabridge.JavaBridge;
import org.totalgrid.reef.api.javaclient.IConnection;

public class TestBridgeExceptionBehaviors {

	@Test
	public void throwsExceptionWhenNotStarted() {
		String reef_ip = System.getProperty("reef_node_ip");
		if (reef_ip == null) reef_ip = "127.0.0.1";

		IConnection connection = new JavaBridge(new BrokerConnectionInfo(reef_ip, 5672, "guest", "guest", "test"), ReefServicesList.getInstance(), 5000);

		try {
			connection.newSession();
			fail("Should throw exception when not connected");
		} catch (Exception ex) {
		}
	}
}
