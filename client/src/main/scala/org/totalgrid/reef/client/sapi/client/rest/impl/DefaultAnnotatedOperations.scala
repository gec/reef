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
package org.totalgrid.reef.client.sapi.client.rest.impl

import net.agileautomata.executor4s._

import java.util.concurrent.RejectedExecutionException
import org.totalgrid.reef.client.exception.{ ServiceIOException, InternalClientError, ReefServiceException }

import org.totalgrid.reef.client.sapi.client.Promise

/**
 * object provides the stateless functions to do a complex futures operation
 * safeley and attach a helpful error message if something goes wrong.
 */
object DefaultAnnotatedOperations {
  // TODO: remove last usages of DefaultAnnotatedOperations
  def renderErrorMsg(errorMsg: => String): String = {
    try {
      val errorString = errorMsg
      errorString.replaceAll("\n", " ") + " - "
    } catch {
      case e: Exception => "Error rendering extra errorMsg"
    }
  }

  def definedFuture[A](exe: Executor, value: A): Future[A] = {
    val future = exe.future[A]
    future.set(value)
    future
  }

  private def safeOpWithFuture[A, B](err: => String, exe: Executor)(fun: => Future[Result[A]]): Future[Result[A]] = {
    def convert(r: Result[A]): Result[A] = r match {
      case Success(x) => Success(x)
      case Failure(ex) => ex match {
        case rse: ReefServiceException =>
          rse.addExtraInformation(renderErrorMsg(err))
          Failure(rse)
        case e: Exception => Failure(new InternalClientError("ops() call: unexpected error: " + e.getMessage, e))
      }
    }

    try {
      fun.map(convert)
    } catch {
      case npe: NullPointerException =>
        definedFuture[Result[A]](exe, Failure(new InternalClientError("Null pointer error while making request. " +
          "Check that all parameters are not null.", npe)))
      case rje: RejectedExecutionException =>
        definedFuture[Result[A]](exe, Failure(new ServiceIOException("Underlying connection executor has been closed or disconnected", rje)))
      case rse: ReefServiceException =>
        definedFuture[Result[A]](exe, Failure(rse))
      case ex: Exception =>
        definedFuture[Result[A]](exe, Failure(new InternalClientError("ops() function: unexpected error: " + ex.getMessage, ex)))
    }
  }

  /**
   * tries to execute the given function and catches all errors to create helpful error messages.
   */
  def safeOperation[A, B](err: => String, exe: Executor)(fun: => Future[Result[A]]): Promise[A] =
    Promise.from(safeOpWithFuture(err, exe)(fun))
}