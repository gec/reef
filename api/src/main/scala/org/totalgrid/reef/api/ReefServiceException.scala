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
abstract class ReefServiceException(msg: String, cause: Throwable = null, status: Envelope.Status = null) extends Exception(msg, cause) {

  def getStatus = status

  def this(msg: String) = this(msg, null, null)
  def this(msg: String, cause: Throwable) = this(msg, cause, null)
  def this(msg: String, status: Envelope.Status) = this(msg, null, status)
}

class InternalClientError(msg: String, cause: Throwable) extends ReefServiceException(msg, cause, Envelope.Status.LOCAL_ERROR) {
}

class UnknownServiceException(msg: String) extends ReefServiceException(msg, Envelope.Status.LOCAL_ERROR) {
}

class ServiceIOException(msg: String) extends ReefServiceException(msg, Envelope.Status.LOCAL_ERROR) {
}

class ResponseTimeoutException(msg: String) extends ReefServiceException("Response timed out: " + msg, Envelope.Status.RESPONSE_TIMEOUT) {
}

abstract class ReplyException(msg: String, status: Envelope.Status) extends ReefServiceException(msg, status)

class ExpectationException(msg: String) extends ReplyException(msg, Envelope.Status.UNEXPECTED_RESPONSE) {
}

class BadRequestException(msg: String, val status: Envelope.Status = Envelope.Status.BAD_REQUEST) extends ReplyException(msg, status)

class UnauthorizedException(msg: String) extends BadRequestException(msg, Envelope.Status.UNAUTHORIZED)