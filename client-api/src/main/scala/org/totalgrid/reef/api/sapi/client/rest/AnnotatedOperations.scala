package org.totalgrid.reef.api.sapi.client.rest

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

import org.totalgrid.reef.api.japi.TypeDescriptor

import net.agileautomata.executor4s.{ Future, Result }
import org.totalgrid.reef.api.sapi.client.{ Promise, Subscription }

/**
 *  Defines an interface for composite subcribe/rest operations. Used in the RPC API implementations.
 */
trait AnnotatedOperations {

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
}