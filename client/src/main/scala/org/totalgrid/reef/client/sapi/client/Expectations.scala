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

import org.totalgrid.reef.client.exception.ExpectationException
import org.totalgrid.reef.client.proto.{ StatusCodes, Envelope }

trait Expectations {

  class ResponseWithExpectation[A](resp: Response[A]) extends ResponseExpectations[A] {
    def expectMany(num: Option[Int], expected: Option[Envelope.Status], errorFun: Option[(Int, Int) => String]): List[A] = {

      expected match {
        case Some(x) =>
          if (resp.status != x)
            throw new ExpectationException("Status " + resp.status + " != " + " expected " + x)
          resp.list
        case None => resp match {
          case SuccessResponse(_, list) => list
          case FailureResponse(status, error) => throw StatusCodes.toException(status, error)
        }
      }

      num.foreach { expected =>
        val actual = resp.list.size
        if (expected != actual) {
          val msg = errorFun match {
            case Some(fun) => fun(expected, actual)
            case None => defaultError(expected, actual)
          }
          throw new ExpectationException(msg)
        }
      }

      resp.list
    }
  }

  implicit def toPimpedExpectation[A](resp: Response[A]) = new ResponseWithExpectation[A](resp)
}

object Expectations extends Expectations

trait ResponseExpectations[+A] {

  // implement to widen the trait
  def expectMany(num: Option[Int], expected: Option[Envelope.Status], errorFun: Option[(Int, Int) => String]): List[A]

  // widened helpers

  final def expectOneOrNone(): Option[A] = {
    val list = expectMany()
    list match {
      case List(x) => Some(x)
      case Nil => None
      case _ =>
        throw new ExpectationException("Expected a result list of size 0,1 but got a list of size: " + list.size)
    }
  }

  final def expectOne(status: Envelope.Status): A = expectMany(Some(1), Some(status), None).head

  final def expectOne(): A = expectMany(Some(1), None, None).head

  final def expectOne(error: => String): A = expectMany(Some(1), None, combineErrors(error)).head

  final def expectNone(status: Option[Envelope.Status]): Unit = expectMany(Some(0), status, None)

  final def expectNone(status: Envelope.Status): Unit = expectNone(Some(status))

  final def expectNone(): Unit = expectNone(None)

  final def expectMany(): List[A] = expectMany(None, None, None)

  final def expectMany(status: Envelope.Status): List[A] = expectMany(None, Some(status), None)

  final def expectMany(num: Int): List[A] = expectMany(Some(num), None, None)

  final def expectMany(num: Int, status: Envelope.Status): List[A] = expectMany(Some(num), Some(status), None)

  private def combineErrors(error: => String): Option[(Int, Int) => String] =
    Some((x: Int, y: Int) => error + " - " + defaultError(x, y))

  protected def defaultError(expected: Int, actual: Int): String =
    "Expected a result list of size " + expected + ", but got list of size: " + actual
}