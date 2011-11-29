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
package org.totalgrid.reef.protocol.api

import mock.{ NullProtocol, RecordingProtocol }
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import org.totalgrid.reef.proto.FEP

@RunWith(classOf[JUnitRunner])
class AddRemoveValidationTest extends FunSuite with ShouldMatchers {

  import RecordingProtocol._

  val channel = FEP.CommChannel.newBuilder.setName("port1").build()

  // ordering here is important as there are stacking traits.
  // AddRemove gets called first, then the recoding happens
  def getProtocol = new NullProtocol with RecordingProtocol with AddRemoveValidation

  test("RemoveExceptions") {
    val m = getProtocol
    intercept[IllegalArgumentException](m.removeChannel("foo"))
    m.next() should equal(None)
    intercept[IllegalArgumentException](m.removeEndpoint("foo"))
    m.next() should equal(None)
  }

  test("AddEndpointWithoutPort") {
    val m = getProtocol

    intercept[IllegalArgumentException](m.addEndpoint("ep", "unknown", Nil, NullBatchPublisher, NullEndpointPublisher))
  }

  test("EndpointAlreadyExists") {
    val m = getProtocol
    m.addChannel(channel, NullChannelPublisher)
    m.addEndpoint("ep", "port1", Nil, NullBatchPublisher, NullEndpointPublisher)
    intercept[IllegalArgumentException](m.addEndpoint("ep", "port1", Nil, NullBatchPublisher, NullEndpointPublisher))
  }

  def addPortAndTwoEndpoints(m: RecordingProtocol) {
    m.addChannel(channel, NullChannelPublisher)
    m.next() should equal(Some(AddChannel(channel.getName)))
    m.addEndpoint("ep1", "port1", Nil, NullBatchPublisher, NullEndpointPublisher)
    m.next() should equal(Some(AddEndpoint("ep1", "port1", Nil)))
    m.addEndpoint("ep2", "port1", Nil, NullBatchPublisher, NullEndpointPublisher)
    m.next() should equal(Some(AddEndpoint("ep2", "port1", Nil)))
    m.next() should equal(None)
  }

  test("RemovePortWithEndpoints") {
    val m = getProtocol
    addPortAndTwoEndpoints(m)
    m.removeChannel("port1")
    m.next() should equal(None)
    m.removeEndpoint("ep1")
    m.removeEndpoint("ep2")
    m.next() should equal(Some(RemoveEndpoint("ep1")))
    m.next() should equal(Some(RemoveEndpoint("ep2")))
    m.removeChannel("port1")
    m.next() should equal(Some(RemoveChannel("port1")))
    m.next() should equal(None)
  }

  test("TracksEndpointsCorrectly") {
    val m = getProtocol
    addPortAndTwoEndpoints(m)
    m.removeEndpoint("ep1")
    m.next() should equal(Some(RemoveEndpoint("ep1")))
    m.removeChannel("port1")
    m.next() should equal(None)
    m.removeEndpoint("ep2")
    m.next() should equal(Some(RemoveEndpoint("ep2")))
    m.removeChannel("port1")
    m.next() should equal(Some(RemoveChannel("port1")))
    m.next() should equal(None)
  }
}

