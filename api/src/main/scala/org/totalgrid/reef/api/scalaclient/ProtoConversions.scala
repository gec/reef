package org.totalgrid.reef.api.scalaclient

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
import org.totalgrid.reef.api.{ Envelope, StatusCodes }

/**
 *
 */
object ProtoConversions {

  implicit def convertResponseToResult[A](response: Option[Response[A]]): MultiResult[A] = response match {
    case Some(Response(status, list, msg)) =>
      if (StatusCodes.isSuccess(status)) MultiSuccess(status, list)
      else Failure(status, msg)
    case None => Failure(Envelope.Status.RESPONSE_TIMEOUT, error = "Service response timeout")
  }

  implicit def convertMultiResultToSingle[A](response: MultiResult[A]): SingleResult[A] = response match {
    case MultiSuccess(status, List(x)) => SingleSuccess(status, x)
    case MultiSuccess(status, list) =>
      Failure(Envelope.Status.UNEXPECTED_RESPONSE, error = "Expected one result, but got: " + list.size)
    case x: Failure => x
  }

  implicit def convertMultiListToSingleList[A](list: List[MultiResult[A]]): List[SingleResult[A]] = list.map { x => x }

  implicit def convert[A](callback: List[SingleResult[A]] => Unit): List[MultiResult[A]] => Unit = { a: List[MultiResult[A]] =>
    callback(a)
  }

  implicit def convertResultToListOrThrow[A](response: MultiResult[A]): List[A] = response match {
    case MultiSuccess(status, list) => list
    case x: Failure => throw x.toException
  }

  implicit def convertResultToTypeOrThrow[A](response: SingleResult[A]): A = response match {
    case SingleSuccess(status, x) => x
    case x: Failure => throw x.toException
  }

  implicit def convertSingleCallbackToMulti[A](callback: SingleResult[A] => Unit): (MultiResult[A]) => Unit = { r: MultiResult[A] =>
    callback(r)
  }

}