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

import org.totalgrid.reef.proto.Measurements.*;
import org.totalgrid.reef.proto.Model.*;
import org.totalgrid.reef.proto.Processing.MeasOverride;
import java.util.List;

import org.totalgrid.reef.messaging.javabridge.*;
import org.totalgrid.reef.integration.helpers.*;

@SuppressWarnings("unchecked")
public class TestMeasOverrideService extends JavaBridgeTestBase {

	/** Test that the measurement overrides work correctly */
	@Test
	public void deleteAndPutOverrides() throws InterruptedException {
		List<Point> points = SampleRequests.getAllPoints(client);
		assertTrue(points.size() > 0);
		Point p = points.get(0);
		MockEventAcceptor<Measurement> mock = new MockEventAcceptor<Measurement>();
		Subscription sub = client.addSubscription(Deserializers.measurementSnapshot(), mock);

		{
			MeasOverride ovr = SampleProtos.makeMeasOverride(p);
			client.delete(ovr);
		}

		{
			MeasurementSnapshot ms = SampleProtos.makeMeasSnapshot(p);
			MeasurementSnapshot rsp = client.get_one(ms, sub);
			assertEquals(rsp.getMeasurementsCount(), 1);
		}

		Measurement m = SampleProtos.makeIntMeas(p.getName(), 12345, 54321);
		MeasOverride ovr = SampleProtos.makeMeasOverride(p, m);
		client.put_one(ovr);

		assertTrue(mock.waitFor(SampleProtos.makeSubstituted(m), 5000));
	}
}
