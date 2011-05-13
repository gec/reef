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
package org.totalgrid.reef.messaging.javaclient

import org.totalgrid.reef.api.ExpectationException
import org.totalgrid.reef.api.javaclient.IResult
import org.totalgrid.reef.api.scalaclient.{ MultiResult, MultiSuccess, Failure }
import scala.collection.JavaConversions._

class Result[A](result: MultiResult[A]) extends IResult[A] {

  def isSuccess = result match {
    case MultiSuccess(status, x) => true
    case _ => false
  }

  final override def expectOne(): A = expectSuccess match {
    case List(x) => x
    case x: List[A] =>
      throw new ExpectationException("Expected a single result, but got list of size: " + x.size)
  }

  final override def expectMany() = expectSuccess

  private def expectSuccess: List[A] = result match {
    case MultiSuccess(_, x) => x
    case Failure(status, msg) =>
      throw new ExpectationException("Expected a successfully response, but got failure with status: " + status)
  }

  def getResult: java.util.List[A] = result match {
    case MultiSuccess(status, x) => x
    case x: Failure => throw x.toException
  }

  def getFailure: Failure = result match {
    case x: Failure => x
    case _ => throw new Exception("Success cannot be interpreted as failure")
  }

}