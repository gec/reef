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

import org.totalgrid.reef.api._

trait MultiResult[+A]

trait SingleResult[+A]

case class SingleSuccess[A](status: Envelope.Status, result: A) extends SingleResult[A]

case class MultiSuccess[A](status: Envelope.Status, result: List[A]) extends MultiResult[A]

case class Failure(status: Envelope.Status, error: String = "") extends Throwable with SingleResult[Nothing] with MultiResult[Nothing] {
  override def toString: String = super.toString + " " + status + " message: " + error

  def toException: ReefServiceException = status match {
    case Envelope.Status.RESPONSE_TIMEOUT => new ResponseTimeoutException
    case Envelope.Status.UNAUTHORIZED => new UnauthorizedException(error)
    case Envelope.Status.UNEXPECTED_RESPONSE => new ExpectationException(error)
    case _ => new BadRequestException(error, status)
  }
}