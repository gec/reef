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
package org.totalgrid.reef.api.scalaclient.mock

import org.totalgrid.reef.api.scalaclient.{ ClientOperations, Response }
import org.totalgrid.reef.api.{ Envelope, RequestEnv, IDestination }

import scala.collection.mutable.Queue

case class RequestRecord[A](verb: Envelope.Verb, payload: A, env: RequestEnv, callback: Response[A] => Unit)

class MockClientOperations extends ClientOperations {

  private val queue = new Queue[RequestRecord[_]]

  def asyncRequest[A](verb: Envelope.Verb, payload: A, env: RequestEnv, dest: IDestination)(callback: Response[A] => Unit) {
    queue += RequestRecord(verb, payload, env, callback)
  }

  def next(): AnyRef = queue.dequeue
  def callback[A <: AnyRef](result: Response[A]) = queue.front.callback.asInstanceOf[Response[A] => Unit](result)

}