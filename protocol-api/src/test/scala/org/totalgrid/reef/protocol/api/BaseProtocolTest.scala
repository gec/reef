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

@RunWith(classOf[JUnitRunner])
class BaseProtocolTest extends FunSuite with ShouldMatchers {

  import MockProtocol._

  val port = FEP.CommChannel.newBuilder.setName("port1").build()

  test("RemoveExceptions") {
    val m = new MockProtocol
    intercept[IllegalArgumentException] { m.removeChannel("foo") }
    intercept[IllegalArgumentException] { m.removeEndpoint("foo") }
  }

  test("AddEndpointWithoutPort") {
    val m = new MockProtocol

    intercept[IllegalArgumentException] { m.addEndpoint("ep", "unknown", Nil, NullPublisher, NullEndpointListener) }
  }

  test("EndpointAlreadyExists") {
    val m = new MockProtocol
    m.addChannel(port, NullChannelListener)
    m.addEndpoint("ep", "port1", Nil, NullPublisher, NullEndpointListener)
    intercept[IllegalArgumentException] { m.addEndpoint("ep", "port1", Nil, NullPublisher, NullEndpointListener) }
  }

  def addPortAndTwoEndpoints(m: MockProtocol) {
    m.addChannel(port, NullChannelListener)
    m.checkFor { case AddPort(p) => p should equal(port) }
    m.addEndpoint("ep1", "port1", Nil, NullPublisher, NullEndpointListener)
    m.checkFor { case AddEndpoint("ep1", "port1", Nil) => }
    m.addEndpoint("ep2", "port1", Nil, NullPublisher, NullEndpointListener)
    m.checkFor { case AddEndpoint("ep2", "port1", Nil) => }
    m.checkForNothing
  }

  test("RemovePortWithEndpoints") {
    val m = new MockProtocol
    addPortAndTwoEndpoints(m)
    m.removeChannel("port1")
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
    m.removeChannel("port1")
    m.checkFor { case RemoveEndpoint("ep2") => }
    m.checkFor { case RemovePort("port1") => }
    m.checkForNothing
  }
}

