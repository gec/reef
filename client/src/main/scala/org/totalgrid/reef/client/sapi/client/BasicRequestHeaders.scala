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

import org.totalgrid.reef.client.proto.Envelope
import org.totalgrid.reef.util.JavaInterop._
import org.totalgrid.reef.client._

object BasicRequestHeaders {
  val subQueueName = "SUB_QUEUE_NAME"
  val authToken = "AUTH_TOKEN"
  val resultLimit = "RESULT_LIMIT"
  val destination = "DESTINATION"
  val timeoutMs = "TIMEOUT_MS"

  def from(map: java.util.Map[String, java.util.List[String]]): RequestHeaders = {
    import scala.collection.JavaConversions._
    map.foldLeft(new BasicRequestHeaders)((sum, i) => sum.addHeader(i._1, i._2.head))
  }

  def fromAuth(authToken: String) = empty.setAuthToken(authToken)

  val empty: RequestHeaders = new BasicRequestHeaders
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

  override def setResultLimit(limit: Int) = {
    if (limit <= 0) throw new IllegalArgumentException("Result Limit must be greator than 0 not " + limit)
    setHeader(BasicRequestHeaders.resultLimit, limit.toString)
  }
  override def clearResultLimit = clearHeader(BasicRequestHeaders.resultLimit)
  override def hasResultLimit = hasHeader(BasicRequestHeaders.resultLimit)
  override def getResultLimit = getInteger(BasicRequestHeaders.resultLimit).map { _.toInt }.getOrElse(-1)

  override def setTimeout(timeoutMillis: Long) = {
    if (timeoutMillis <= 0) throw new IllegalArgumentException("Timeout must be greator than 0 not " + timeoutMillis)
    setHeader(BasicRequestHeaders.timeoutMs, timeoutMillis.toString)
  }
  override def clearTimeout = clearHeader(BasicRequestHeaders.timeoutMs)
  override def hasTimeout = hasHeader(BasicRequestHeaders.timeoutMs)
  override def getTimeout = getString(BasicRequestHeaders.timeoutMs).map { _.toLong }.getOrElse(-1)

  override def setDestination(key: Routable) = setHeader(BasicRequestHeaders.destination, notNull(key.getKey, "key"))
  override def clearDestination = clearHeader(BasicRequestHeaders.destination)
  override def hasDestination = hasHeader(BasicRequestHeaders.destination)
  override def getDestination = getString(BasicRequestHeaders.destination).map(key => new AddressableDestination(key)).getOrElse(new AnyNodeDestination())

  override def getAuthToken = getString(BasicRequestHeaders.authToken).getOrElse("")
  override def hasAuthToken = hasHeader(BasicRequestHeaders.authToken)
  override def setAuthToken(token: String) = setHeader(BasicRequestHeaders.authToken, notNull(token, "token"))
  override def clearAuthToken() = clearHeader(BasicRequestHeaders.authToken)

  override def setSubscribeQueue(queueName: String) = setHeader(BasicRequestHeaders.subQueueName, notNull(queueName, "queueName"))
  override def clearSubscribeQueue() = clearHeader(BasicRequestHeaders.subQueueName)
  override def hasSubscribeQueue = hasHeader(BasicRequestHeaders.subQueueName)
  override def getSubscribeQueue = getString(BasicRequestHeaders.subQueueName).getOrElse("")

  /* --- Helpers --- */

  private def addHeader(key: String, value: String) = headers.get(key) match {
    case Some(list) => new BasicRequestHeaders((headers - key) + (key -> (value :: list)))
    case None => new BasicRequestHeaders(headers + (key -> List(value)))
  }

  private def setHeader(key: String, value: String) = new BasicRequestHeaders(headers + (key -> List(value)))

  private def clearHeader(key: String) = new BasicRequestHeaders(headers - key)
  private def hasHeader(key: String) = headers.get(key).map { b => true }.getOrElse(false)

  private def getString(key: String): Option[String] = headers.get(key) match {
    case Some(List(a)) => Some(a)
    case _ => None
  }

  private def getInteger(key: String): Option[Int] = getString(key).map(_.toInt)

  private def getList(key: String): List[String] = headers.get(key) match {
    case Some(x) => x
    case None => Nil
  }

  def merge(other: RequestHeaders) = {
    var out = this
    out = if (other.hasDestination) out.setDestination(other.getDestination) else out
    out = if (other.hasAuthToken) out.setAuthToken(other.getAuthToken) else out
    out = if (other.hasResultLimit) out.setResultLimit(other.getResultLimit) else out
    out = if (other.hasSubscribeQueue) out.setSubscribeQueue(other.getSubscribeQueue) else out
    out = if (other.hasTimeout) out.setTimeout(other.getTimeout) else out
    out
  }

  def toEnvelopeRequestHeaders: java.lang.Iterable[Envelope.RequestHeader] = {
    val slist = for {
      (key, list) <- headers
      value <- list
    } yield Envelope.RequestHeader.newBuilder.setKey(key).setValue(value).build

    import scala.collection.JavaConversions._

    slist.toList
  }
}

