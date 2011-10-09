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
package org.totalgrid.reef.messaging.javaclient

import org.totalgrid.reef.japi.client._
import org.totalgrid.reef.sapi.BasicRequestHeaders
import org.totalgrid.reef.promise.{ Promise => ScalaPromise }
import org.totalgrid.reef.japi.TypeDescriptor
import org.totalgrid.reef.sapi.client.{ ClientSession, Response => ScalaResponse }

/**
 * wraps a ProtoClient with some java helpers to convert to and from java lists
 */
final class SessionWrapper(val client: ClientSession) extends Session {

  private implicit def convertPromise[A](promise: ScalaPromise[ScalaResponse[A]]): Promise[Response[A]] = new PromiseWrapper[A](promise)

  override def get[A](request: A): Promise[Response[A]] = client.get(request)
  override def delete[A](request: A): Promise[Response[A]] = client.delete(request)
  override def post[A](request: A): Promise[Response[A]] = client.post(request)
  override def put[A](request: A): Promise[Response[A]] = client.put(request)

  private implicit def convertSubscription[A](sub: Subscription[A]): BasicRequestHeaders =
    BasicRequestHeaders.empty.setSubscribeQueue(sub.getId)

  override def get[A](request: A, hdr: Subscription[A]): Promise[Response[A]] = client.get(request, hdr)
  override def delete[A](request: A, hdr: Subscription[A]): Promise[Response[A]] = client.delete(request, hdr)
  override def put[A](request: A, hdr: Subscription[A]): Promise[Response[A]] = client.put(request, hdr)
  override def post[A](request: A, hdr: Subscription[A]): Promise[Response[A]] = client.post(request, hdr)

  override def addSubscription[A](pd: TypeDescriptor[A]): Subscription[A] = {
    val sub = client.addSubscription[A](pd.getKlass)
    new SubscriptionWrapper(sub)
  }

  override def addSubscription[A](pd: TypeDescriptor[A], ea: SubscriptionEventAcceptor[A]): Subscription[A] = {
    val sub = client.addSubscription[A](pd.getKlass)
    val wrapped = new SubscriptionWrapper(sub)
    wrapped.start(ea)
    wrapped
  }

  override def getHeaders = client.getHeaders

  //TODO - the cast here is a smell
  override def setHeaders(headers: RequestHeaders) = client.setHeaders(headers.asInstanceOf[BasicRequestHeaders])

  override def close() = client.close()
}

