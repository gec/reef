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
package org.totalgrid.reef.sapi.client

import org.totalgrid.reef.japi._
import org.totalgrid.reef.sapi._
import org.totalgrid.reef.japi.client._

/* ---- Case classes that make the service api easier to use ---- */

case class Request[+A](verb: Envelope.Verb, payload: A, env: BasicRequestHeaders = BasicRequestHeaders.empty)

object Response {

  def convert[A](option: Option[Response[A]]): Response[A] = option match {
    case Some(x) => x
    case None => ResponseTimeout
  }

  def apply[A](status: Envelope.Status = Envelope.Status.INTERNAL_ERROR, list: List[A] = Nil, error: String = ""): Response[A] = {
    if (StatusCodes.isSuccess(status)) Success(status, list)
    else Failure(status, error)
  }

  def apply[A](status: Envelope.Status, single: A): Response[A] = apply(status, single :: Nil, "")
}

sealed trait Response[+A] extends Expectations[A] {

  def status: Envelope.Status
  def list: List[A]
  def error: String
  def success: Boolean

  final override def expectMany(num: Option[Int], expected: Option[Envelope.Status], errorFun: Option[(Int, Int) => String]): List[A] = {

    expected match {
      case Some(x) =>
        if (status != x)
          throw new ExpectationException("Status " + status + " != " + " expected " + x)
        list
      case None => this match {
        case Success(_, list) => list
        case Failure(status, error) => throw StatusCodes.toException(status, error)
      }
    }

    num.foreach { expected =>
      val actual = list.size
      if (expected != actual) {
        val msg = errorFun match {
          case Some(fun) => fun(expected, actual)
          case None => defaultError(expected, actual)
        }
        throw new ExpectationException(msg)
      }
    }

    list
  }

}

final case class Success[A](status: Envelope.Status = Envelope.Status.OK, list: List[A])
    extends Response[A] {

  override val error = ""
  override val success = true
  override def many = Right(list)
}

sealed case class Failure(status: Envelope.Status = Envelope.Status.INTERNAL_ERROR, error: String = "")
    extends Response[Nothing] {

  override val list = Nil
  override val success = false
  override def many = Left(new ReefServiceException(error, status))
  override def toString = "Request failed with status: " + status + ", msg: " + error
}

object ResponseTimeout extends Failure(Envelope.Status.RESPONSE_TIMEOUT)

case class Event[A](event: Envelope.Event, value: A) extends SubscriptionEvent[A] {

  final override def getEventType() = event
  final override def getValue() = value
}

