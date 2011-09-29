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
package org.totalgrid.reef.sapi

import org.totalgrid.reef.japi.client.ServiceHeaders

object RequestEnv {
  val subQueueName = "SUB_QUEUE_NAME"
  val authToken = "AUTH_TOKEN"
  val user = "USER"
  val resultLimit = "RESULT_LIMIT"

  def apply() = new RequestEnv()
}

/**
 * This class wraps the headers we send/receive in the service envelope with helper
 * functions to make them look like a map. It is not intended that the user code directly
 * checks for the presence of specific headers, they should use helper classes like
 * ServiceHandlerHeaders to pull out the specific named values.
 */
class RequestEnv(var headers: Map[String, List[String]]) extends ServiceHeaders {

  def this() = this(Map.empty[String, List[String]])

  def asKeyValueList(): List[Tuple2[String, String]] = {
    headers.toList.map(k => k._2.map(new Tuple2(k._1, _))).flatten
  }

  def addHeader(key: String, value: String) {
    headers.get(key) match {
      case Some(a) => headers = (headers - key) + (key -> (value :: a))
      case None => headers = headers + (key -> List(value))
    }
  }

  def setHeader(key: String, value: String) {
    headers = (headers - key) + (key -> (List(value)))
  }

  def clearHeader(key: String) = {
    val removed = headers.get(key)
    headers -= key
    removed
  }

  def getString(key: String): Option[String] = {
    headers.get(key) match {
      case Some(List(a)) => Some(a)
      case _ => None
    }
  }
  def getInteger(key: String): Option[Int] = {
    headers.get(key) match {
      case Some(List(a)) =>
        Some(a.toInt)
      case _ => None
    }
  }
  def getList(key: String): List[String] = {
    headers.get(key) match {
      case Some(a) => a
      case _ => Nil
    }
  }

  def merge(defaults: RequestEnv) = {
    val k1 = Set(headers.keysIterator.toList: _*)
    val k2 = Set(defaults.headers.keysIterator.toList: _*)
    val intersection = k1 & k2

    val r1 = (for (key <- intersection) yield (key -> headers(key))).toMap
    val r2 = headers.filterKeys(!intersection.contains(_)) ++ defaults.headers.filterKeys(!intersection.contains(_))
    val r3 = new RequestEnv(r2 ++ r1)
    headers = r3.headers
  }

  def clear(): Unit = headers = Map.empty[String, List[String]]

  import org.totalgrid.reef.util.JavaInterop.notNull

  def subQueue: Option[String] = getString(RequestEnv.subQueueName)
  def authTokens: List[String] = getList(RequestEnv.authToken)

  def setSubscribeQueue(queueName: String) {
    addHeader(RequestEnv.subQueueName, notNull(queueName, "queueName"))
  }
  def addAuthToken(token: String) {
    addHeader(RequestEnv.authToken, notNull(token, "token"))
  }

  final override def setAuthToken(token: String) {
    setHeader(RequestEnv.authToken, notNull(token, "token"))
  }

  final override def clearAuthToken() = clearHeader(RequestEnv.authToken)

  def setAuthTokens(ss: List[String]) {
    clearAuthToken()
    ss.foreach(addHeader(RequestEnv.authToken, _))
  }

  def userName = getString(RequestEnv.user)
  def setUserName(s: String) = setHeader(RequestEnv.user, s)

  def getResultLimit() = getInteger(RequestEnv.resultLimit)
  def setResultLimit(limit: Int) = setHeader(RequestEnv.resultLimit, limit.toString)
  def clearResultLimit() = clearHeader(RequestEnv.resultLimit)
}

