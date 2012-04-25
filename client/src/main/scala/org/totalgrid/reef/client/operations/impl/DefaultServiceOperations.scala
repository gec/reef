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
package org.totalgrid.reef.client.operations.impl

import org.totalgrid.reef.client.registration.Service
import org.totalgrid.reef.client.types.TypeDescriptor
import net.agileautomata.executor4s.Executor
import org.totalgrid.reef.client.operations._
import java.util.concurrent.RejectedExecutionException
import org.totalgrid.reef.client.exception.{ ServiceIOException, InternalClientError, ReefServiceException }
import org.totalgrid.reef.client._

class DefaultServiceOperations(restOperations: RestOperations, clientBindOperations: ClientServiceBindingOperation, exe: Executor) extends ServiceOperations {

  def operation[A](operation: BasicOperation): Promise[A] = {

    val annotateError = new PromiseErrorTransform {
      def transformError(error: ReefServiceException): ReefServiceException = {
        error.addExtraInformation(operation.errorMessage())
        error
      }
    }

    try {
      operation.execute(restOperations).transformError(annotateError)
    } catch {
      case npe: NullPointerException =>
        FuturePromise.error[A](new InternalClientError("Null pointer error while making request. Check that all parameters are not null.", npe), exe)
      case rje: RejectedExecutionException =>
        FuturePromise.error[A](new ServiceIOException("Underlying connection executor has been closed or disconnected", rje), exe)
      case rse: ReefServiceException => FuturePromise.error[A](rse, exe)
      case ex: Exception => FuturePromise.error[A](new InternalClientError("Unexpected error: " + ex.getMessage, ex), exe)
    }
  }

  def subscription[T, U](descriptor: TypeDescriptor[U], operation: SubscribeOperation): Promise[SubscriptionResult[T, U]] = null

  def clientServiceBinding[T](service: Service, operation: ClientServiceBindingOperation): Promise[SubscriptionBinding] = null

}
