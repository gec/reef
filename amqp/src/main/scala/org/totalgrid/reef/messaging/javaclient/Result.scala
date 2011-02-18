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

import org.totalgrid.reef.api.javaclient.IResult
import org.totalgrid.reef.api.ServiceTypes._
import scala.collection.JavaConversions._

class Result[A](result: MultiResult[A]) extends IResult[A] {

  def isSuccess = result match {
    case MultiSuccess(x) => true
    case _ => false
  }

  def getResult: java.util.List[A] = result match {
    case MultiSuccess(x) => x
    case x: Failure => throw x.toException
  }

  def getFailure: Failure = result match {
    case x: Failure => x
    case _ => throw new Exception("Success cannot be interpreted as failure")
  }

}