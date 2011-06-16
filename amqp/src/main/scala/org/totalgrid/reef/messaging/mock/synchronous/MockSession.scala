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
package org.totalgrid.reef.messaging.mock.synchronous

import org.totalgrid.reef.sapi.{ Destination, RequestEnv }
import java.lang.Exception
import org.totalgrid.reef.japi.Envelope
import org.totalgrid.reef.sapi.client._

class MockSession extends ClientSession with AsyncRestAdapter {

  // implement the ClientSession trait

  final override def asyncRequest[A](verb: Envelope.Verb, payload: A, env: RequestEnv = getDefaultHeaders, dest: Destination)(callback: Response[A] => Unit) = {
    if (!open) throw new IllegalStateException("Session is not open")
    queue.enqueue(Record[A](Request(verb, payload, env, dest), callback))
  }

  final override def close() = open = false

  final override def isOpen = open

  final override def addSubscription[A](klass: Class[_]): Subscription[A] = throw new Exception("Unimplemented")

  // Testing functions

  private var open = true
  case class Record[A](request: Request[A], callback: Response[A] => Unit)
  private val queue = new scala.collection.mutable.Queue[Any]

  def size = queue.size

  def respond[A](fun: Request[A] => Response[A]) = {
    if (queue.size == 0) throw new Exception("No requests, so can't respond")
    val record = queue.dequeue().asInstanceOf[Record[A]]
    record.callback(fun(record.request))
  }

}