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
 * This class wraps the headers we send/receive in the service envelope with helper
 * functions to make them look like a map. It is not intended that the user code directly
 * checks for the presence of specific headers, they should use helper classes like
 * ServiceHandlerHeaders to pull out the specific named values.
 */
class RequestEnv(var headers: Map[String, List[String]]) {

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
    new RequestEnv(r2 ++ r1)

  }

  def reset(): Unit = headers = Map.empty[String, List[String]]
}

/**
 * helper to get/set headers on a request
 */
class ServiceHandlerHeaders(val env: RequestEnv = new RequestEnv) {

  import org.totalgrid.reef.util.JavaInterop.notNull

  def subQueue = env.getString("SUB_QUEUE_NAME")
  def authTokens: List[String] = env.getList("AUTH_TOKEN")

  def setSubscribeQueue(queueName: String) {
    env.addHeader("SUB_QUEUE_NAME", notNull(queueName, "queueName"))
  }
  def addAuthToken(token: String) {
    env.addHeader("AUTH_TOKEN", notNull(token, "token"))
  }
  def setAuthToken(token: String) {
    env.setHeader("AUTH_TOKEN", notNull(token, "token"))
  }
  def setAuthTokens(ss: List[String]) {
    env.clearHeader("AUTH_TOKEN")
    ss.foreach(env.addHeader("AUTH_TOKEN", _))
  }

  /**
   * clear all headers
   */
  def reset(): Unit = env.reset
}

object ServiceHandlerHeaders {
  implicit def convertRequestEnvToServiceHeaders(e: RequestEnv) = new ServiceHandlerHeaders(e)
}