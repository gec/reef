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

import org.totalgrid.reef.protocol.api.mock.{ RecordingProtocol, NullProtocol }
import org.totalgrid.reef.client.service.proto.FEP.{ EndpointConnection, CommChannel }

import scala.collection.immutable.Queue

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.totalgrid.reef.client.Client

@RunWith(classOf[JUnitRunner])
class AlwaysOnlineTest extends FunSuite with ShouldMatchers {

  val client = Mockito.mock(classOf[Client])

  class MockPublisher[A] extends Publisher[A] {
    var queue = Queue.empty[A]
    def publish(state: A) = {
      queue += state
    }
  }

  test("Channel callbacks") {
    val mp = new NullProtocol with RecordingProtocol with ChannelAlwaysOnline
    val pub = new MockPublisher[CommChannel.State]

    mp.addChannel(CommChannel.newBuilder.setName("channel1").build, pub, client)
    mp.removeChannel("channel1")

    pub.queue should equal(Queue(CommChannel.State.OPENING, CommChannel.State.OPEN, CommChannel.State.CLOSED))
  }

  test("Endpoint callbacks") {
    val mp = new NullProtocol with RecordingProtocol with EndpointAlwaysOnline
    val pub = new MockPublisher[EndpointConnection.State]

    mp.addEndpoint("endpoint1", "", Nil, NullBatchPublisher, pub, client)
    pub.queue should equal(Queue(EndpointConnection.State.COMMS_UP))

    mp.removeEndpoint("endpoint1")
    pub.queue should equal(Queue(EndpointConnection.State.COMMS_UP, EndpointConnection.State.COMMS_DOWN))
  }

}

