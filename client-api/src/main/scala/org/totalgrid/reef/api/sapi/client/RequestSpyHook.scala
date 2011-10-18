package org.totalgrid.reef.api.sapi.client

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

import rest.RestOperations
import org.totalgrid.reef.api.japi.Envelope
import net.agileautomata.executor4s.Future

/**
 * inserts itself into the request chain so we can see every request and its results
 */
trait RequestSpyHook extends RestOperations with RequestSpyManager with DefaultHeaders {

  override def addRequestSpy(listener: RequestSpy) = requestSpys ::= listener
  override def removeRequestSpy(listener: RequestSpy) = requestSpys = requestSpys.filterNot(_ == listener)

  protected var requestSpys = List.empty[RequestSpy]

  abstract override def request[A](
    verb: Envelope.Verb,
    request: A,
    headers: BasicRequestHeaders): Future[Response[A]] = {

    val future = super.request(verb, request, headers)

    requestSpys.foreach(_.onRequestReply(verb, request, future))

    future
  }
}