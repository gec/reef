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
package org.totalgrid.reef.integration.helpers;

import org.junit.*;

import org.totalgrid.reef.protoapi.IConnectionListener;
import org.totalgrid.reef.protoapi.java.client.*;
import org.totalgrid.reef.messaging.javabridge.JavaBridge;
import org.totalgrid.reef.messaging.BrokerConnectionInfo;

import org.totalgrid.reef.integration.SampleRequests;

/**
 * Base class for JUnit based integration tests run against the "live" system
 */
public class JavaBridgeTestBase {

	private boolean autoLogon;

	public class MockConnectionListener implements IConnectionListener {
		private BlockingQueue<Boolean> queue = new BlockingQueue<Boolean>();

		public void opened() {
			queue.push(true);
		}

		public void closed() {
			queue.push(false);
		}

		boolean waitForStateChange(int timeout) throws InterruptedException {
			return queue.pop(timeout);
		}
	}

	/**
	 * connector to the bus, restarted for every test connected for
	 */
	protected IConnection connection = new JavaBridge(getConnectionInfo(), 5000);
	protected ISession client = null;
	protected MockConnectionListener listener = new MockConnectionListener();

	/**
	 * Baseclass for junit integration tests, provides a JavaBridge that is started and stopped with
	 * every test case.
	 * 
	 * @param autoLogon
	 *            If set we automatically acquire and set auth tokens for the client on every
	 *            request
	 */
	public JavaBridgeTestBase(boolean autoLogon) {
		this.autoLogon = autoLogon;
		connection.addConnectionListener(listener);
	}

	/**
	 * defaults autoLogon to true
	 */
	public JavaBridgeTestBase() {
		this(true);
	}

	/**
	 * gets the ip of the qpid server, defaults to 127.0.0.1 but can be override with java property
	 * -Dreef_node_ip=192.168.100.10
	 */
	private BrokerConnectionInfo getConnectionInfo() {
		String reef_ip = System.getProperty("reef_node_ip");
		if (reef_ip == null) reef_ip = "127.0.0.1";
		String reef_port = System.getProperty("reef_node_port");
		if (reef_port == null) reef_port = "5672";
		String user = System.getProperty("org.totalgrid.reef.amqp.user");
		if (user == null) user = "guest";
		String password = System.getProperty("org.totalgrid.reef.amqp.password");
		if (password == null) password = "guest";
		String virtualHost = System.getProperty("org.totalgrid.reef.amqp.virtualHost");
		if (virtualHost == null) virtualHost = "test";

		return new BrokerConnectionInfo(reef_ip, Integer.parseInt(reef_port), user, password, virtualHost);
	}

	@Before
	public void startBridge() throws InterruptedException {
		connection.start();
		org.junit.Assert.assertTrue(listener.waitForStateChange(5000));
		client = connection.newSession();
		if (autoLogon) {
			// core user has full read/write permissions
			SampleRequests.logonAs(client, "core", "core", true);
		}
	}

	@After
	public void stopBridge() throws InterruptedException {
		client.close();
		connection.stop();
		org.junit.Assert.assertFalse(listener.waitForStateChange(5000));
	}
}
