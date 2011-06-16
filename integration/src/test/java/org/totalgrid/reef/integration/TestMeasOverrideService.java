/**
 * Copyright 2011 Green Energy Corp.
 * 
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the GNU Affero General Public License
 * Version 3.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 * http://www.gnu.org/licenses/agpl.html
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.integration;

import org.junit.*;

import static org.junit.Assert.*;

import org.totalgrid.reef.japi.ReefServiceException;
import org.totalgrid.reef.japi.client.SubscriptionResult;
import org.totalgrid.reef.japi.request.MeasurementOverrideService;
import org.totalgrid.reef.japi.request.MeasurementService;

import org.totalgrid.reef.japi.request.builders.MeasurementRequestBuilders;
import org.totalgrid.reef.proto.Measurements.*;
import org.totalgrid.reef.proto.Model.*;
import org.totalgrid.reef.proto.Processing.MeasOverride;

import java.util.LinkedList;
import java.util.List;

import org.totalgrid.reef.integration.helpers.*;

@SuppressWarnings("unchecked")
public class TestMeasOverrideService extends ReefConnectionTestBase
{

    /** Test that the measurement overrides work correctly */
    @Test
    public void deleteAndPutOverrides() throws InterruptedException, ReefServiceException
    {

        // use a point we know will be static
        String pointName = "StaticSubstation.Line02.Current";
        Point p = Point.newBuilder().setName( pointName ).build();
        List<Point> ps = new LinkedList<Point>();
        ps.add( p );

        MeasurementService measurementService = helpers;
        MeasurementOverrideService overrideService = helpers;

        Measurement originalValue = measurementService.getMeasurementByPoint( p );

        MockSubscriptionEventAcceptor<Measurement> mock = new MockSubscriptionEventAcceptor<Measurement>( true );

        // delete override by point
        overrideService.clearMeasurementOverridesOnPoint( p );


        // subscribe to updates for this point
        SubscriptionResult<List<Measurement>, Measurement> result = measurementService.subscribeToMeasurementsByPoints( ps );

        assertEquals( result.getResult().size(), 1 );
        result.getSubscription().start( mock );


        long now = System.currentTimeMillis();

        // create an override
        Measurement m = MeasurementRequestBuilders.makeIntMeasurement( pointName, 11111, now );
        MeasOverride override = overrideService.setPointOverriden( p, m );

        // make sure we see it in the event stream
        assertTrue( mock.waitFor( MeasurementRequestBuilders.makeSubstituted( m ), 5000 ) );

        // verify that substitued value got to measurement database
        Measurement stored = measurementService.getMeasurementByPoint( p );
        assertEquals( MeasurementRequestBuilders.makeSubstituted( m ), stored );

        List<Measurement> batch = new LinkedList<Measurement>();

        // if we now try to put a measurement it should be suppressed (b/c we have overridden it)
        Measurement suppressedValue = MeasurementRequestBuilders.makeIntMeasurement( pointName, 22222, now + 1 );
        batch.add( suppressedValue );

        measurementService.publishMeasurements( batch );

        // we store the most recently reported value in a cache so if we publish a value
        // while it is overridden and then take off the override we should see the cached value published
        Measurement cachedValue = MeasurementRequestBuilders.makeIntMeasurement( pointName, 33333, now + 2 );
        batch.clear();
        batch.add( cachedValue );
        measurementService.publishMeasurements( batch );

        // now we remove the override, we should get the second measurement we attempted to publish
        overrideService.deleteMeasurementOverride( override );

        // publish a final measurement we expect to see on the subscription channel
        Measurement finalValue = MeasurementRequestBuilders.makeIntMeasurement( pointName, 44444, now + 3 );
        batch.clear();
        batch.add( finalValue );
        measurementService.publishMeasurements( batch );

        // verify that last value got to measurement database
        Measurement lastValue = measurementService.getMeasurementByPoint( p );
        assertEquals( finalValue, lastValue );

        // verify that we get that final value
        assertTrue( mock.waitFor( finalValue, 5000 ) );

        // then verify that we never got the suppressed value
        List<Measurement> measurements = mock.getPayloads();
        assertTrue( measurements.contains( cachedValue ) );
        assertFalse( measurements.contains( suppressedValue ) );

        // put the original value back in
        batch.clear();
        batch.add( originalValue );
        measurementService.publishMeasurements( batch );
    }
}
