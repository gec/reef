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
package org.totalgrid.reef.clientapi.sapi.client.rest

import org.totalgrid.reef.clientapi.sapi.client._
import org.totalgrid.reef.clientapi.proto.Envelope.Verb
import org.totalgrid.reef.clientapi.types.TypeDescriptor

import net.agileautomata.executor4s.{ Result, Future }

trait RestOperations {

  def request[A](verb: Verb, payload: A, headers: Option[BasicRequestHeaders]): Future[Response[A]]

  final def get[A](payload: A, headers: BasicRequestHeaders) = request(Verb.GET, payload, Some(headers))
  final def delete[A](payload: A, headers: BasicRequestHeaders) = request(Verb.DELETE, payload, Some(headers))
  final def post[A](payload: A, headers: BasicRequestHeaders) = request(Verb.POST, payload, Some(headers))
  final def put[A](payload: A, headers: BasicRequestHeaders) = request(Verb.PUT, payload, Some(headers))

  final def get[A](payload: A) = request(Verb.GET, payload, None)
  final def delete[A](payload: A) = request(Verb.DELETE, payload, None)
  final def post[A](payload: A) = request(Verb.POST, payload, None)
  final def put[A](payload: A) = request(Verb.PUT, payload, None)

  /**
   * subscribe returns a Future to the result that is always going to be set when it is returned, it is
   * returned as a future so a client who wants to listen to the SubscriptionResult will get the event
   * on the same dispatcher as the result would come on
   */
  def subscribe[A](descriptor: TypeDescriptor[A]): Future[Result[Subscription[A]]]
}