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
package org.totalgrid.reef.sapi.client.requestspys

import org.totalgrid.reef.api.japi.Envelope.Verb
import org.totalgrid.reef.api.sapi.client.{Response, RequestSpy}
import net.agileautomata.executor4s.Future

/**
 * simple request spy that prints out all requests and responses.
 * Can be integrated with logging frame work:
 * <code>
 *   new LoggingRequestSpy(logger.info _)
 * </code>
 */
class LoggingRequestSpy(func: String => Unit) extends RequestSpy {

  def onRequestReply[A](verb: Verb, request: A, response: Future[Response[A]]) = {
    val str = verb.toString + " => " + request + " <= " + response.await
    func(str)
  }
}