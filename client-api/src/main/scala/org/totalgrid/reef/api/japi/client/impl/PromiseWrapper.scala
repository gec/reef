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
package org.totalgrid.reef.api.japi.client.impl

import org.totalgrid.reef.api.sapi.client.{ Promise => ScalaPromise }

import org.totalgrid.reef.api.sapi.client.{ Response => ScalaResponse }
import org.totalgrid.reef.api.japi.client.{ Promise, ResponseListener, Response }

final class PromiseWrapper[A](promise: ScalaPromise[ScalaResponse[A]]) extends Promise[Response[A]] {

  override def await(): Response[A] = new ResponseWrapper(promise.await)

  override def addListener(listener: ResponseListener[Response[A]]): Unit = promise.listen { rsp =>
    listener.onCompletion(this)
  }

  override def isComplete = promise.isComplete
}
