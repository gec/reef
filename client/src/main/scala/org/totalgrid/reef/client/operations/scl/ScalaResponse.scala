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

import org.totalgrid.reef.client.operations.{ Response, DefaultResponse }
import org.totalgrid.reef.client.exception.ExpectationException
import org.totalgrid.reef.client.proto.Envelope.Status
import org.totalgrid.reef.client.proto.{ StatusCodes, Envelope }
import java.util.Arrays

trait ScalaResponse {

  class RichResponse[A](resp: Response[A]) {

    def many: List[A] = checkGood

    def one: A = {
      inspect {
        case (list, 0) => throw new ExpectationException("Expected a response list of size 1, but got an empty list")
        case (list, 1) => list.head
        case (list, count) => throw new ExpectationException("Expected a response list of size 1, but got a list of size: " + count)
      }
    }
    def oneOrNone: Option[A] = {
      inspect {
        case (list, 0) => None
        case (list, 1) => Some(list.head)
        case (list, count) => throw new ExpectationException("Expected a response list of size 1, but got a list of size: " + count)
      }
    }

    private def inspect[B](f: (List[A], Int) => B): B = {
      val list = checkGood
      f(list, list.size)
    }

    private def checkGood: List[A] = {
      import scala.collection.JavaConversions._
      resp.isSuccess match {
        case true => resp.getList.toList
        case false => throw resp.getException
      }
    }
  }

  implicit def _scalaResponse[A](resp: Response[A]): RichResponse[A] = new RichResponse(resp)
}

object ScalaResponse extends ScalaResponse {

  def success[A](status: Status, result: A): Response[A] = {
    new DefaultResponse[A](status, Arrays.asList(result))
  }
  def success[A](status: Status, results: List[A]): Response[A] = {
    import scala.collection.JavaConversions._
    new DefaultResponse[A](status, results.toList)
  }
  def failure[A](status: Status, error: String): Response[A] = {
    new DefaultResponse[A](status, error)
  }

  def wrap[A](status: Envelope.Status = Envelope.Status.INTERNAL_ERROR, list: List[A] = Nil, error: String = "") = {
    if (StatusCodes.isSuccess(status)) success[A](status, list)
    else failure[A](status, error)
  }

}

