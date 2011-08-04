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

import org.totalgrid.reef.proto.{ ProcessStatus }
import org.totalgrid.reef.messaging._
import org.totalgrid.reef.messaging.mock._
import org.totalgrid.reef.sapi.AllMessages

import org.scalatest.Suite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import org.totalgrid.reef.util.SyncVar
import org.totalgrid.reef.executor.ReactActorExecutor

import ProcessStatus._

import org.totalgrid.reef.proto.Application.HeartbeatConfig

@RunWith(classOf[JUnitRunner])
class ProcessHeartbeatActorTest extends Suite with ShouldMatchers {

  val EXCHANGE = "test_proc_status"

  def makeConfig(): HeartbeatConfig = {
    HeartbeatConfig.newBuilder
      .setProcessId("1")
      .setPeriodMs(10)
      .setRoutingKey("key")
      .setDest(EXCHANGE).build
  }

  def subscribe(amqp: AMQPProtoFactory): SyncVar[StatusSnapshot] = {
    val box = new SyncVar(StatusSnapshot.getDefaultInstance)
    amqp.subscribe(EXCHANGE, AllMessages, StatusSnapshot.parseFrom, { s: StatusSnapshot => box.update(s) })
    box
  }

  def testComeUp() {
    AMQPFixture.mock(true) { amqp =>
      val box = subscribe(amqp)
      val actor = new ProcessHeartbeatActor(amqp, makeConfig) with ReactActorExecutor
      actor.start
      box.waitFor((s: StatusSnapshot) => { s.getOnline == true })

    }
  }

  def testBeating() {
    AMQPFixture.mock(true) { amqp =>
      val box = subscribe(amqp)
      val actor = new ProcessHeartbeatActor(amqp, makeConfig) with ReactActorExecutor

      actor.start
      var lastTime = -1: Long
      for (i <- 1 to 3) yield box.waitFor((s: StatusSnapshot) => { val ret = s.getTime != lastTime; lastTime = s.getTime; ret })
    }
  }

  def testGoDown() {
    AMQPFixture.mock(true) { amqp =>
      val box = subscribe(amqp)
      val actor = new ProcessHeartbeatActor(amqp, makeConfig) with ReactActorExecutor

      actor.start
      box.waitFor((s: StatusSnapshot) => { s.getOnline == true })
      actor.stop
      box.waitFor((s: StatusSnapshot) => { s.getOnline == false })
    }
  }
}
