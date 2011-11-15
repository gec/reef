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
package org.totalgrid.reef.clientapi.sapi.client.rest.impl

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import net.agileautomata.executor4s.testing.MockExecutor
import net.agileautomata.executor4s._
import org.totalgrid.reef.broker.{ BrokerConnectionListener, BrokerConnection, BrokerConnectionFactory }
import org.totalgrid.reef.clientapi.sapi.client.rest.ConnectionWatcher
import org.mockito.{ ArgumentCaptor, Mockito }
import org.totalgrid.reef.clientapi.exceptions.ServiceIOException

@RunWith(classOf[JUnitRunner])
class ReconnectingFactoryTest extends FunSuite with ShouldMatchers {

  def goodConnection() = {
    val exe = new MockExecutor
    val broker = new BrokerConnectionFactory {
      var connection = Option.empty[BrokerConnection]
      def connect = connection.getOrElse(throw new ServiceIOException("test failure"))
    }

    val connection = Mockito.mock(classOf[BrokerConnection])
    val captor = ArgumentCaptor.forClass(classOf[BrokerConnectionListener])
    Mockito.doNothing().when(connection).addListener(captor.capture())

    val watcher = Mockito.mock(classOf[ConnectionWatcher])

    val reconnector = new DefaultReconnectingFactory(broker, exe, 1000, 5000)
    reconnector.addConnectionWatcher(watcher)

    (exe, broker, connection, captor, watcher, reconnector)
  }

  test("Sucessfull Connection calls listener") {

    val (exe, broker, connection, captor, watcher, reconnector) = goodConnection()

    broker.connection = Some(connection)

    // nothing should be queued before we start
    exe.numQueuedTimers should equal(0)

    // startup the reconnector, should enqueue the first connection attempt
    reconnector.start()
    exe.numQueuedTimers should equal(1)

    // should call factory.connect and then inform the listeners
    // of the new connection
    exe.tick(5000.milliseconds)
    Mockito.verify(watcher).onConnectionOpened(connection)

    // no more pending actions
    exe.numQueuedTimers should equal(0)

    // now stop the reconnector, make sure it calls disconnect
    reconnector.stop()
    Mockito.verify(connection).disconnect()
    exe.numQueuedTimers should equal(0)
  }

  test("Failure to connect queues new attempt") {
    val (exe, broker, connection, captor, watcher, reconnector) = goodConnection()

    // startup the reconnector, should enqueue the first connection attempt
    reconnector.start()
    exe.numQueuedTimers should equal(1)

    exe.tick(5000.milliseconds)
    exe.numQueuedTimers should equal(1)
    (1 to 4).foreach { i =>
      exe.tick(5000.milliseconds)
      exe.numQueuedTimers should equal(1)
    }

    broker.connection = Some(connection)

    exe.tick(5000.milliseconds)
    Mockito.verify(watcher).onConnectionOpened(connection)

  }

  test("Stopping while failed to connect calls watcher with a failure") {
    val (exe, broker, connection, captor, watcher, reconnector) = goodConnection()

    // startup the reconnector, should enqueue the first connection attempt
    reconnector.start()
    exe.numQueuedTimers should equal(1)

    exe.tick(5000.milliseconds)
    exe.numQueuedTimers should equal(1)
    (1 to 4).foreach { i =>
      exe.tick(5000.milliseconds)
      exe.numQueuedTimers should equal(1)
    }

    reconnector.stop()
    Mockito.verify(watcher).onConnectionClosed(true)
  }

  test("Requeues connection on unexpected close") {

    val (exe, broker, connection, captor, watcher, reconnector) = goodConnection()

    broker.connection = Some(connection)

    // startup the reconnector, should enqueue the first connection attempt
    reconnector.start()
    exe.tick(5000.milliseconds)
    Mockito.verify(watcher).onConnectionOpened(connection)

    captor.getValue.onDisconnect(false)

    exe.numQueuedTimers should equal(1)
    Mockito.verify(watcher).onConnectionClosed(false)

    exe.tick(5000.milliseconds)
    Mockito.verify(watcher, Mockito.times(2)).onConnectionOpened(connection)
  }

}