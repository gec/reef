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
import impl.DefaultServiceOperations.{ DefaultSubscriptionResult, CancelingListener }
import java.util.concurrent.RejectedExecutionException
import org.totalgrid.reef.client.exception.{ ServiceIOException, InternalClientError, ReefServiceException }
import org.totalgrid.reef.client._
import scl.ScalaPromise._

object DefaultServiceOperations {

  class CancelingListener[A](cancel: () => Unit) extends PromiseListener[A] {
    def onComplete(promise: Promise[A]) {
      try {
        promise.await() // TODO: better way?
      } catch {
        case x => cancel()
      }
    }
  }

  class DefaultSubscriptionResult[A, B](result: A, sub: Subscription[B]) extends SubscriptionResult[A, B] {
    def getResult: A = result
    def getSubscription: Subscription[B] = sub
  }
}

class DefaultServiceOperations(restOperations: RestOperations, bindOperations: BindOperations, exe: Executor) extends ServiceOperations {

  private def safeOp[A](op: () => Promise[A], err: () => String): Promise[A] = {
    /*val annotateError = new PromiseErrorTransform {
      def transformError(error: ReefServiceException): ReefServiceException = {
        error.addExtraInformation(err())
        error
      }
    }*/


    try {
      //op().transformError(annotateError)
      op().mapError { rse => rse.addExtraInformation(err()); rse }
    } catch {
      case npe: NullPointerException =>
        FuturePromise.error[A](new InternalClientError("Null pointer error while making request. Check that all parameters are not null.", npe), exe)
      case rje: RejectedExecutionException =>
        FuturePromise.error[A](new ServiceIOException("Underlying connection executor has been closed or disconnected", rje), exe)
      case rse: ReefServiceException => FuturePromise.error[A](rse, exe)
      case ex: Exception => FuturePromise.error[A](new InternalClientError("Unexpected error: " + ex.getMessage, ex), exe)
    }
  }

  def operation[A](operation: BasicOperation[A]): Promise[A] = {
    safeOp(() => operation.execute(restOperations), operation.errorMessage _)
  }

  def subscription[A, B](descriptor: TypeDescriptor[B], operation: SubscribeOperation[A]): Promise[SubscriptionResult[A, B]] = {
    try {
      val sub: Subscription[B] = bindOperations.subscribe(descriptor)
      val promise: Promise[A] = safeOp(() => operation.execute(sub, restOperations), operation.errorMessage _)
      promise.listen(new CancelingListener(sub.cancel _))
      promise.map(v => new DefaultSubscriptionResult(v, sub))

    } catch {
      case ex: Exception => FuturePromise.error[SubscriptionResult[A, B]](new InternalClientError("Couldn't create subscribe queue." + ex.getMessage, ex), exe)
    }
  }

  def clientServiceBinding[A, B](service: Service, descriptor: TypeDescriptor[A], operation: ClientServiceBindingOperation[B]): Promise[SubscriptionBinding] = {
    try {
      val binding = bindOperations.lateBindService(service, descriptor)
      val promise: Promise[B] = safeOp(() => operation.execute(binding, restOperations), operation.errorMessage _)
      promise.listen(new CancelingListener(binding.cancel _))
      promise.map(result => binding)

    } catch {
      case ex: Exception => FuturePromise.error[SubscriptionBinding](
        new InternalClientError("Couldn't bind client service handler." + ex.getMessage, ex), exe)
    }
  }

}
