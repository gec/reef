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
package org.totalgrid.reef.protocol.api

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import org.totalgrid.reef.proto.FEP
import org.totalgrid.reef.proto.Measurements.MeasurementBatch

@RunWith(classOf[JUnitRunner])
class BaseProtocolTest extends FunSuite with ShouldMatchers {

  val nullPublisher = new IPublisher {
    def publish(batch: MeasurementBatch): Unit = {}
  }

  import MockProtocol._

  val port = FEP.Port.newBuilder().setName("port1").build()

  test("RemoveExceptions") {
    val m = new MockProtocol
    intercept[IllegalArgumentException] { m.removePort("foo") }
    intercept[IllegalArgumentException] { m.removeEndpoint("foo") }
  }

  test("AddEndpointWithoutPort") {
    val m = new MockProtocol

    intercept[IllegalArgumentException] { m.addEndpoint("ep", "unknown", Nil, nullPublisher) }
  }

  test("EndpointAlreadyExists") {
    val m = new MockProtocol
    m.addPort(port)
    m.addEndpoint("ep", "port1", Nil, nullPublisher)
    intercept[IllegalArgumentException] { m.addEndpoint("ep", "port1", Nil, nullPublisher) }
  }

  def addPortAndTwoEndpoints(m: MockProtocol) {
    m.addPort(port)
    m.checkFor { case AddPort(p) => p should equal(port) }
    m.addEndpoint("ep1", "port1", Nil, nullPublisher)
    m.checkFor { case AddEndpoint("ep1", "port1", Nil) => }
    m.addEndpoint("ep2", "port1", Nil, nullPublisher)
    m.checkFor { case AddEndpoint("ep2", "port1", Nil) => }
    m.checkForNothing
  }

  test("RemovePortWithEndpoints") {
    val m = new MockProtocol
    addPortAndTwoEndpoints(m)
    m.removePort("port1")
    m.checkFor { case RemoveEndpoint(_) => } //order of endpoint removal isn't specified
    m.checkFor { case RemoveEndpoint(_) => }
    m.checkFor { case RemovePort("port1") => }
    m.checkForNothing
  }

  test("TracksEndpointsCorrectly") {
    val m = new MockProtocol
    addPortAndTwoEndpoints(m)
    m.removeEndpoint("ep1")
    m.checkFor { case RemoveEndpoint("ep1") => }
    m.removePort("port1")
    m.checkFor { case RemoveEndpoint("ep2") => }
    m.checkFor { case RemovePort("port1") => }
    m.checkForNothing
  }
}

