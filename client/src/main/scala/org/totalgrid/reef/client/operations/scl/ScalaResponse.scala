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

import org.totalgrid.reef.client.proto.StatusCodes
import org.totalgrid.reef.client.operations.Response
import org.totalgrid.reef.client.exception.ExpectationException
import org.totalgrid.reef.client.proto.Envelope.Status
import org.totalgrid.reef.client.javaimpl.ResponseWrapper

trait ScalaResponse {

  class RichResponse[A](resp: Response[A]) {
    //import java.util.List

    def many: List[A] = checkGood

    def one: A = {
      inspect {
        case (list, 0) => throw new ExpectationException("Expected a response list of size 1, but got an empty list")
        case (list, 1) => list.head //list.get(0)
        case (list, count) => throw new ExpectationException("Expected a response list of size 1, but got a list of size: " + count)
      }
    }
    def oneOrNone: Option[A] = {
      inspect {
        case (list, 0) => None
        case (list, 1) => Some(list.head) //Some(list.get(0))
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
        case true => resp.getList.toList // resp.getList
        case false => throw StatusCodes.toException(resp.getStatus, resp.getError)
      }
    }
  }

  implicit def _scalaResponse[A](resp: Response[A]): RichResponse[A] = new RichResponse(resp)
}

object ScalaResponse extends ScalaResponse {

  def success[A](status: Status, results: List[A]): Response[A] = {
    import scala.collection.JavaConversions._
    new ResponseWrapper[A](status, results.toList, "", true)
  }
  def failure[A](status: Status, error: String): Response[A] = {
    new ResponseWrapper[A](status, null, error, false)
  }

  import org.totalgrid.reef.client.sapi.client.{ FailureResponse, SuccessResponse }
  import scala.collection.JavaConversions._
  def convert[A](resp: Response[A]): org.totalgrid.reef.client.sapi.client.Response[A] = {
    resp.isSuccess match {
      case true => SuccessResponse(resp.getStatus, resp.getList.toList)
      case false => FailureResponse(resp.getStatus, resp.getError)
    }
  }
}

