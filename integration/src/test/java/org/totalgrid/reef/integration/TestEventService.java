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

import org.junit.Test;
import org.totalgrid.reef.api.ReefServiceException;
import org.totalgrid.reef.api.javaclient.ISubscription;
import org.totalgrid.reef.api.javaclient.ISubscriptionCreationListener;
import org.totalgrid.reef.api.javaclient.ISubscriptionResult;
import org.totalgrid.reef.api.request.AlarmService;
import org.totalgrid.reef.api.request.EventService;
import org.totalgrid.reef.api.request.builders.EventConfigRequestBuilders;
import org.totalgrid.reef.api.request.builders.EventRequestBuilders;
import org.totalgrid.reef.integration.helpers.BlockingQueue;
import org.totalgrid.reef.integration.helpers.ReefConnectionTestBase;
import org.totalgrid.reef.integration.helpers.MockEventAcceptor;
import org.totalgrid.reef.proto.Alarms.Alarm;
import org.totalgrid.reef.proto.Events;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class TestEventService extends ReefConnectionTestBase {

    @Test
    public void prepareEvents()  throws ReefServiceException {

        // make an event type for our test events
        client.put(EventConfigRequestBuilders.makeEvent("Test.Event", "Event", 1)).await().expectOne();

        EventService es = helpers;

        // populate some events
        for(int i=0; i < 15; i++){
            es.publishEvent(EventRequestBuilders.makeNewEventForEntityByName("Test.Event", "StaticSubstation.Line02.Current"));
        }
    }

	@Test
	public void getRecentEvents() throws ReefServiceException {
        EventService es = helpers;
		List<Events.Event> events = es.getRecentEvents(10);
		assertEquals(events.size(), 10);
	}

    @Test
	public void subscribeEvents() throws ReefServiceException, InterruptedException {

        MockEventAcceptor<Events.Event> mock = new MockEventAcceptor<Events.Event>(true);

        EventService es = helpers;

		ISubscriptionResult<List<Events.Event>, Events.Event> events = es.subscribeToRecentEvents(10);
        assertEquals(events.getResult().size(), 10);

        es.publishEvent(EventRequestBuilders.makeNewEventForEntityByName("Test.Event", "StaticSubstation.Line02.Current"));

        events.getSubscription().start(mock);

        mock.pop(1000);


	}

    @Test
    public void prepareAlarms()  throws ReefServiceException {

        // make an event type for our test alarms
        client.put(EventConfigRequestBuilders.makeAudibleAlarm("Test.Alarm", "Alarm", 1)).await().expectOne();

        EventService es = helpers;

        // populate some alarms
        for(int i=0; i < 5; i++){
            es.publishEvent(EventRequestBuilders.makeNewEventForEntityByName("Test.Alarm", "StaticSubstation.Line02.Current"));
        }
    }

    @Test
	public void subscribeAlarms() throws ReefServiceException, InterruptedException {

        MockEventAcceptor<Alarm> mock = new MockEventAcceptor<Alarm>(true);

        EventService es = helpers;
        AlarmService as = helpers;

        ISubscriptionResult<List<Alarm>, Alarm> result = as.subscribeToActiveAlarms(2);
		List<Alarm> events = result.getResult();
        assertEquals(events.size(), 2);

        es.publishEvent(EventRequestBuilders.makeNewEventForEntityByName("Test.Alarm", "StaticSubstation.Line02.Current"));

        result.getSubscription().start(mock);
        mock.pop(1000);
    }

    @Test
	public void subscriptionCreationCallback() throws ReefServiceException, InterruptedException {

        final BlockingQueue<ISubscription<?>> callback = new BlockingQueue<ISubscription<?>>();

        EventService es = (EventService)helpers;

        es.addSubscriptionCreationListener(new ISubscriptionCreationListener() {
            @Override
            public void onSubscriptionCreated(ISubscription<?> sub) {
                callback.push(sub);
            }
        });

        ISubscriptionResult<List<Events.Event>, Events.Event> result = es.subscribeToRecentEvents(1);
		List<Events.Event> events = result.getResult();
        assertEquals(events.size(), 1);

        assertEquals(callback.pop(1000), result.getSubscription());
    }

}