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
package org.totalgrid.reef.client.javaimpl

import org.totalgrid.reef.client.operations.Response
import java.util.List
import scala.collection.immutable.{ List => SList }
import scala.collection.JavaConversions._
import org.totalgrid.reef.client.proto.Envelope.Status
import org.totalgrid.reef.client.sapi.client.{ Response => SResponse }
import org.totalgrid.reef.client.exception.ReefServiceException
import org.totalgrid.reef.client.proto.StatusCodes

object ResponseWrapper {

  def success[A](status: Status, results: List[A]): Response[A] = {
    new ResponseWrapper(status, results, "", true)
  }
  def success[A](status: Status, results: SList[A]): Response[A] = {
    new ResponseWrapper(status, results, "", true)
  }
  def failure[A](status: Status, error: String): Response[A] = {
    new ResponseWrapper[A](status, null, error, false)
  }

  def convert[A](resp: SResponse[A]): Response[A] = {
    import scala.collection.JavaConversions._
    new ResponseWrapper(resp.status, resp.list, resp.error, resp.success)
  }
}

class ResponseWrapper[A](status: Status, results: List[A], error: String, success: Boolean) extends Response[A] {

  def getStatus: Status = status

  def getList: List[A] = results

  def getErrorMessage: String = error

  def isSuccess: Boolean = success

  lazy val getException: ReefServiceException = {
    if (isSuccess) throw new IllegalArgumentException("Response was successful, no exception available")
    StatusCodes.toException(status, error)
  }
}
