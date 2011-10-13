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
package org.totalgrid.reef.sapi.newclient.impl

import net.agileautomata.executor4s.{ Failure, Success, Result, Future }
import org.totalgrid.reef.japi.{ TypeDescriptor, InternalClientError, ReefServiceException }
import org.totalgrid.reef.sapi.client.Subscription
import org.totalgrid.reef.sapi.newclient._

final class DefaultAnnotatedOperations(client: Client) extends AnnotatedOperations {

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
  final def operation[A](err: => String)(fun: RestOperations => Future[Result[A]]): Promise[A] =
    Promise.from(opWithFuture[A](err)(fun))

  final def subscription[A, B](desc: TypeDescriptor[B], err: => String)(fun: (Subscription[B], RestOperations) => Future[Result[A]]): Promise[SubscriptionResult[A, B]] = {
    val future = client.subscribe(desc) match {
      case Success(sub) =>
        val future = opWithFuture(err)(fun(sub, _))
        future.listen(r => if (r.isFailure) sub.cancel())
        future.map(_.map(a => SubscriptionResult(a, sub)))
      case Failure(ex) =>
        val future = client.future[Result[SubscriptionResult[A, B]]]
        future.set(Failure("Subscribe failed - " + renderErrorMsg(err) + " - " + ex.getMessage))
        future
    }

    Promise.from(future)
  }
}