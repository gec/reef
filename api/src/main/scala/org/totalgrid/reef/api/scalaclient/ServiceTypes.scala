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
package org.totalgrid.reef.api.scalaclient

import org.totalgrid.reef.api.{ Envelope, RequestEnv, IDestination, AnyNode }

import org.totalgrid.reef.api.javaclient.IEvent

/* ---- Case classes that make the service api easier to use ---- */

case class Request[A](verb: Envelope.Verb, payload: A, env: RequestEnv = new RequestEnv, destination: IDestination = AnyNode)

case class Response[A](status: Envelope.Status = Envelope.Status.INTERNAL_ERROR, result: List[A] = Nil, error: String = "")

case class Event[A](event: Envelope.Event, value: A) extends IEvent[A] {

  final override def getEventType() = event
  final override def getValue() = value

}

