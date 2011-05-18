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
package org.totalgrid.reef.messaging.javaclient

import org.totalgrid.reef.api._
import org.totalgrid.reef.api.javaclient.{ IPromise, IResponse, ISubscription, ISession, IEventAcceptor }
import scalaclient.{ ClientSession, IPromise => IScalaPromise, Response => ScalaResponse }

/**
 * wraps a ProtoClient with some java helpers to convert to and from java lists
 */
class Session(val client: ClientSession) extends ISession {

  private implicit def convert[A](promise: IScalaPromise[ScalaResponse[A]]): IPromise[IResponse[A]] = new Promise[A](promise)

  final override def get[A](request: A): IPromise[IResponse[A]] = client.get(request)
  final override def delete[A](request: A): IPromise[IResponse[A]] = client.delete(request)
  final override def post[A](request: A): IPromise[IResponse[A]] = client.post(request)
  final override def put[A](request: A): IPromise[IResponse[A]] = client.put(request)

  private implicit def convert[A](sub: ISubscription[A]): RequestEnv = {
    val headers = new ServiceHandlerHeaders(new RequestEnv)
    headers.setSubscribeQueue(sub.getId)
    headers.env
  }

  final override def get[A](request: A, hdr: ISubscription[A]): IPromise[IResponse[A]] = client.get(request, hdr)
  final override def delete[A](request: A, hdr: ISubscription[A]): IPromise[IResponse[A]] = client.delete(request, hdr)
  final override def put[A](request: A, hdr: ISubscription[A]): IPromise[IResponse[A]] = client.put(request, hdr)
  final override def post[A](request: A, hdr: ISubscription[A]): IPromise[IResponse[A]] = client.post(request, hdr)

  final override def addSubscription[A](pd: ITypeDescriptor[A]): ISubscription[A] = {
    val sub = client.addSubscription[A](pd.getKlass)
    new SubscriptionWrapper(sub)
  }

  final override def addSubscription[A](pd: ITypeDescriptor[A], ea: IEventAcceptor[A]): ISubscription[A] = {
    val sub = client.addSubscription[A](pd.getKlass)
    val wrapped = new SubscriptionWrapper(sub)
    wrapped.start(ea)
    wrapped
  }

  final override def getDefaultEnv = new ServiceHandlerHeaders(client.getDefaultHeaders)

  final override def close() = client.close()
}

