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
package org.totalgrid.reef.client.sapi.client

import net.agileautomata.executor4s.TimeInterval
import org.totalgrid.reef.client.proto.{ StatusCodes, Envelope }

/* ---- Case classes that make the service api easier to use ---- */

object Response {

  def apply[A](status: Envelope.Status = Envelope.Status.INTERNAL_ERROR, list: List[A] = Nil, error: String = ""): Response[A] = {
    if (StatusCodes.isSuccess(status)) SuccessResponse(status, list)
    else FailureResponse(status, error)
  }

  def apply[A](status: Envelope.Status, single: A): Response[A] = apply(status, single :: Nil, "")

  def apply[A](status: Envelope.Status, list: List[A]): Response[A] = apply(status, list, "")

}

sealed trait Response[+A] {

  def status: Envelope.Status
  def list: List[A]
  def error: String
  def success: Boolean

}

sealed case class SuccessResponse[A](status: Envelope.Status = Envelope.Status.OK, list: List[A])
    extends Response[A] {

  override val error = ""
  override val success = true
}

sealed case class FailureResponse(status: Envelope.Status = Envelope.Status.INTERNAL_ERROR, error: String = "")
    extends Response[Nothing] {

  override val list = Nil
  override val success = false
  override def toString = "Request failed with status: " + status + ", msg: " + error
}

sealed case class ResponseTimeout(interval: TimeInterval) extends FailureResponse(Envelope.Status.RESPONSE_TIMEOUT,
  "Timed out waiting for response after: " + interval)

