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

import builders.AlarmListRequestBuilders
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

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
    val desc = "Get all alarms by specifying a wildcard AlarmSelect (except a limit of 2 records returned)."

    client.addExplanation("Get all alarms", desc)
    client.getOneOrThrow(AlarmListRequestBuilders.getAll(2))
  }

  test("Get all unacknowledged alarms") {
    val desc = "Get unacknowledged alarms (limit 2). Need to specify the two unacknowledged states."

    client.addExplanation("Get unacknowledged alarms", desc)
    client.getOneOrThrow(AlarmListRequestBuilders.getUnacknowledged(2))
  }

  test("Get alarms with multiple selects") {

    val desc = "Get unacknowledged alarms with type 'Scada.OutOfNominal' (limit 2)"

    client.addExplanation("Get alarms with multiple selects", desc)
    client.getOneOrThrow(AlarmListRequestBuilders.getUnacknowledgedWithType("Scada.OutOfNominal", 2))
  }

}