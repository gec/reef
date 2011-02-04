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
package org.totalgrid.reef.protoapi

import org.totalgrid.reef.proto.Envelope

/** Contains types/case classes used in the proto service interfaces
 */
object ProtoServiceTypes {

  /* ---- Case classes that make the service protoapi easier to use ---- */

  case class Request[T](verb: Envelope.Verb, payload: T, env: RequestEnv)

  case class Response[T](status: Envelope.Status, error: String, result: List[T]) {
    def this(status: Envelope.Status, result: List[T]) = this(status, "", result)
    def this(status: Envelope.Status, result: T) = this(status, "", List(result))
  }

  case class Event[T](event: Envelope.Event, result: T) {
    // accessors for java client
    def getEvent() = event
    def getResult() = result
  }

  /* ---- Further decomposition that makes matching results even easier to use ---- */

  trait MultiResult[+T]
  trait SingleResult[+T]

  case class Failure(status: Envelope.Status, error: String) extends Throwable with SingleResult[Nothing] with MultiResult[Nothing] {

    override def toString: String = {
      super.toString + " " + status + " message: " + error
    }
    // accessors for java client
    def getStatus() = status
    def getError() = error

    def toException: ProtoServiceException =
      new ProtoServiceException(error, status)
  }

  case class SingleResponse[T](result: T) extends SingleResult[T]
  case class MultiResponse[T](result: List[T]) extends MultiResult[T]

}