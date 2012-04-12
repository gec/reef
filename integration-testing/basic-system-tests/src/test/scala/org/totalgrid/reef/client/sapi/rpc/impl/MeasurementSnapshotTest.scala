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
import org.totalgrid.reef.client.exception.{ BadRequestException, ExpectationException }

import org.totalgrid.reef.client.sapi.rpc.impl.util.ServiceClientSuite

@RunWith(classOf[JUnitRunner])
class MeasurementSnapshotTest extends ServiceClientSuite {

  test("Non existant measurement get") {

    intercept[BadRequestException] {
      // "If we ask for the current value of a measurement that should return error code.")
      client.getMeasurementsByNames("UnknownPoint" :: Nil)
    }

    intercept[ExpectationException] {
      // "Asking for a non-existant point fails localy because we don't get the one we asked for.")
      client.getPointByName("UnknownPoint")
    }
  }
}
