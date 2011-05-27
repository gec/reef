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

import org.totalgrid.reef.proto.FEP.{ CommEndpointConnection, CommChannel }

import scala.collection.immutable.Queue

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class AlwaysOnlineTest extends FunSuite with ShouldMatchers {

  class MockListener[A] extends IListener[A] {
    var queue = Queue.empty[A]
    def onUpdate(state: A) = queue += state
  }

  test("Channel callbacks") {
    val mp = new MockProtocol with ChannelAlwaysOnline
    val listener = new MockListener[CommChannel.State]

    mp.addChannel(CommChannel.newBuilder.setName("channel1").build, listener)
    mp.removeChannel("channel1")

    listener.queue should equal(Queue(CommChannel.State.OPENING, CommChannel.State.OPEN, CommChannel.State.CLOSED))
  }

  test("Endpoint callbacks") {
    val mp = new MockProtocol(false) with EndpointAlwaysOnline
    val listener = new MockListener[CommEndpointConnection.State]

    mp.addEndpoint("endpoint1", "", Nil, NullPublisher, listener)
    listener.queue should equal(Queue(CommEndpointConnection.State.COMMS_UP))

    mp.removeEndpoint("endpoint1")
    listener.queue should equal(Queue(CommEndpointConnection.State.COMMS_UP, CommEndpointConnection.State.COMMS_DOWN))
  }

}

