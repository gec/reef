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

import org.junit.Test;

import org.totalgrid.reef.integration.helpers.MockSubscriptionEventAcceptor;
import org.totalgrid.reef.integration.helpers.ReefConnectionTestBase;

import org.totalgrid.reef.japi.request.MeasurementService;

import org.totalgrid.reef.japi.*;
import org.totalgrid.reef.japi.client.Subscription;
import org.totalgrid.reef.japi.client.SubscriptionEvent;
import org.totalgrid.reef.japi.client.SubscriptionResult;
import org.totalgrid.reef.proto.Measurements;
import org.totalgrid.reef.proto.Model;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * tests to prove that the simulator is up and measurements are being processed correctly.
 */
@SuppressWarnings("unchecked")
public class TestEndToEndIntegration extends ReefConnectionTestBase
{

    /**
     * Tests subscribing to the measurement snapshot service via a get operation
     */
    @Test
    public void testSimulatorProducingMeasurements() throws java.lang.InterruptedException, ReefServiceException
    {

        MeasurementService ms = helpers;

        // mock object that will receive queue and measurement subscription
        MockSubscriptionEventAcceptor<Measurements.Measurement> mock = new MockSubscriptionEventAcceptor<Measurements.Measurement>();


        List<Model.Point> points = SampleRequests.getAllPoints( client );

        SubscriptionResult<List<Measurements.Measurement>, Measurements.Measurement> result = ms.subscribeToMeasurementsByPoints( points );

        List<Measurements.Measurement> response = result.getResult();
        Subscription<Measurements.Measurement> sub = result.getSubscription();

        assertEquals( response.size(), points.size() );

        sub.start( mock );

        // check that at least one measurement has been updated in the queue
        SubscriptionEvent<Measurements.Measurement> m = mock.pop( 10000 );
        assertEquals( m.getEventType(), Envelope.Event.MODIFIED );

        // now cancel the subscription
        sub.cancel();
        Thread.sleep( 1000 );
        mock.clear();

        try
        {
            mock.pop( 1000 );
            fail( "pop() should raise an Exception" );
        }
        catch ( Exception e )
        {
        }

    }

}
