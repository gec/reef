package org.totalgrid.reef.sapi.request.framework

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
import org.totalgrid.reef.japi.{ ReefServiceException, InternalClientError }

import org.totalgrid.reef.messaging.javaclient.{ SubscriptionResultWrapper, SessionWrapper }

import org.totalgrid.reef.japi.client.{ Subscription => JavaSubscription }
import org.totalgrid.reef.sapi.client.{ RestOperations, SubscriptionManagement, Subscription, ClientSession }
import org.totalgrid.reef.japi.client._
import com.google.protobuf.GeneratedMessage
import org.totalgrid.reef.promise.{ FixedPromise, Promise => Prom }

// TODO rename BaseReefService or similar, class is redundant.
trait ReefServiceBaseClass extends ClientSource with SubscriptionCreator {

  def useSubscription[S, R](session: SubscriptionManagement, klass: Class[_])(block: Subscription[R] => Prom[S]): Prom[SubscriptionResult[S, R]] = {
    val sub: Subscription[R] = session.addSubscription[R](klass)
    try {

      val result = block(sub)
      val ret = new FixedPromise[SubscriptionResult[S, R]](new SubscriptionResultWrapper(result.await, sub))
      onSubscriptionCreated(ret.await.getSubscription)
      ret
    } catch {
      case x =>
        sub.cancel()
        throw x
    }
  }

  private var creationListeners = List.empty[SubscriptionCreationListener]

  private def onSubscriptionCreated(sub: JavaSubscription[_]) {
    creationListeners.foreach(_.onSubscriptionCreated(sub))
  }

  def addSubscriptionCreationListener(listener: SubscriptionCreationListener) {
    creationListeners ::= listener
  }
}

/**
 * base trait for implementations that need a ClientSession and don't want to specify
 * if we are using a pooled or not implementation
 */
trait ClientSource {

  // TODO - find a type-safe replacement
  def convertByCasting(session: Session): RestOperations with SubscriptionManagement = session.asInstanceOf[SessionWrapper].client

  protected def ops[A](errorMsg: => String)(block: RestOperations with SubscriptionManagement => Prom[A]): Prom[A] = {
    val original = _ops(block)
    new Prom[A] {
      def await() = catchErrors(errorMsg) { original.await }

      // TODO: what should we do on listen with exceptions
      def listen(fun: (A) => Unit) = catchErrors(errorMsg) { original.listen(fun) }

      def isComplete = original.isComplete
    }
  }

  private def catchErrors[A](errorMsg: String)(func: => A): A = {
    try {
      func
    } catch {
      case rse: ReefServiceException =>
        // we are just trying to verify that only ReefService derived Execeptions bubble out of the
        // calls, if its already a ReefServiceException we have nothing to do
        rse.addExtraInformation(renderErrorMsg(errorMsg))
        throw rse
      case e: Exception =>
        val ice = new InternalClientError("ops() call: unexpected error: " + e.getMessage, e)
        ice.addExtraInformation(renderErrorMsg(errorMsg))
        throw ice
    }
  }

  private def renderErrorMsg(errorMsg: => String): String = {
    try {
      val errorString = errorMsg
      errorString.replaceAll("\n", " ") + " - "
    } catch {
      case e: Exception => "Error rendering extra errorMsg"
    }
  }

  // TODO: make _ops protected again once we figure out client creation plan
  def _ops[A](block: RestOperations with SubscriptionManagement => A): A
}

/**
 * simplest implementation of ClientSource, just hands the same session for every request
 * without attaching any extra information
 */
trait SingleSessionClientSource extends ClientSource {
  def session: RestOperations with SubscriptionManagement

  override def _ops[A](block: RestOperations with SubscriptionManagement => A): A = block(session)
}

/**
 * temporary solution to generating a new interface from existing impl
 * TODO: remove ClientSourceProxy
 */
trait ClientSourceProxy extends ClientSource {
  protected def clientSource: ClientSource
  override def _ops[A](block: RestOperations with SubscriptionManagement => A): A = clientSource._ops(block)
}

/**
 * takes a single session and sets the authToken before each call and removes it afterwards
 */
trait AuthorizedSingleSessionClientSource extends ClientSource {

  def session: ClientSession
  def authToken: String

  override def _ops[A](block: RestOperations with SubscriptionManagement => A): A = {
    try {
      session.getDefaultHeaders.setAuthToken(authToken)
      block(session)
    } finally {
      session.getDefaultHeaders.clear()
    }
  }
}

/**
 * uses a sessionpool to acquire a session out of a pool for each call
 */
trait PooledClientSource extends ClientSource {

  // TODO: examine pooling implementation to obviate need for ISessionConsumer wrapper
  def sessionPool: SessionExecutionPool

  override def _ops[A](block: RestOperations with SubscriptionManagement => A): A = {
    sessionPool.execute(new SessionFunction[A] {
      def apply(session: Session) = block(convertByCasting(session))
    })
  }
}

/**
 * uses a sessionpool to acquire a session out of a pool for each call and also attaches an
 * authtoken before every call
 */
trait AuthorizedAndPooledClientSource extends ClientSource {

  def sessionPool: SessionExecutionPool
  def authToken: String

  override def _ops[A](block: RestOperations with SubscriptionManagement => A): A = {
    sessionPool.execute(authToken, new SessionFunction[A] {
      def apply(session: Session) = block(convertByCasting(session))
    })
  }
}

