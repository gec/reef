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

import org.totalgrid.reef.proto.FEP
import org.totalgrid.reef.proto.Communications.{ ChannelState, EndpointState }

import scala.collection.immutable.Queue

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class AlwaysOnlineTest extends FunSuite with ShouldMatchers {

  def getMockChannelListener = new IChannelListener {
    var queue = Queue.empty[ChannelState.State]
    def onStateChange(state: ChannelState.State) = queue += state
  }

  def getMockEndpointListener = new IEndpointListener {
    var queue = Queue.empty[EndpointState.State]
    def onStateChange(state: EndpointState.State) = queue += state
  }

  test("Channel callbacks") {
    val mp = new MockProtocol with ChannelAlwaysOnline
    val listener = getMockChannelListener

    mp.addChannel(FEP.Port.newBuilder.setName("channel1").build, listener)
    mp.removeChannel("channel1")

    listener.queue should equal(Queue(ChannelState.State.OPENING, ChannelState.State.OPEN, ChannelState.State.CLOSED))
  }

  test("Endpoint callbacks") {
    val mp = new MockProtocol(false) with EndpointAlwaysOnline
    val listener = getMockEndpointListener

    mp.addEndpoint("endpoint1", "", Nil, NullPublisher, listener)
    listener.queue should equal(Queue(EndpointState.State.COMMS_UP))

    mp.removeEndpoint("endpoint1")
    listener.queue should equal(Queue(EndpointState.State.COMMS_UP, EndpointState.State.COMMS_DOWN))
  }

}

