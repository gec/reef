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
package org.totalgrid.reef.procstatus

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import org.totalgrid.reef.proto.ProcessStatus.StatusSnapshot
import org.totalgrid.reef.proto.Application.HeartbeatConfig

import org.mockito.{ ArgumentCaptor, Mockito }
import org.totalgrid.reef.client.sapi.rpc.ApplicationService
import net.agileautomata.executor4s.testing.MockExecutor
import net.agileautomata.executor4s.Success
import org.totalgrid.reef.api.sapi.client.impl.FixedPromise

@RunWith(classOf[JUnitRunner])
class ProcessHeartbeatActorTest extends FunSuite with ShouldMatchers {

  val REPEAT_TIME = 10

  def makeConfig: HeartbeatConfig = {
    HeartbeatConfig.newBuilder
      .setInstanceName("test")
      .setProcessId("1")
      .setPeriodMs(REPEAT_TIME).build
  }

  test("Heartbeats are sent") {
    val services = Mockito.mock(classOf[ApplicationService])
    val argument = ArgumentCaptor.forClass(classOf[StatusSnapshot])
    val promise = new FixedPromise(Success(StatusSnapshot.getDefaultInstance))
    Mockito.when(services.sendHeartbeat(argument.capture())).thenReturn(promise)

    val mockExecutor = new MockExecutor

    val actor = new ProcessHeartbeatActor(services, makeConfig, mockExecutor)

    actor.start()

    mockExecutor.runNextPendingAction should equal(true)
    argument.getValue.getOnline should equal(true)

    actor.stop()

    mockExecutor.isIdle should equal(true)
    argument.getValue.getOnline should equal(false)
  }
}
