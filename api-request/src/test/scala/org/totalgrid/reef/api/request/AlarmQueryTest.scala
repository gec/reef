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

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import scala.collection.JavaConversions._
import org.totalgrid.reef.proto.Events.EventSelect
import org.totalgrid.reef.proto.Alarms._
import java.util.{ Date, Calendar }

@RunWith(classOf[JUnitRunner])
class AlarmQueryTest
    extends ServiceClientSuite("AlarmQuery.xml", "Alarm Query",
      <div>
        <p>
          Use<span class="proto">AlarmList</span>
          to query alarms. The same proto
        is returned by the query and will contain the selected Alarms.
        </p>
        <p>
          The<span class="proto">AlarmList</span>
          request must contain an
          <span class="proto">AlarmSelect</span>
          which may specify a list of alarm states and an<span class="proto">EventSelect</span>
          .
        </p>
        <p>
          The<span class="proto">AlarmSelect</span>
          proto may specify a list of alarm states and an<span class="proto">EventSelect</span>
          .
          The<span class="proto">EventSelect</span>
          contains several fields
        that specify what alarms should be selected by the query. Each field is
        'AND'ed with every other field.
        </p>
        <p>See<a href="https://github.com/gec/reef/blob/master/schema/proto/Alarms.proto">Alarms.proto</a></p>
      </div>)
    with ShouldMatchers {

  test("Get all alarms (limit 2)") {
    val desc = "Get all alarms by specifying an empty AlarmSelect (except a limit of 2 records returned)."

    val request =
      AlarmList.newBuilder
        .setSelect(
          AlarmSelect.newBuilder
          .setEventSelect(EventSelect.newBuilder.setLimit(2)))
        .build

    val response = client.getOneOrThrow(request)

    doc.addCase[AlarmList]("Get all alarms", "Get", desc, request, response)

  }

  test("Get all unacknowledged alarms") {
    val desc = "Get unacknowledged alarms (limit 2). Need to specify the two unacknowledged states."

    val request: AlarmList =
      AlarmList.newBuilder
        .setSelect(
          AlarmSelect.newBuilder
          .addState(Alarm.State.UNACK_AUDIBLE)
          .addState(Alarm.State.UNACK_SILENT)
          .setEventSelect(EventSelect.newBuilder.setLimit(2)))
        .build

    val response = client.getOneOrThrow(request)

    doc.addCase("Get unacknowledged alarms", "Get", desc, request, response)

  }

  test("Get alarms with multiple selects") {

    val desc = "Get unacknowledged alarms with type 'Scada.OutOfNominal' (limit 2)"

    val NOW = now()
    val yesterday = nowPlus(Calendar.DATE, -1)
    val HOURS_AGO_2 = nowPlus(Calendar.HOUR, -2)

    val request: AlarmList =
      AlarmList.newBuilder
        .setSelect(
          AlarmSelect.newBuilder
          .addState(Alarm.State.UNACK_AUDIBLE)
          .addState(Alarm.State.UNACK_SILENT)
          .setEventSelect(
            EventSelect.newBuilder
            .addEventType("Scada.OutOfNominal")
            .setLimit(2)))
        .build

    val response = client.getOneOrThrow(request)

    doc.addCase("Get alarms with multiple selects", "Get", desc, request, response)

  }

  // Get a time offset based on the well known NOW_MS
  def now(): Long = {
    val cal = Calendar.getInstance()
    cal.set(Calendar.MILLISECOND, 0) // truncate milliseconds to 0.
    cal.getTimeInMillis
  }

  // Get a time offset based on the well known NOW_MS
  def nowPlus(field: Int, amount: Int): Long = {
    val cal = Calendar.getInstance
    cal.setTimeInMillis(now)
    cal.add(field, amount)
    cal.getTimeInMillis
  }

}