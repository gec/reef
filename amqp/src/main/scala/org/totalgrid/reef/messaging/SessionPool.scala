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
import org.totalgrid.reef.api.scalaclient.{ ClientSessionPool, ClientSession }
import scala.collection.mutable._
import org.totalgrid.reef.api.javaclient.{ ISessionConsumer, ISessionPool }

class SessionPool[A <: { def getClientSession(): ClientSession }](conn: A) extends ClientSessionPool with ISessionPool {

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

  def borrow[A](fun: ClientSession => A): A = {

    val session = acquire()

    try {
      fun(session)
    } finally {
      release(session)
    }

  }

  def borrow[A](authToken: String)(fun: ClientSession => A): A = {

    borrow { session =>
      try {
        import org.totalgrid.reef.api.ServiceHandlerHeaders._
        session.getDefaultHeaders.setAuthToken(authToken)
        fun(session)
      } finally {
        session.getDefaultHeaders.reset
      }
    }

  }

  def borrow[A](consumer: ISessionConsumer[A]): A = borrow(client => consumer.apply(new Session(client)))

  def borrow[A](consumer: ISessionConsumer[A], authToken: String): A = borrow(authToken)(client => consumer.apply(new Session(client)))

  def getUnderlyingClientSessionPool: ClientSessionPool = this
}