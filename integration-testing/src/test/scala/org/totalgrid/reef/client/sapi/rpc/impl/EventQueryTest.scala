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
package org.totalgrid.reef.client.sapi.rpc.impl

import org.totalgrid.reef.client.sapi.rpc.impl.builders.EventListRequestBuilders
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import java.util.Calendar
import org.totalgrid.reef.client.sapi.rpc.impl.util.ServiceClientSuite

@RunWith(classOf[JUnitRunner])
class EventQueryTest extends ServiceClientSuite {

  test("Get all events (limit 2)") {
    client.getRecentEvents(2).await
  }

  test("Get all login/logout events") {
    client.getRecentEvents(List("System.UserLogin", "System.UserLogout"), 2).await
  }

  test("Get events with multiple selects") {

    val yesterday = nowPlus(Calendar.DATE, -1)
    val twoHoursFromNow = nowPlus(Calendar.HOUR, 2)

    client.searchForEvents(EventListRequestBuilders.getByTimeRangeAndSubsystemSelector(yesterday, twoHoursFromNow, "Core", 2).build).await
  }

  // Get a time offset based on the well known NOW_MS
  def nowPlus(field: Int, amount: Int): Long = {
    val cal = Calendar.getInstance
    cal.set(Calendar.MILLISECOND, 0) // truncate milliseconds to 0.
    cal.add(field, amount)
    cal.getTimeInMillis
  }

}