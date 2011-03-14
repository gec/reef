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
package org.totalgrid.reef.api.request

import builders.EventListRequestBuilders
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import scala.collection.JavaConversions._
import org.totalgrid.reef.proto.Events.{ EventList, EventSelect }
import java.util.{ Date, Calendar }

@RunWith(classOf[JUnitRunner])
class EventQueryTest
    extends ServiceClientSuite("EventQuery.xml", "Event Query",
      <div>
        <p>
          Use
          <span class="proto">EventList</span>
          to query events. The same proto
        is returned by the query and will contain the selected events.
        </p>
        <p>
          The<span class="proto">EventList</span>
          request must contain an
          <span class="proto">EventSelect</span>
          which specifies the
        parameters of what is being requested.
        </p>
        <p>
          The<span class="proto">EventSelect</span>
          proto contains several fields
        that specify what events should be selected by the query. Each field is
        'AND'ed with every other field.
        </p>
        <p>See<a href="https://github.com/gec/reef/blob/master/schema/proto/Events.proto">Events.proto</a></p>
      </div>)
    with ShouldMatchers {

  test("Get all events (limit 2)") {
    val desc = "Get all events by specifying a wildcard EventSelect (except a limit of 2 records returned)."

    client.addExplanation("Get all events", desc)
    client.getOneOrThrow(EventListRequestBuilders.getAll(2))

  }

  test("Get all login/logout events") {
    val desc = "Get all login/logout events (limit 2)."

    client.addExplanation("Get all login/logout events", desc)
    client.getOneOrThrow(EventListRequestBuilders.getAllByEventTypes(List("System.UserLogin", "System.UserLogout"), 2))

  }

  test("Get events with multiple selects") {

    val desc = "Get events with subsystem 'Core' from yesterday until 2hrs from now (limit 2)"

    val yesterday = nowPlus(Calendar.DATE, -1)
    val twoHoursFromNow = nowPlus(Calendar.HOUR, 2)

    client.addExplanation("Get events with multiple selects", desc)
    client.getOneOrThrow(EventListRequestBuilders.getByTimeRangeAndSubsystem(yesterday, twoHoursFromNow, "Core", 2))
  }

  // Get a time offset based on the well known NOW_MS
  def nowPlus(field: Int, amount: Int): Long = {
    val cal = Calendar.getInstance
    cal.set(Calendar.MILLISECOND, 0) // truncate milliseconds to 0.
    cal.add(field, amount)
    cal.getTimeInMillis
  }

}