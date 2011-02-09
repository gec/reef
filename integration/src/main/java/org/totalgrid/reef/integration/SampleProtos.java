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

import org.totalgrid.reef.proto.Measurements.*;
import org.totalgrid.reef.proto.Measurements.Quality.Source;
import org.totalgrid.reef.proto.Processing.MeasOverride;
import org.totalgrid.reef.proto.Commands.*;
import org.totalgrid.reef.proto.Commands.CommandAccess.AccessMode;
import org.totalgrid.reef.proto.Model.*;

import java.util.List;

class SampleProtos {

	public static UserCommandRequest makeControlRequest(Command cmd, String user) {
		UserCommandRequest.Builder ucr = UserCommandRequest.newBuilder().setUser(user);
		CommandRequest.Builder cr = CommandRequest.newBuilder();
		cr.setName(cmd.getName()).setType(CommandRequest.ValType.NONE);
		return ucr.setCommandRequest(cr).build();
	}

	public static CommandAccess makeCommandAccess(Command cmd, String user, long timeout, boolean allow) {
		return CommandAccess.newBuilder().addCommands(cmd.getName()).setExpireTime(timeout).setUser(user).setAccess(
				allow ? AccessMode.ALLOWED : AccessMode.BLOCKED).build();
	}

	/**
	 * Creates a default integer type measurement proto
	 * 
	 * @param name
	 *            name of the measurement
	 * @param value
	 *            the value as an integer
	 * @param time
	 *            the timestamp on the measurement
	 * @return A fully built measurement proto
	 */
	public static Measurement makeIntMeas(String name, long value, long time) {
		Measurement.Builder b = Measurement.newBuilder();
		b.setName(name).setIntVal(value).setType(Measurement.Type.INT);
		b.setTime(time).setQuality(Quality.newBuilder());
		return b.build();
	}

	/**
	 * Adds substituted quality attributes to a measurement
	 * 
	 * @param m
	 *            Measurement to add attributes onto
	 * @return Modified measurement proto
	 */
	public static Measurement makeSubstituted(Measurement m) {
		Measurement.Builder b = Measurement.newBuilder(m);
		Quality.Builder q = Quality.newBuilder(b.getQuality()).setSource(Source.SUBSTITUTED).setOperatorBlocked(true);
		b.setQuality(q);
		return b.build();
	}

	/**
	 * Takes a list of Points and prepares a request for the measurement snapshot service which
	 * provides the current values of the points (and the subscription to new values)
	 */
	public static MeasurementSnapshot makeMeasSnapshot(List<Point> list) {
		MeasurementSnapshot.Builder b = MeasurementSnapshot.newBuilder();
		for (Point p : list) {
			b.addPointNames(p.getName());
		}
		return b.build();
	}

	/**
	 * Takes a point and a measurement value and creates a measurement override
	 */
	public static MeasOverride makeMeasOverride(Point p, Measurement m) {
		MeasOverride.Builder b = MeasOverride.newBuilder();
		b.setPoint(p);
		b.setMeas(m);
		return b.build();
	}

	/**
	 * Takes a point and a measurement value and creates a measurement override
	 */
	public static MeasOverride makeMeasOverride(Point p) {
		MeasOverride.Builder b = MeasOverride.newBuilder();
		b.setPoint(p);
		return b.build();
	}


	public static MeasurementSnapshot makeMeasSnapshot(String pointName) {
		MeasurementSnapshot.Builder b = MeasurementSnapshot.newBuilder();
		b.addPointNames(pointName);
		return b.build();
	}

    /**
	 * create a measurementbatch from one or measurements
	 */
	public static MeasurementBatch makeMeasBatch(Measurement ... measurements) {
		MeasurementBatch.Builder b = MeasurementBatch.newBuilder();
		for (Measurement p : measurements) {
			b.addMeas(p);
		}
        b.setWallTime(System.currentTimeMillis());
		return b.build();
	}

}