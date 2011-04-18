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
package org.totalgrid.reef.api

/**
 * Base class for all exceptions thrown directly by services
 */
abstract class ReefServiceException(val msg: String) extends Exception(msg) {

  val status: Envelope.Status

  def getStatus = status
}

class UnknownServiceException(msg: String) extends ReefServiceException(msg) {
  val status = Envelope.Status.LOCAL_ERROR
}

class ServiceIOException(msg: String) extends ReefServiceException(msg) {
  val status = Envelope.Status.LOCAL_ERROR
}

class ResponseTimeoutException extends ReefServiceException("Response timed out") {
  val status = Envelope.Status.RESPONSE_TIMEOUT
}

abstract class ReplyException(msg: String) extends ReefServiceException(msg)

class ExpectationException(msg: String) extends ReplyException(msg) {
  val status = Envelope.Status.UNEXPECTED_RESPONSE
}

class BadRequestException(msg: String, val status: Envelope.Status = Envelope.Status.BAD_REQUEST) extends ReplyException(msg)

class UnauthorizedException(msg: String) extends BadRequestException(msg, Envelope.Status.UNAUTHORIZED)