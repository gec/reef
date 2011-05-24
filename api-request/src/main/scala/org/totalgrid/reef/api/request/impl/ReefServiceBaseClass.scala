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
package org.totalgrid.reef.api.request.impl

import org.totalgrid.reef.api.scalaclient.{ ClientSession, SubscriptionManagement, SyncOperations }
import com.google.protobuf.GeneratedMessage
import org.totalgrid.reef.api.{ Subscription, InternalClientError, ReefServiceException, ExpectationException }
import org.totalgrid.reef.messaging.javaclient.SubscriptionResult
import org.totalgrid.reef.api.javaclient.{ ISubscription, ISession, ISessionConsumer, SessionExecutionPool }
import java.util.concurrent.CopyOnWriteArrayList
import java.util.{ NoSuchElementException, Iterator }

// TODO rename BaseReefService or similar, class is redundant.
trait ReefServiceBaseClass extends SessionSource {

  private val listeners: CopyOnWriteArrayList[SubscriptionListener] = new CopyOnWriteArrayList[SubscriptionListener]()

  def addListener(listener: SubscriptionListener): Boolean =
    {
      listeners.add(listener)
    }

  def useSubscription[S, R <: GeneratedMessage](session: SubscriptionManagement, klass: Class[_])(block: Subscription[R] => S): SubscriptionResult[S, R] = {
    val subscription: Subscription[R] = session.addSubscription[R](klass)
    try {
      val result = block(subscription)
      val subscriptionResult = new SubscriptionResult(result, subscription)
      notifyListeners(subscriptionResult)
      subscriptionResult
    } catch {
      case x =>
        subscription.cancel
        throw x
    }
  }

  protected def reThrowExpectationException[R](why: => String)(f: => R): R = {
    try {
      f
    } catch {
      case e: ExpectationException => throw new ExpectationException(why)
    }
  }

  private def notifyListeners[S, R <: GeneratedMessage](subscriptionResult: SubscriptionResult[S, R]) {
    val iterator: Iterator[SubscriptionListener] = listeners.iterator()
    while (iterator.hasNext) {
      try {
        val listener: SubscriptionListener = iterator.next()
        if (listener != null) {
          listener.onNewSubscription(subscriptionResult.getSubscription)
        }
      } catch {
        case e: NoSuchElementException => {}
      }
    }
  }
}

trait SubscriptionListener {
  def onNewSubscription[T <: GeneratedMessage](subscription: ISubscription[T]): Unit
}

/**
 * base trait for implementations that need a ClientSession and don't want to specify
 * if we are using a pooled or not implementation
 */
trait SessionSource {
  protected def ops[A](block: SyncOperations with SubscriptionManagement => A): A = {
    try {
      _ops(block)
    } catch {
      case rse: ReefServiceException =>
        // we are just trying to verify that only ReefService derived Execeptions bubble out of the
        // calls, if its already a ReefServiceException we have nothing to do
        throw rse
      case e: Exception =>
        throw new InternalClientError("ops() call: unexpected error: " + e.getMessage, e)
    }
  }

  protected def _ops[A](block: SyncOperations with SubscriptionManagement => A): A
}

/**
 * simplest implementation of ClientSource, just hands the same session for every request
 * without attaching any extra information
 */
trait SingleSessionClientSource extends SessionSource {
  def session: SyncOperations with SubscriptionManagement

  override def _ops[A](block: SyncOperations with SubscriptionManagement => A): A = block(session)
}

/**
 * takes a single session and sets the authToken before each call and removes it afterwards
 */
trait AuthorizedSingleSessionClientSource extends SessionSource {

  def session: ClientSession
  def authToken: String

  override def _ops[A](block: SyncOperations with SubscriptionManagement => A): A = {
    try {
      import org.totalgrid.reef.api.ServiceHandlerHeaders._
      session.getDefaultHeaders.setAuthToken(authToken)
      block(session)
    } finally {
      session.getDefaultHeaders.reset
    }
  }
}

/**
 * uses a sessionpool to acquire a session out of a pool for each call
 */
trait PooledClientSource extends SessionSource {

  // TODO: examine pooling implementation to obviate need for ISessionConsumer wrapper
  def sessionPool: SessionExecutionPool

  override def _ops[A](block: SyncOperations with SubscriptionManagement => A): A = {
    sessionPool.execute(new ISessionConsumer[A] {
      def apply(session: ISession) = block(session.getUnderlyingClient)
    })
  }
}

/**
 * uses a sessionpool to acquire a session out of a pool for each call and also attaches an
 * authtoken before every call
 */
trait AuthorizedAndPooledClientSource extends SessionSource {

  def sessionPool: SessionExecutionPool
  def authToken: String

  override def _ops[A](block: SyncOperations with SubscriptionManagement => A): A = {
    sessionPool.execute(authToken, new ISessionConsumer[A] {
      def apply(session: ISession) = block(session.getUnderlyingClient)
    })
  }
}

