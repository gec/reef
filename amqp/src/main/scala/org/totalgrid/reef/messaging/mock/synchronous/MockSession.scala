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
import org.totalgrid.reef.japi.Envelope
import org.totalgrid.reef.sapi.client._
import java.lang.Exception
import org.totalgrid.reef.promise.{ FixedPromise, Promise }

object MockSession {
  type RequestHandler[A] = Request[A] => Response[A]
}

class MockSession extends ClientSession {

  import MockSession._

  private case class Record[A](request: Request[A], promise: SimplePromise[Response[A]])

  class SimplePromise[A](default: A) extends Promise[A] {

    private var option: Option[A] = None
    private var list: List[A => Unit] = Nil

    def set(value: A) = option match {
      case Some(x) => throw new Exception("Value has already been set to: " + value)
      case None =>
        option = Some(value)
        list.foreach(_.apply(value))
    }

    def await(): A = option match {
      case Some(x) => x
      case None => default
    }

    def listen(fun: A => Unit): Unit = option match {
      case Some(x) => fun(x)
      case None => list = fun :: list
    }

    def isComplete: Boolean = option.isDefined
  }

  final override def request[A](verb: Envelope.Verb, payload: A, env: RequestEnv, dest: Destination): Promise[Response[A]] = {
    if (!open) throw new IllegalStateException("Session is not open")
    val request = Request[A](verb, payload, env, dest)
    nextHandler[A]() match {
      case Some(handler) => new FixedPromise[Response[A]](handler(request))
      case None =>
        val promise = new SimplePromise[Response[A]](Failure(Envelope.Status.RESPONSE_TIMEOUT))
        requests.enqueue(Record[A](request, promise))
        promise
    }
  }

  final override def close() = open = false

  final override def isOpen = open

  final override def addSubscription[A](klass: Class[_]): Subscription[A] = throw new Exception("Unimplemented")

  // Testing functions

  private var open = true

  private val requests = new scala.collection.mutable.Queue[Record[_]]

  private val responses = new scala.collection.mutable.Queue[RequestHandler[_]]

  private def nextHandler[A](): Option[RequestHandler[A]] = {
    if (responses.isEmpty) None
    else {
      if (responses.front.isInstanceOf[RequestHandler[A]]) Some(responses.dequeue().asInstanceOf[RequestHandler[A]])
      else throw new Exception("Synchronous response types do not match, found" + responses.front)
    }
  }

  def numResponsesPending = responses.size
  def numRequestsPending = requests.size

  def queueResponse[A](fun: RequestHandler[A]): Unit = responses.enqueue(fun)

  def respond[A](fun: RequestHandler[A]): Request[A] = {
    if (requests.size == 0) throw new Exception("No requests, so can't respond")
    val record = requests.dequeue().asInstanceOf[Record[A]]
    record.promise.set(fun(record.request))
    record.request
  }

}