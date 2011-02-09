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
import org.totalgrid.reef.proto.Measurements.*;
import org.totalgrid.reef.proto.Model.*;
import java.util.List;

import org.totalgrid.reef.protoapi.ProtoServiceTypes.*;
import org.totalgrid.reef.messaging.javabridge.*;
import org.totalgrid.reef.messaging.Descriptors;

import org.totalgrid.reef.integration.helpers.*;

@SuppressWarnings("unchecked")
public class TestMeasSnapshotService extends JavaBridgeTestBase {

	/**
	 * Test that the number of measurements retrieved from the measurement Snapshot service by name
	 * is the same.
	 */
	@Test
	public void measSnapshotCountMatches() {
		List<Point> plist = SampleRequests.getAllPoints(client);
		List<Measurement> mlist = SampleRequests.getCurrentValues(client, SampleProtos.makeMeasSnapshot(plist));
		assertTrue(plist.size() == mlist.size());
	}

	/**
	 * Tests subscribing to the measurement snapshot service via a get operation
	 */
	@Test
	public void testMeasSnapshotSubscription() throws java.lang.InterruptedException {

		// mock object that will receive queue and measurement subscription
		MockEventAcceptor<Measurement> mock = new MockEventAcceptor<Measurement>();

		Subscription sub = client.addSubscription(Descriptors.measurementSnapshot(), mock);

		// make the all points request, w/ subscribe queue set
		MeasurementSnapshot request = SampleProtos.makeMeasSnapshot(SampleRequests.getAllPoints(client));
		MeasurementSnapshot response = client.getOne(request, sub);

		assertEquals(request.getPointNamesCount(), response.getMeasurementsCount());

		// check that at least one measurement has been updated in the queue
		Event<Measurement> m = mock.pop(10000);
		assertTrue(m.getEvent() == Envelope.Event.MODIFIED);

		// now cancel the subscription
		sub.cancel();
		Thread.sleep(1000);
		mock.clear();

		try {
			mock.pop(1000);
			fail("pop() should raise an Exception");
		} catch (Exception e) {
		}

	}

}
