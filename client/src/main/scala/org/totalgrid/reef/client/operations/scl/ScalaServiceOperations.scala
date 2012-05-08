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
package org.totalgrid.reef.client.operations.scl

import org.totalgrid.reef.client.types.TypeDescriptor
import org.totalgrid.reef.client.registration.Service
import org.totalgrid.reef.client.operations._
import org.totalgrid.reef.client._

trait ScalaServiceOperations {

  class RichServiceOperations(ops: ServiceOperations) {

    def operation[A](err: => String)(f: RestOperations => Promise[A]): Promise[A] = {
      ops.request(new BasicRequest[A] {
        def errorMessage(): String = err

        def execute(operations: RestOperations): Promise[A] = f(operations)
      })
    }

    def batchOperation[A](err: => String)(f: RestOperations => Promise[A]): Promise[A] = {
      ops.batchRequest(new BasicRequest[A] {
        def errorMessage(): String = err

        def execute(operations: RestOperations): Promise[A] = f(operations)
      })
    }

    def subscription[A, B](desc: TypeDescriptor[B], err: => String)(fun: (SubscriptionBinding, RestOperations) => Promise[A]): Promise[SubscriptionResult[A, B]] = {
      ops.subscriptionRequest(desc, new SubscriptionBindingRequest[A] {
        def execute(subscription: SubscriptionBinding, operations: RestOperations): Promise[A] = fun(subscription, operations)

        def errorMessage(): String = err
      })
    }

    def clientSideService[A, B](handler: Service, desc: TypeDescriptor[B], err: => String)(fun: (SubscriptionBinding, RestOperations) => Promise[A]): Promise[SubscriptionBinding] = {
      ops.clientServiceBinding(handler, desc, new SubscriptionBindingRequest[A] {
        def execute(binding: SubscriptionBinding, operations: RestOperations): Promise[A] = fun(binding, operations)

        def errorMessage(): String = err
      })
    }
  }

  implicit def _scalaServiceOperations(ops: ServiceOperations): RichServiceOperations = {
    new RichServiceOperations(ops)
  }

}

object ScalaServiceOperations extends ScalaServiceOperations with ScalaPromise with ScalaResponse with ScalaRequestHeaders

