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
package org.totalgrid.reef.messaging.mock.synchronous

import org.totalgrid.reef.messaging.mock.MockSessionPool
import org.totalgrid.reef.messaging.Connection
import org.totalgrid.reef.sapi.client.{ Event, SessionPool }
import org.totalgrid.reef.sapi.service.AsyncService
import org.totalgrid.reef.sapi.Destination
import org.totalgrid.reef.executor.Executor
import org.totalgrid.reef.broker.CloseableChannel
import java.lang.Exception

object MockConnection {
  case class EventQueueRecord[A](onEvent: Event[A] => Unit, onNewQueue: String => Unit)
}

class MockConnection extends Connection {

  import MockConnection._

  val session = new MockSession

  private val queue = new scala.collection.mutable.Queue[EventQueueRecord[_]]

  def expectEventQueueRecord[A]: EventQueueRecord[A] = {
    if (queue.isEmpty) throw new Exception("Expected an EventQueueRecord but queue was empty")
    else {
      val ret = queue.dequeue()
      if (ret.isInstanceOf[EventQueueRecord[A]]) ret.asInstanceOf[EventQueueRecord[A]]
      else throw new Exception("Excpected: " + classOf[EventQueueRecord[A]] + " but was " + ret.getClass)
    }
  }

  def eventQueueSize = queue.size

  private val pool = new MockSessionPool(session)

  final override def newSession = session

  final override def getSessionPool(): SessionPool = pool

  final override def defineEventQueue[A](deserialize: Array[Byte] => A, accept: Event[A] => Unit): Unit =
    throw new Exception("Not implemented")

  final override def defineEventQueueWithNotifier[A](deserialize: Array[Byte] => A, accept: Event[A] => Unit)(notify: String => Unit): Unit =
    queue.enqueue(EventQueueRecord[A](accept, notify))

  final override def bindService(service: AsyncService[_], destination: Destination, competing: Boolean, reactor: Option[Executor]): CloseableChannel = throw new Exception("Unimplemented")

}
