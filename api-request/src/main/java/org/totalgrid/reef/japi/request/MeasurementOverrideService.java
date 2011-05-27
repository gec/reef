/**
 * Copyright 2011 Green Energy Corp.
 * 
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.japi.request;

import org.totalgrid.reef.japi.ReefServiceException;
import org.totalgrid.reef.proto.Measurements.Measurement;
import org.totalgrid.reef.proto.Model.Point;
import org.totalgrid.reef.proto.Processing.MeasOverride;

/**
 * SCADA systems there is the concept of a stopping the measurement stream from field and publishing another value in
 * its place. There are 2 ways this is done, marking a point "Not in Service" (NIS) and "Overriding" the point.
 * <p/>
 * This is usually done for one of two  reasons*  - A field devices is reporting a bad value (wildly oscillating or pinned to 0) and is generating spurious
 * alarms or confusing "advanced apps" that are performing calculations on that value. In this case an operator will
 * override that data to its nominal value.
 * - Training/Testing purposes, when an operator or integrator is testing alarms/UI/apps its often valuable to just
 * be able to quickly override a value and see that the correct behaviors occur.
 */
public interface MeasurementOverrideService
{

    /**
     * take a point "out of service", suppresses future measurements from "field protocol". Reads the last measurement
     * out of the measurement store and republishes it with the OLD_DATA and OPERATOR_BLOCKED quality bits set. It is
     * important to use this function, not trying to read the current value and change the quality in client code,
     * because the measurement processor is the only place in the system that can do this override safely without any
     * possibility of losing measurements.
     */
    MeasOverride setPointOutOfService( Point point ) throws ReefServiceException;

    /**
     * overrides the "field protocol" measurement stream for a point. While an override is in place
     * the value on a point will not change unless the override is changed. Beware that there is no checking done to make
     * sure that the overridden measurement is "similar" to the measurements read in from the field. It is important
     * that the client code provides a sensible Measurement. The measurement processor will publish the measurement with
     * the SUBSTITUTED quality bit set.
     */
    MeasOverride setPointOverriden( Point point, Measurement measurement ) throws ReefServiceException;

    /**
     * deletes a given measurement override, most recent field value will be instantly published if available
     */
    MeasOverride deleteMeasurementOverride( MeasOverride measOverride ) throws ReefServiceException;

    /**
     * makes sure that any measurement overrides are cleared if existent
     *
     * @return whether an override was cleared.
     */
    boolean clearMeasurementOverridesOnPoint( Point point ) throws ReefServiceException;
}