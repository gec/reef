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

import org.totalgrid.reef.japi.client.{ SessionFunction, SessionExecutionPool }
import org.totalgrid.reef.messaging.javaclient.SessionWrapper
import org.totalgrid.reef.util.Logging
import org.totalgrid.reef.japi.Envelope.Verb
import org.totalgrid.reef.sapi._
import client._
import org.totalgrid.reef.japi.{ InternalClientError, Envelope, ReefServiceException }
import org.totalgrid.reef.promise.{ FixedPromise, Promise }

class BasicSessionPool(source: SessionSource) extends SessionPool with SessionExecutionPool with Logging {

  class ErroredClientSession(exception: ReefServiceException) extends ClientSession {

    final override def request[A](verb: Verb, payload: A, env: BasicRequestHeaders): Promise[Response[A]] =
      new FixedPromise[Response[A]](FailureResponse(Envelope.Status.BUS_UNAVAILABLE, "The session pool could not obtain a session: " + exception.toString))

    final override def isOpen: Boolean = false

    final override def close() = {}

    final override def addSubscription[A](klass: Class[_]): Subscription[A] = throw new InternalClientError("Bus is unavailable", exception)
  }

  private val available = scala.collection.mutable.Set.empty[ClientSession]
  private var count = 0

  final override def size = count

  private def acquire(): ClientSession = available.synchronized {
    available.lastOption match {
      case Some(session) =>
        available.remove(session)
        session
      case None => getNewSession()
    }
  }

  private def getNewSession(): ClientSession = {
    try {
      val session = source.newSession()
      count += 1
      session
    } catch {
      case rse: ReefServiceException =>
        logger.warn("Exception getting new ClientSession", rse)
        new ErroredClientSession(rse)
    }
  }

  private def release(session: ClientSession) = available.synchronized {
    session match {
      case ex: ErroredClientSession => // do nothing
      case _ =>
        // if the session somehow gets closed, we discard it
        if (session.isOpen) available.add(session)
        else count -= 1
    }

  }

  final override def borrow[A](fun: ClientSession => A): A = {

    val session = acquire()

    try {
      fun(session)
    } finally {
      release(session)
    }

  }

  final override def borrow[A](authToken: String)(fun: ClientSession => A): A = borrow { session =>
    val headers = session.getHeaders
    try {
      session.modifyHeaders(_.setAuthToken(authToken))
      fun(session)
    } finally {
      session.setHeaders(headers)
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

