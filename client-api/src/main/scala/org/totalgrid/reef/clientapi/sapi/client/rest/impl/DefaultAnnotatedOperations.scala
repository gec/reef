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

import net.agileautomata.executor4s.{ Failure, Success, Result, Future }

import org.totalgrid.reef.clientapi.exceptions.{ InternalClientError, ReefServiceException }
import org.totalgrid.reef.clientapi.types.TypeDescriptor

import org.totalgrid.reef.clientapi.sapi.client.{ SubscriptionCreatorManager, Subscription, Promise }
import org.totalgrid.reef.clientapi.sapi.client.rest.{ SubscriptionResult, RestOperations, AnnotatedOperations }
import org.totalgrid.reef.clientapi.javaimpl.SubscriptionWrapper

final class DefaultAnnotatedOperations(client: RestOperations, manager: SubscriptionCreatorManager) extends AnnotatedOperations {

  private def renderErrorMsg(errorMsg: => String): String = {
    try {
      val errorString = errorMsg
      errorString.replaceAll("\n", " ") + " - "
    } catch {
      case e: Exception => "Error rendering extra errorMsg"
    }
  }

  private def opWithFuture[A](err: => String)(fun: RestOperations => Future[Result[A]]): Future[Result[A]] = {
    def convert(r: Result[A]): Result[A] = r match {
      case Success(x) => Success(x)
      case Failure(ex) => ex match {
        case rse: ReefServiceException =>
          rse.addExtraInformation(renderErrorMsg(err))
          Failure(rse)
        case e: Exception => Failure(new InternalClientError("ops() call: unexpected error: " + e.getMessage, e))
      }
    }
    fun(client).map(convert)
  }

  /**
   * Forces the user of this class to provide a descriptive error message for the operation they're performing
   */
  def operation[A](err: => String)(fun: RestOperations => Future[Result[A]]): Promise[A] =
    Promise.from(opWithFuture[A](err)(fun))

  // TODO - it's probably possible to make SubscriptionResult only polymorphic in one type
  def subscription[A, B](desc: TypeDescriptor[B], err: => String)(fun: (Subscription[B], RestOperations) => Future[Result[A]]): Promise[SubscriptionResult[A, B]] = {
    val subscribeFuture = client.subscribe(desc)
    val future = subscribeFuture.await match {
      case Success(sub) =>
        manager.onSubscriptionCreated(new SubscriptionWrapper(sub))
        val opFuture = opWithFuture(err)(fun(sub, _))
        def onResult(r: Result[A]) = {
          if (r.isFailure) sub.cancel()
        }
        opFuture.listen(onResult)
        opFuture.map(_.map(a => SubscriptionResult(a, sub)))
      case Failure(ex) =>
        subscribeFuture.map { r =>
          // TODO: make Failure typable
          Failure("Couldn't create subscribe queue - " + renderErrorMsg(err) + " - " + ex.getMessage).asInstanceOf[Result[SubscriptionResult[A, B]]]
        }
    }

    Promise.from(future)
  }
}