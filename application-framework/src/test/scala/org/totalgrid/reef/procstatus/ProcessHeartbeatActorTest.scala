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

import org.totalgrid.reef.executor.mock.{ MockExecutorTrait }
import org.totalgrid.reef.japi.request.{ ApplicationService }
import org.mockito.{ ArgumentCaptor, Mockito }

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
    Mockito.when(services.sendHeartbeat(argument.capture())).thenReturn(null)

    val actor = new ProcessHeartbeatActor(services, makeConfig) with MockExecutorTrait

    actor.start()

    actor.numActionsPending should equal(1)
    actor.repeatNext(1, 1) should equal(REPEAT_TIME)
    argument.getValue.getOnline should equal(true)

    actor.stop()

    actor.numActionsPending should equal(0)
    argument.getValue.getOnline should equal(false)
  }
}
