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

import org.totalgrid.reef.client.{ Routable, RequestHeaders }

trait ScalaRequestHeaders {

  class PimpedRequestHeaders(headers: RequestHeaders) {
    def subQueue: Option[String] = if (headers.hasSubscribeQueue) Some(headers.getSubscribeQueue) else None
    def timeout: Option[Long] = if (headers.hasTimeout) Some(headers.getTimeout) else None
    def resultLimit: Option[Int] = if (headers.hasResultLimit) Some(headers.getResultLimit) else None
    def destination: Option[Routable] = if (headers.hasDestination) Some(headers.getDestination) else None
  }

  implicit def toPimpedRequestHeaders(headers: RequestHeaders) = new PimpedRequestHeaders(headers)
}

object ScalaRequestHeaders extends ScalaRequestHeaders
