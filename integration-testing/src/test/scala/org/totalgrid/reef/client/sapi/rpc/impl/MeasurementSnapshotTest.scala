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
import org.totalgrid.reef.clientapi.exceptions.{ BadRequestException, ExpectationException }

import org.totalgrid.reef.client.sapi.rpc.impl.util.ClientSessionSuite

@RunWith(classOf[JUnitRunner])
class MeasurementSnapshotTest
    extends ClientSessionSuite("MeasurementSnapshot.xml", "MeasurementSnapshot",
      <div>
        <p>
          The MeasurementSnapshot service provides the current state of measurements. The request contains the
          list of measurement names, the response contains the requested measurement objects.
        </p>
      </div>)
    with ShouldMatchers {

  test("Simple gets") {

    val names = List("StaticSubstation.Line02.Current", "StaticSubstation.Breaker02.Bkr", "StaticSubstation.Breaker02.Tripped")

    recorder.addExplanation("Get single measurement", "Get the current state of a single measurement.")
    client.getMeasurementsByNames(names.slice(0, 1))

    recorder.addExplanation("Get multiple measurements", "Get the current state of multiple measurements. Notice that they are all returned wrapped in a single parent object.")
    client.getMeasurementsByNames(names)
  }

  test("Non existant measurement get") {

    intercept[BadRequestException] {
      recorder.addExplanation("Get non-existant measurement", "If we ask for the current value of a measurement that should return error code.")
      client.getMeasurementsByNames("UnknownPoint" :: Nil).await
    }

    intercept[ExpectationException] {
      recorder.addExplanation("Get non-existant point", "Asking for a non-existant point fails localy because we don't get the one we asked for.")
      client.getPointByName("UnknownPoint").await
    }
  }
}
