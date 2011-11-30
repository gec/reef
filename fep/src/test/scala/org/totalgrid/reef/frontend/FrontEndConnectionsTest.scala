/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the GNU Affero General Public License
 * Version 3.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.frontend

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

import org.totalgrid.reef.test.MockitoStubbedOnly

import FrontEndTestHelpers._
import org.totalgrid.reef.protocol.api.mock.{ NullProtocol, RecordingProtocol }
import org.totalgrid.reef.protocol.api.mock.RecordingProtocol._
import org.mockito.{ Matchers, Mockito }
import org.totalgrid.reef.client.service.commands.CommandRequestHandler
import org.totalgrid.reef.client.sapi.client.impl.FixedPromise
import net.agileautomata.executor4s.Success
import org.totalgrid.reef.client.sapi.client.rest.Client

@RunWith(classOf[JUnitRunner])
class FrontEndConnectionsTest extends FunSuite with ShouldMatchers {
  test("Add/remove proto") {
    val config = getConnectionProto(true, Some("routing"))
    val client = Mockito.mock(classOf[FrontEndProviderServices], new MockitoStubbedOnly)
    val rawClient = Mockito.mock(classOf[Client])
    val cancelable = new MockCancelable
    val commandBinding = new FixedPromise(Success(cancelable))
    Mockito.doReturn(commandBinding).when(client).bindCommandHandler(Matchers.eq(config.getEndpoint.getUuid), Matchers.any(classOf[CommandRequestHandler]))

    val mp = new NullProtocol("mock") with RecordingProtocol { override def requiresChannel = true }

    val channelName = config.getEndpoint.getChannel.getName
    val endpointName = config.getEndpoint.getName

    cancelable.canceled should equal(false)

    val connections = new FrontEndConnections(mp :: Nil, client, rawClient)

    connections.add(config)

    mp.next() should equal(Some(AddChannel(channelName)))
    mp.next() should equal(Some(AddEndpoint(endpointName, channelName, Nil)))
    mp.next() should equal(None)

    connections.remove(config)

    mp.next() should equal(Some(RemoveEndpoint(endpointName)))
    mp.next() should equal(Some(RemoveChannel(channelName)))
    mp.next() should equal(None)

    cancelable.canceled should equal(true)
  }
}