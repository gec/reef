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
package org.totalgrid.reef.messaging.mock.synchronous

import org.totalgrid.reef.messaging.mock.MockSessionPool
import org.totalgrid.reef.messaging.Connection
import org.totalgrid.reef.api.scalaclient.{ Event, ISessionPool }
import org.totalgrid.reef.api.service.IServiceAsync
import org.totalgrid.reef.api.IDestination
import org.totalgrid.reef.reactor.Reactable

class MockConnection extends Connection {

  val session = new MockSession

  private val pool = new MockSessionPool(session)

  def getSessionPool(): ISessionPool = pool

  def defineEventQueue[A](deserialize: Array[Byte] => A, accept: Event[A] => Unit): Unit = throw new Exception("Unimplemented")

  def defineEventQueueWithNotifier[A](deserialize: Array[Byte] => A, accept: Event[A] => Unit)(notify: String => Unit): Unit = throw new Exception("Unimplemented")

  def bindService(service: IServiceAsync[_], destination: IDestination, competing: Boolean, reactor: Option[Reactable]): Unit = throw new Exception("Unimplemented")

}
