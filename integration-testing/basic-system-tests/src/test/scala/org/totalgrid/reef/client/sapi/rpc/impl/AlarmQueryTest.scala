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

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.totalgrid.reef.client.exception.ReefServiceException

import org.totalgrid.reef.client.sapi.rpc.impl.util.ServiceClientSuite

@RunWith(classOf[JUnitRunner])
class AlarmQueryTest extends ServiceClientSuite {
  //
  //  test("Get alarms") {
  //    val alarm = client.getActiveAlarms(1).head
  //
  //    // TODO: fix AlarmService.getAlarmById to use ReefId
  //    client.getAlarmById(alarm.getId.getValue)
  //  }

  test("Test alarm failure") {
    val exc = intercept[ReefServiceException] {
      client.getAlarmById("1234567890123456789")
    }.getMessage
    exc.contains("1234567890123456789") should equal(true)
    exc.contains("id") should equal(true)
  }

}