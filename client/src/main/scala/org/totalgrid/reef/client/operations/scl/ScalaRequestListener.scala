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
package org.totalgrid.reef.client.operations.scl

import org.totalgrid.reef.client.operations.{ RequestListener, RequestListenerManager }

object ScalaRequestListener {

  /**
   * Attaches a listener, runs a block of code and then makes sure it is removed
   */
  def withRequestListener[A, B <: RequestListenerManager](client: B, listener: RequestListener)(func: => A): A =
    withRequestListener(client, Some(listener))(func)

  /**
   * Optionally attaches a listener, runs code then removes spy afterwards. Makes it simpler to
   * have single code path in client code that may or may not actually want to attach spy
   */
  def withRequestListener[A, B <: RequestListenerManager](client: B, listener: Option[RequestListener])(func: => A): A = {
    try {
      listener.foreach { client.addRequestListener(_) }

      func

    } finally {
      listener.foreach { client.removeRequestListener(_) }
    }
  }
}
