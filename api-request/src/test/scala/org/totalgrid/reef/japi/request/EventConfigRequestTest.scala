package org.totalgrid.reef.japi.request

/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  Green Energy Corp licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import builders.{ EventRequestBuilders, EventConfigRequestBuilders }
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class EventConfigRequestTest
    extends ClientSessionSuite("EventConfig.xml", "Event Config",
      <div>
        <p>
          Event Configs describe all of the types of "well known" event types in the system. When an event occurs,
          1 of 3 things will occur:
          <ol>
            <li>
              <b>Log:</b>
              The event will be logged to a system file but not stored in the events table. This is
              primarily used when we want to suppress events that are deemed unimportant in this installation.
            </li>
            <li>
              <b>Event:</b>
              The event will be stored in the events table. This will generally be visibile on an HMI
              but usually wouldn't be cause a "push" notification to users.
            </li>
            <li>
              <b>Alarm:</b>
              If an event is configured to have type Alarm it is recorded as an event but an alarm
              notification is also generated. This is usually pushed to the users quickly since a user needs to interact
              with the system to silence and/or acknowledge the alarm.
            </li>
          </ol>
        </p>
      </div>)
    with ShouldMatchers {

  test("Create Log, Event, Alarm configurations") {
    client.addExplanation("Treat as Log", "When ")
    client.put(EventConfigRequestBuilders.makeLog("Demo.AsLog", "Log Message", 1)).await().expectOne

    client.addExplanation("Treat as Event", "")
    client.put(EventConfigRequestBuilders.makeEvent("Demo.AsEvent", "Event Message", 2)).await().expectOne

    client.addExplanation("Treat as Alarm", "")
    client.put(EventConfigRequestBuilders.makeAudibleAlarm("Demo.AsAlarm", "Alarm Message", 3)).await().expectOne

    client.addExplanation("Post an Event", "Post an event that is configured to make an event entry in the table.")
    client.put(EventRequestBuilders.makeNewEventForEntityByName("Demo.AsEvent", "StaticSubstation.Line02.Current")).await().expectOne

    client.addExplanation("Post a Log", "When we post an event that is downgraded to a log message the result doesn't have the UID field set since they werenot stored in database")
    client.put(EventRequestBuilders.makeNewEventForEntityByName("Demo.AsLog", "StaticSubstation.Line02.Current")).await().expectOne

    client.addExplanation("Use attribute formatting", "The resource string can be dynamic based on the data passed with the event")
    client.put(EventConfigRequestBuilders.makeLog("Demo.Formatting", "Attributes name: {name} value: {value}", 1)).await().expectOne

    client.addExplanation("Use attribute formatting", "The resource string can be dynamic based on the data passed with the event")
    client.put(EventRequestBuilders.makeNewEventWithAttributes("Demo.Formatting", "name" -> "abra", "value" -> "cadabra")).await().expectOne
  }

}