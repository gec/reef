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

package org.totalgrid.reef.messaging

import javaclient.Session
import org.totalgrid.reef.api.scalaclient.{ ClientSessionExecutionPool, ClientSession }
import scala.collection.mutable._
import org.totalgrid.reef.api.javaclient.{ ISessionConsumer, SessionExecutionPool }

class SessionExecutionPoolImpl[A <: { def getClientSession(): ClientSession }](conn: A) extends ClientSessionExecutionPool with SessionExecutionPool {

  private val available = Set.empty[ClientSession]
  private val unavailable = Set.empty[ClientSession]

  private def acquire(): ClientSession = available.synchronized {
    available.lastOption match {
      case Some(s) =>
        available.remove(s)
        unavailable.add(s)
        s
      case None =>
        val s = conn.getClientSession()
        unavailable.add(s)
        s
    }
  }

  private def release(session: ClientSession) = available.synchronized {
    unavailable.remove(session)
    available.add(session)
  }

  override def execute[A](fun: ClientSession => A): A =
    {
      val session = acquire()
      doExecute(fun, session)
    }

  override def execute[A](authToken: String)(fun: ClientSession => A): A =
    {
      execute(doExecute(authToken, fun))
    }

  // implementation of SessionExecutionPool

  override def execute[A](consumer: ISessionConsumer[A]): A =
    {
      execute(client => consumer.apply(new Session(client)))
    }

  override def execute[A](authToken: String, consumer: ISessionConsumer[A]): A =
    {
      execute(authToken)(client => consumer.apply(new Session(client)))
    }

  def shutdown() {}

  private def doExecute[A](authToken: String, function: (ClientSession) => A): (ClientSession) => A =
    { session =>
      try {
        import org.totalgrid.reef.api.ServiceHandlerHeaders._
        session.getDefaultHeaders.setAuthToken(authToken)
        function(session)
      } finally {
        session.getDefaultHeaders.reset
      }
    }

  private def doExecute[A](function: (ClientSession) => A, session: ClientSession): A =
    {
      try {
        function(session)
      } finally {
        release(session)
      }
    }

}