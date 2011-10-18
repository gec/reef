package org.totalgrid.reef.api.sapi.client

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
import org.totalgrid.reef.api.japi.Envelope
import org.totalgrid.reef.api.japi.client.{ Routable, RequestHeaders }
import org.totalgrid.reef.api.sapi.{ AnyNodeDestination, AddressableDestination }
import org.totalgrid.reef.util.JavaInterop.notNull

object BasicRequestHeaders {
  val subQueueName = "SUB_QUEUE_NAME"
  val authToken = "AUTH_TOKEN"
  val user = "USER"
  val resultLimit = "RESULT_LIMIT"
  val destination = "DESTINATION"

  def from(list: List[Envelope.RequestHeader]): BasicRequestHeaders =
    list.foldLeft(BasicRequestHeaders.empty)((sum, i) => sum.addHeader(i.getKey, i.getValue))

  val empty = new BasicRequestHeaders
}

/**
 * This class wraps the headers we send/receive in the service envelope with helper
 * functions to make them look like a map. It is not intended that the user code directly
 * checks for the presence of specific headers, they should use helper classes like
 * ServiceHandlerHeaders to pull out the specific named values.
 */
final class BasicRequestHeaders private (val headers: Map[String, List[String]]) extends RequestHeaders {

  private def this() = this(Map.empty[String, List[String]])

  /* --- Implement RequestHeaders --- */

  override def setAuthToken(token: String) = setHeader(BasicRequestHeaders.authToken, notNull(token, "token"))

  override def clearAuthToken() = clearHeader(BasicRequestHeaders.authToken)

  override def setResultLimit(limit: Int) = setHeader(BasicRequestHeaders.resultLimit, limit.toString)

  override def clearResultLimit() = clearHeader(BasicRequestHeaders.resultLimit)

  /* --- Specific getters/setters not part of RequestHeaders --- */

  def setDestination(destination: Routable) = setHeader(BasicRequestHeaders.destination, destination.getKey)

  def clearDestination() = clearHeader(BasicRequestHeaders.destination)

  def getDestination: Routable =
    getString(BasicRequestHeaders.destination).map(key => AddressableDestination(key)).getOrElse(AnyNodeDestination)

  /* --- Helpers --- */

  def addHeader(key: String, value: String) = headers.get(key) match {
    case Some(list) => new BasicRequestHeaders((headers - key) + (key -> (value :: list)))
    case None => new BasicRequestHeaders(headers + (key -> List(value)))
  }

  def setHeader(key: String, value: String) = new BasicRequestHeaders(headers + (key -> List(value)))

  def clearHeader(key: String) = new BasicRequestHeaders(headers - key)

  def getString(key: String): Option[String] = headers.get(key) match {
    case Some(List(a)) => Some(a)
    case _ => None
  }

  def getInteger(key: String): Option[Int] = getString(key).map(_.toInt)

  def getList(key: String): List[String] = headers.get(key) match {
    case Some(x) => x
    case None => Nil
  }

  /**
   * Merge the headers together, if a key is already present in this headers, the value from the other header is discarded
   */
  def merge(rhs: BasicRequestHeaders) = new BasicRequestHeaders(headers.foldLeft(rhs.headers)((map, x) => map + x))

  def subQueue: Option[String] = getString(BasicRequestHeaders.subQueueName)

  def authTokens: List[String] = getList(BasicRequestHeaders.authToken)

  def setSubscribeQueue(queueName: String) = addHeader(BasicRequestHeaders.subQueueName, notNull(queueName, "queueName"))

  def addAuthToken(token: String) = addHeader(BasicRequestHeaders.authToken, notNull(token, "token"))

  def getResultLimit() = getInteger(BasicRequestHeaders.resultLimit)

  def setAuthTokens(ss: List[String]): BasicRequestHeaders =
    ss.foldLeft(clearAuthToken)((rh, token) => rh.addAuthToken(token))

  def userName = getString(BasicRequestHeaders.user)

  def setUserName(s: String) = setHeader(BasicRequestHeaders.user, s)

  def toEnvelopeRequestHeaders: Iterable[Envelope.RequestHeader] = {
    for {
      (key, list) <- headers
      value <- list
    } yield Envelope.RequestHeader.newBuilder.setKey(key).setValue(value).build
  }
}

