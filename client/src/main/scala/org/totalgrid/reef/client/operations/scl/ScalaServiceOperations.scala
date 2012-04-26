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

import org.totalgrid.reef.client.Promise
import org.totalgrid.reef.client.operations.{ BasicOperation, RestOperations, ServiceOperations }

object ScalaServiceOperations {

  class RichServiceOperations(ops: ServiceOperations) {

    def operation[A](err: => String)(f: RestOperations => Promise[A]): Promise[A] = {
      ops.operation(new BasicOperation[A] {
        def errorMessage(): String = err

        def execute(operations: RestOperations): Promise[A] = f(operations)
      })
    }
  }

  implicit def _scalaServiceOperations[A](ops: ServiceOperations): RichServiceOperations = {
    new RichServiceOperations(ops)
  }

}

/*
/**
   * Perform an operation with a RestOperations class.
   * @param err If an error occurs, this message is attached to the exception
   * @param fun function that uses the client to generate a result
   */
  def operation[A](err: => String)(fun: RestOperations => Future[Result[A]]): Promise[A]

  /**
   * Similiar to operation. Does a rest operation with a subscription. If the operation fails, the subscription is automatically canceled
   * @param desc TypeDescriptor for the subscription type.
   * @param err If an error occurs, this message is attached to the exception
   * @param fun function that uses the client and subscription to generate a result
   */
  def subscription[A, B](desc: TypeDescriptor[B], err: => String)(fun: (Subscription[B], RestOperations) => Future[Result[A]]): Promise[SubscriptionResult[A, B]]

  /**
   * Does a rest operation with a localServiceBinding. Almost the same as a subscription, but we keep track of the sender
   * and respond back to the requester.
   * @param handler Service handler for all client handled service requests.
   * @param err If an error occurs, this message is attached to the exception
   * @param fun function that uses the client and subscription to generate a result
   */
  def clientSideService[A, B](handler: AsyncService[B], err: => String)(fun: (SubscriptionBinding, RestOperations) => Future[Result[A]]): Promise[SubscriptionBinding]
 */
