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

import org.totalgrid.reef.api.ISubscription;
import org.totalgrid.reef.api.ReefServiceException;
import org.totalgrid.reef.proto.Alarms.*;
import org.totalgrid.reef.proto.Alarms;
import org.totalgrid.reef.proto.Events;
import org.totalgrid.reef.proto.Model.Entity;
import org.totalgrid.reef.proto.Model.Relationship;

import java.util.List;

import org.totalgrid.reef.integration.helpers.*;

public class TestAlarmService extends JavaBridgeTestBase {

    /**
     * insert an event config and post an event of that type to generate the alarm
     */
    @Test
    public void prepareAlarms()  throws ReefServiceException {
        client.putOne(EventConfig.newBuilder()
                .setEventType("Test.Alarm")
                .setResource("Alarm")
                .setDesignation(EventConfig.Designation.ALARM)
                .setAlarmState(Alarms.Alarm.State.UNACK_AUDIBLE)
                .setSeverity(1).build()
        );

        // add an alarm for a point we know is not changing
        client.putOne(Events.Event.newBuilder()
                .setEventType("Test.Alarm")
                .setEntity(Entity.newBuilder().setName("StaticSubstation.Line02.Current")).build()
        );
    }

	/** Test that some alarms are returned from the AlarmQuery service */
	@Test
	public void simpleQueries() throws ReefServiceException {

		// Get all alarms that are not removed.
		List<Alarm> alarms = SampleRequests.getUnRemovedAlarms(client, "Test.Alarm");
		assertTrue(alarms.size() > 0);

	}

	/** Test getting alarms for a whole substation or individual device */
	@Test
	public void entityQueries()  throws ReefServiceException {

		// Get the first substation
		Entity substation = SampleRequests.getRandomSubstation(client);

		// Get all the points in the substation. Alarms are associated with individual points.
		Entity eqRequest = Entity.newBuilder().setName("StaticSubstation").addRelations(
				Relationship.newBuilder().setRelationship("owns").setDescendantOf(true).addEntities(Entity.newBuilder().addTypes("Point"))).build();

		// Get the alarms on both the substation and devices under the substation.
		List<Alarm> alarms = SampleRequests.getAlarmsForEntity(client, eqRequest, "Test.Alarm");
		assertTrue(alarms.size() > 0);
	}

	/** Test alarm state update. */
	@Test
	public void updateAlarms()  throws ReefServiceException {

		// Get unacknowledged alarms.
		List<Alarm> alarms = SampleRequests.getUnRemovedAlarms(client, "Test.Alarm");
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

}