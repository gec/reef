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

import org.totalgrid.reef.messaging.javabridge.Subscription;
import org.totalgrid.reef.messaging.Descriptors;
import org.totalgrid.reef.proto.Alarms.*;
import org.totalgrid.reef.proto.Model.Entity;
import org.totalgrid.reef.proto.Model.Relationship;

import java.util.List;

import org.totalgrid.reef.integration.helpers.*;

public class TestAlarmService extends JavaBridgeTestBase {

	/** Test that some alarms are returned from the AlarmQuery service */
	@Test
	public void simpleQueries() {

		// Get all alarms that are not removed.
		List<Alarm> alarms = SampleRequests.getUnRemovedAlarms(client);
		assertTrue(alarms.size() > 0);

	}

	/** Test getting alarms for a whole substation or individual device */
	@Test
	public void entityQueries() {

		// Get the first substation
		Entity substation = SampleRequests.getRandomSubstation(client);

		// Get all the points in the substation. Alarms are associated with individual points.
		Entity eqRequest = Entity.newBuilder(substation).addRelations(
				Relationship.newBuilder().setRelationship("owns").setDescendantOf(true).addEntities(Entity.newBuilder().addTypes("Point"))).build();

		// Get the alarms on both the substation and devices under the substation.
		List<Alarm> alarms = SampleRequests.getAlarmsForEntity(client, eqRequest);
		assertTrue(alarms.size() > 0);
	}

	/** Test alarm state update. */
	@Test
	public void updateAlarms() {

		// Get unacknowledged alarms.
		List<Alarm> alarms = SampleRequests.getUnRemovedAlarms(client);
		assertTrue(alarms.size() > 0);

		// Grab the first unacknowledged alarm and acknowledge it.
		for (Alarm alarm : alarms) {
			if (alarm.getState() == Alarm.State.UNACK_AUDIBLE || alarm.getState() == Alarm.State.UNACK_SILENT) {
				Alarm result = SampleRequests.updateAlarm(client, alarm.getUid(), Alarm.State.ACKNOWLEDGED);
				assertTrue(result.getState() == Alarm.State.ACKNOWLEDGED);
				break;
			}
		}
	}

	/** Show a subscription to all alarms. */
	@Test
	@SuppressWarnings("unchecked")
	public void subscribeToAllAlarms() throws java.lang.InterruptedException {

		MockEventAcceptor<Alarm> mock = new MockEventAcceptor<Alarm>();

		Subscription sub = client.addSubscription(Descriptors.alarm(), mock);

		List<Alarm> alarms = client.get(Alarm.newBuilder().build(), sub);

		assertTrue(alarms.size() > 0);

		mock.pop(10000);
	}
}