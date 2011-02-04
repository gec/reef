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

import org.totalgrid.reef.protoapi.client.ServiceClient;
import org.totalgrid.reef.proto.Measurements.*;
import org.totalgrid.reef.proto.Auth.*;
import org.totalgrid.reef.proto.Commands.*;
import org.totalgrid.reef.proto.Model.*;

import org.totalgrid.reef.proto.Alarms.*;
import org.totalgrid.reef.proto.Events.*;
import org.totalgrid.reef.messaging.javabridge.*;

import java.util.List;
import java.util.Random;

import org.totalgrid.reef.protoapi.ProtoServiceException;

@SuppressWarnings("unchecked")
public class SampleRequests {

	public static UserCommandRequest executeControl(IServiceClient client, String user, Command cmd) {
		UserCommandRequest request = SampleProtos.makeControlRequest(cmd, user);
		UserCommandRequest result = client.putOne(request);
		return result;
	}

	public static CommandAccess putCommandAccess(IServiceClient client, String user, Command cmd, long timeout, boolean allow) {
		CommandAccess accessRequest = SampleProtos.makeCommandAccess(cmd, user, timeout, allow);
		CommandAccess result = client.putOne(accessRequest);
		return result;
	}

	public static CommandAccess deleteCommandAccess(IServiceClient client, String cmdName) {
		CommandAccess request = CommandAccess.newBuilder().addCommands(cmdName).build();
		CommandAccess result = client.deleteOne(request);
		return result;
	}

	public static void clearCommandAccess(IServiceClient client, String cmdName) {
		try {
			deleteCommandAccess(client, cmdName);
		} catch (ProtoServiceException pse) {

		}
	}

	public static CommandAccess getCommandAccess(IServiceClient client, String user, Command cmd) {
		CommandAccess request = CommandAccess.newBuilder().addCommands(cmd.getName()).build();
		CommandAccess result = client.getOne(request);
		return result;
	}

	/**
	 * Asks for all points regardless of who owns them.
	 */
	public static List<Point> getAllPoints(IServiceClient client) {
		Point p = Point.newBuilder().setName("*").build();
		List<Point> list = client.get(p);
		return list;
	}

	/**
	 * Requests the current values (most recent measurement) for points in MeasurementSnapshot
	 * proto.
	 */
	public static List<Measurement> getCurrentValues(IServiceClient client, MeasurementSnapshot request) {
		MeasurementSnapshot ms = client.getOne(request);
		return ms.getMeasurementsList();
	}

	/**
	 * @param bridge
	 *            connection object to use
	 * @return List of all command objects
	 */
	public static List<Command> getAllCommands(IServiceClient client) {
		Command c = Command.newBuilder().setName("*").build();
		return client.get(c);
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
	public static AuthToken logonAs(IServiceClient client, String user, String password, boolean addAuthTokenForAllClients) {
		Agent agent = Agent.newBuilder().setName(user).setPassword(password).build();
		AuthToken b = AuthToken.newBuilder().setAgent(agent).build();
		AuthToken t = client.putOne(b);
		if (addAuthTokenForAllClients) {
			// add the auth token to the list of auth tokens we send with every request
			client.getDefaultEnv().setAuthToken(t.getToken());
		}
		return t;
	}

	/**
	 * Return a list of the most recent alarms (not in the REMOVED state).
	 */
	public static List<Alarm> getUnRemovedAlarms(IServiceClient client) {
		Alarm s = Alarm.newBuilder().build(); // select all State!=REMOVED by default.
		List<Alarm> list = client.get(s);
		return list;
	}

	/**
	 * Return a list of the most recent alarms (not in the REMOVED state) for the given entity. The
	 * entity could be a substation or a device. In the case of a substation it should return all
	 * the alarms on the substation entity and all alarms on devices under the substation.
	 */
	public static List<Alarm> getAlarmsForEntity(IServiceClient client, Entity entity) {

		Event.Builder es = Event.newBuilder();
		es.setEntity(entity);

		Alarm p = Alarm.newBuilder().setEvent(es).build();
		List<Alarm> list = client.get(p);
		return list;
	}

	/**
	 * Update the state of an existing alarm.
	 */
	public static Alarm updateAlarm(IServiceClient client, String uid, Alarm.State newState) {
		Alarm.Builder a = Alarm.newBuilder();
		a.setUid(uid);
		a.setState(newState);
		Alarm result = client.putOne(a.build()); // TODO: Check return code.
		return result;
	}

	/**
	 * Randomly choose a substation or fail test if there are no substations
	 */
	public static Entity getRandomSubstation(IServiceClient client) {
		Entity request = Entity.newBuilder().addTypes("Substation").build();
		List<Entity> substations = client.get(request);
		if (substations.size() == 0) throw new RuntimeException("No Subsations");		
		Random r = new Random();
		Entity substation = substations.get(r.nextInt(substations.size()));
		return substation;
	}
}
