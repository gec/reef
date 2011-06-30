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
package org.totalgrid.reef.messaging.broker.qpid

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import org.totalgrid.reef.broker.BrokerConnectionInfo
import org.totalgrid.reef.broker.qpid.QpidBrokerConnection
import org.totalgrid.reef.messaging.mock.AMQPFixture
import org.totalgrid.reef.messaging.sync.SyncSubscriptionHandler
import org.totalgrid.reef.executor.ReactActorExecutor
import org.totalgrid.reef.messaging.{ ProtoSubscriptionTestBase, AmqpClientSession }

@RunWith(classOf[JUnitRunner])
class QpidProtoSubscriptionTest extends ProtoSubscriptionTestBase {
  def setupTest(test: AmqpClientSession => Unit) {

    val qpidInfo = BrokerConnectionInfo.loadInfo("test")
    val qpid = new QpidBrokerConnection(qpidInfo)

    AMQPFixture.sync(qpid, true) { syncAmqp =>
      val client = new AmqpClientSession(syncAmqp, servicelist, 10000)

      val pub = new SyncSubscriptionHandler(syncAmqp.getChannel, exchange + "_events")

      val executor = new ReactActorExecutor {}
      executor.start

      syncAmqp.bindService(exchange, (new DemoSubscribeService(pub)).respond, executor, competing = true)

      try {
        test(client)
      } finally {
        executor.stop
      }
    }

  }
}