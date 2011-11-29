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

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.totalgrid.reef.clientapi.exceptions.ReefServiceException

import org.totalgrid.reef.client.sapi.rpc.impl.builders.AlarmListRequestBuilders
import org.totalgrid.reef.client.sapi.rpc.impl.util.ClientSessionSuite

@RunWith(classOf[JUnitRunner])
class AlarmQueryTest
    extends ClientSessionSuite("AlarmQuery.xml", "Alarm Query",
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
    val desc = "Get all alarms by specifying a wildcard AlarmSelect (except a limit of 2 records returned)."

    recorder.addExplanation("Get all alarms", desc)
    session.get(AlarmListRequestBuilders.getAll(2)).await.expectOne
  }

  test("Get all unacknowledged alarms") {
    val desc = "Get unacknowledged alarms (limit 2). Need to specify the two unacknowledged states."

    recorder.addExplanation("Get unacknowledged alarms", desc)
    session.get(AlarmListRequestBuilders.getUnacknowledged(2)).await.expectOne
  }

  test("Get alarms with multiple selects") {

    val desc = "Get unacknowledged alarms with type 'Scada.OutOfNominal' (limit 2)"

    recorder.addExplanation("Get alarms with multiple selects", desc)
    session.get(AlarmListRequestBuilders.getUnacknowledgedWithType("Scada.OutOfNominal", 2)).await.expectOne
  }

  test("Test alarm failure") {
    val exc = intercept[ReefServiceException] {
      client.getAlarmById("1234567890123456789").await
    }.getMessage
    exc.contains("1234567890123456789") should equal(true)
    exc.contains("id") should equal(true)
  }

}