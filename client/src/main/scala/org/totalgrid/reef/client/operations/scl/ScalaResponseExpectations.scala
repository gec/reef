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
package org.totalgrid.reef.client.operations.scl

import org.totalgrid.reef.client.operations.Response
import org.totalgrid.reef.client.exception.ExpectationException
import org.totalgrid.reef.client.proto.{ Envelope, StatusCodes }

object ScalaResponseExpectations {
  class RichResponse[A](resp: Response[A]) {
    import java.util.List

    def expectMany(num: Option[Int], expected: Option[Envelope.Status], errorFun: Option[(Int, Int) => String]): List[A] = {
      lazy val status = resp.getStatus
      lazy val list = resp.getList
      lazy val error = resp.getError

      expected match {
        case Some(stat) => {
          if (status != stat) {
            throw new ExpectationException("Status " + status + " != " + " expected " + stat)
          }
        }
        case None => resp.isSuccess match {
          case true =>
          case false => throw StatusCodes.toException(status, error)
        }
      }

      num.foreach { n =>
        val actual = list.size()
        if (actual != n) {
          throw new ExpectationException(errorFun.map(_(n, actual)) getOrElse (defaultError(n, actual)))
        }
      }

      list
    }

    final def expectOne(status: Envelope.Status): A = expectMany(Some(1), Some(status), None).get(0)

    final def expectOne(): A = expectMany(Some(1), None, None).get(0)

    final def expectOne(error: => String): A = expectMany(Some(1), None, combineErrors(error)).get(0)

    final def expectNone(status: Option[Envelope.Status]) { expectMany(Some(0), status, None) }

    final def expectNone(status: Envelope.Status) { expectNone(Some(status)) }

    final def expectNone() { expectNone(None) }

    final def expectMany(): List[A] = expectMany(None, None, None)

    final def expectMany(status: Envelope.Status): List[A] = expectMany(None, Some(status), None)

    final def expectMany(num: Int): List[A] = expectMany(Some(num), None, None)

    final def expectMany(num: Int, status: Envelope.Status): List[A] = expectMany(Some(num), Some(status), None)

    private def combineErrors(error: => String): Option[(Int, Int) => String] =
      Some((x: Int, y: Int) => error + " - " + defaultError(x, y))

    private def defaultError(expected: Int, actual: Int): String = {
      "Expected a result list of size " + expected + ", but got list of size: " + actual
    }
  }

  implicit def _scalaResponse[A](resp: Response[A]): RichResponse[A] = new RichResponse(resp)
}

