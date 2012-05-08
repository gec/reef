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
package org.totalgrid.reef.client.operations.impl

import org.totalgrid.reef.client.operations.{ Response, RestOperations }
import org.totalgrid.reef.client.{ RequestHeaders, SubscriptionBinding, Promise }
import org.totalgrid.reef.client.proto.Envelope.Verb
import org.totalgrid.reef.client.sapi.client.BasicRequestHeaders
import org.totalgrid.reef.client.sapi.client.rest.impl.DefaultClient

class DefaultRestOperations(client: DefaultClient) extends RestOperations with DerivedRestOperations with OptionallyBatchedRestOperations {

  def batched: Option[BatchRestOperations] = None

  protected def request[A](verb: Verb, payload: A, headers: Option[RequestHeaders]): Promise[Response[A]] = {
    client.requestJava(verb, payload, headers)
  }

}

trait DerivedRestOperations {
  protected def request[A](verb: Verb, payload: A, headers: Option[RequestHeaders]): Promise[Response[A]]

  def request[A](verb: Verb, payload: A, subscriptionBinding: Option[SubscriptionBinding], headers: Option[RequestHeaders]): Promise[Response[A]] = {
    val basic = subscriptionBinding.map(sb => headers.getOrElse(BasicRequestHeaders.empty).setSubscribeQueue(sb.getId)).orElse(headers)

    request(verb, payload, basic)
  }

  def request[A](verb: Verb, payload: A, subscriptionBinding: SubscriptionBinding): Promise[Response[A]] = {
    request(verb, payload, Some(subscriptionBinding), None)
  }
  def request[A](verb: Verb, payload: A, subscriptionBinding: SubscriptionBinding, headers: RequestHeaders): Promise[Response[A]] = {
    request(verb, payload, Some(subscriptionBinding), Some(headers))
  }
  def request[A](verb: Verb, payload: A, headers: RequestHeaders): Promise[Response[A]] = {
    request(verb, payload, None, Some(headers))
  }
  def request[A](verb: Verb, payload: A): Promise[Response[A]] = {
    request(verb, payload, None, None)
  }

  def get[A](payload: A, subscriptionBinding: SubscriptionBinding): Promise[Response[A]] = request(Verb.GET, payload, Some(subscriptionBinding), None)
  def delete[A](payload: A, subscriptionBinding: SubscriptionBinding): Promise[Response[A]] = request(Verb.DELETE, payload, Some(subscriptionBinding), None)
  def put[A](payload: A, subscriptionBinding: SubscriptionBinding): Promise[Response[A]] = request(Verb.PUT, payload, Some(subscriptionBinding), None)
  def post[A](payload: A, subscriptionBinding: SubscriptionBinding): Promise[Response[A]] = request(Verb.POST, payload, Some(subscriptionBinding), None)

  def get[A](payload: A, headers: RequestHeaders): Promise[Response[A]] = request(Verb.GET, payload, None, Some(headers))
  def delete[A](payload: A, headers: RequestHeaders): Promise[Response[A]] = request(Verb.DELETE, payload, None, Some(headers))
  def put[A](payload: A, headers: RequestHeaders): Promise[Response[A]] = request(Verb.PUT, payload, None, Some(headers))
  def post[A](payload: A, headers: RequestHeaders): Promise[Response[A]] = request(Verb.POST, payload, None, Some(headers))

  def get[A](payload: A): Promise[Response[A]] = request(Verb.GET, payload, None, None)
  def delete[A](payload: A): Promise[Response[A]] = request(Verb.DELETE, payload, None, None)
  def put[A](payload: A): Promise[Response[A]] = request(Verb.PUT, payload, None, None)
  def post[A](payload: A): Promise[Response[A]] = request(Verb.POST, payload, None, None)
}
