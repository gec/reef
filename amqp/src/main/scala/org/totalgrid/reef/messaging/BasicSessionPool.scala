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
package org.totalgrid.reef.messaging

import scala.collection.mutable._
import org.totalgrid.reef.sapi.client.{ ClientSession, SessionPool }
import org.totalgrid.reef.japi.client.{ SessionFunction, SessionExecutionPool }
import org.totalgrid.reef.messaging.javaclient.SessionWrapper

class BasicSessionPool[A <: { def getClientSession(): ClientSession }](conn: A) extends SessionPool with SessionExecutionPool {

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

  override def borrow[A](fun: ClientSession => A): A = {

    val session = acquire()

    try {
      fun(session)
    } finally {
      release(session)
    }

  }

  override def borrow[A](authToken: String)(fun: ClientSession => A): A = {

    borrow { session =>
      try {
        session.getDefaultHeaders.setAuthToken(authToken)
        fun(session)
      } finally {
        session.getDefaultHeaders.clear()
      }
    }

  }

  // implement Java SessionExecutionPool
  final override def execute[A](function: SessionFunction[A]): A = {
    borrow(client => function.apply(new SessionWrapper(client)))
  }

  final override def execute[A](authToken: String, function: SessionFunction[A]): A = {
    borrow(authToken)(client => function.apply(new SessionWrapper(client)))
  }
}