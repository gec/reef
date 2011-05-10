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

import org.totalgrid.reef.api.javaclient.ISession;
import org.totalgrid.reef.api.ReefServiceException;

import org.totalgrid.reef.api.request.builders.*;
import org.totalgrid.reef.proto.Measurements.*;
import org.totalgrid.reef.proto.Auth.*;
import org.totalgrid.reef.proto.Model.*;

import org.totalgrid.reef.proto.Alarms.*;

import java.util.List;
import java.util.LinkedList;
import java.util.Random;

// TODO: port all java integration tests to use api-request library reef_techdebt-9

@SuppressWarnings("unchecked")
public class SampleRequests {

	/**
	 * Asks for all points regardless of who owns them.
	 */
	public static List<Point> getAllPoints(ISession client) throws ReefServiceException {
		List<Point> list = client.get(PointRequestBuilders.getAll());
		return list;
	}

	/**
	 * Requests the current values (most recent measurement) for points
	 */
	public static List<Measurement> getCurrentValues(ISession client, List<Point> points) throws ReefServiceException  {
		MeasurementSnapshot ms = client.getOne(MeasurementSnapshotRequestBuilders.getByPoints(points));
		return ms.getMeasurementsList();
	}

	/**
	 * Simulates logging onto the bus as a user or application with name/password combination.
	 * 
	 * @param user
	 * @param password
	 * @param addAuthTokenForAllClients
	 *            If set the authToken is added to the defaultEnvs for all clients using that bridge
	 * @return The AuthToken proto returned by the server, .getToken has the magic string.
	 */
	public static void logonAs(ISession client, String user, String password, boolean addAuthTokenForAllClients) throws ReefServiceException {
		AuthToken t = client.putOne(AuthTokenRequestBuilders.requestAuthToken(user, password));
		if (addAuthTokenForAllClients) {
			// add the auth token to the list of auth tokens we send with every request
			client.getDefaultEnv().setAuthToken(t.getToken());
		}
	}

	/**
	 * Return a list of the most recent alarms (not in the REMOVED state).
	 */
	public static List<Alarm> getUnRemovedAlarms(ISession client, String eventType)  throws ReefServiceException {
		List<Alarm> list = client.get(AlarmRequestBuilders.getAllByType(eventType));
		return list;
	}

	/**
	 * Return a list of the most recent alarms (not in the REMOVED state) for the given entity. The
	 * entity could be a substation or a device. In the case of a substation it should return all
	 * the alarms on the substation entity and all alarms on devices under the substation.
	 */
	public static List<Alarm> getAlarmsForEntity(ISession client, Entity entity, String eventType) throws ReefServiceException  {
		List<Alarm> list = client.get(AlarmRequestBuilders.getByTypeForEntity(eventType, entity));
		return list;
	}

	/**
	 * Update the state of an existing alarm.
	 */
	public static Alarm updateAlarm(ISession client, Alarm alarm, Alarm.State newState)  throws ReefServiceException {
		Alarm result = client.putOne(AlarmRequestBuilders.updateAlarmState(alarm, newState));
		return result;
	}

	/**
	 * Randomly choose a substation or fail test if there are no substations
	 */
	public static Entity getRandomSubstation(ISession client)  throws ReefServiceException {
		List<Entity> substations = client.get(EntityRequestBuilders.getByType("Substation"));
		if (substations.size() == 0) throw new RuntimeException("No Substations");
		Random r = new Random();
		Entity substation = substations.get(r.nextInt(substations.size()));
		return substation;
	}

    /**
     * get a flattened list of all children of the entity with name parent.
     * Examples: all "Points" under "Apex" or all "Commands" under "Breaker2"
     */
    public static List<Entity> getChildrenOfType(ISession client, String parent, String type)  throws ReefServiceException {
        Entity sub = client.getOne(EntityRequestBuilders.getOwnedChildrenOfTypeFromRootName(parent, type));

		List<Entity> entities = new LinkedList<Entity>();

		for (Relationship r : sub.getRelationsList()) {
			for (Entity e : r.getEntitiesList()) {
				entities.add(e);
			}
        }

		return entities;
    }
}
