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
package org.totalgrid.reef.api.sapi.client

import org.totalgrid.reef.api.japi.{ ServiceIOException, Envelope }
import net.agileautomata.executor4s.Future

/**
 * a RequestSpy can watch the stream of requests and monitor the results. We can add spys
 * to measure latency, add logging statements or ui enhancements. The spy will be called using
 * the original callers thread.
 */
trait RequestSpy {
  def onRequestReply[A](verb: Envelope.Verb, request: A, response: Future[Response[A]])
}

/**
 * clients capable of calling a spy with every request will override this method
 */
trait RequestSpyManager {
  def addRequestSpy(listener: RequestSpy): Unit = throw new ServiceIOException("Request Spy unsupported by this client")
  def removeRequestSpy(listener: RequestSpy): Unit = {}
}

/**
 * helpers to make using a requestSpy easier
 */
object RequestSpy {

  /**
   * attaches a spy, runs a block of code and then makes sure it is removed
   */
  def withRequestSpy[A, B <: RequestSpyManager](client: B, spy: RequestSpy)(func: => A): A =
    withRequestSpy(client, Some(spy))(func)

  /**
   * optionally attaches a spy, runs code then removes spy afterwards. Makes it simpler to
   * have single code path in client code that may or may not actually want to attach spy
   */
  def withRequestSpy[A, B <: RequestSpyManager](client: B, spy: Option[RequestSpy])(func: => A): A = {
    try {
      spy.foreach { client.addRequestSpy(_) }

      func

    } finally {
      spy.foreach { client.removeRequestSpy(_) }
    }
  }
}