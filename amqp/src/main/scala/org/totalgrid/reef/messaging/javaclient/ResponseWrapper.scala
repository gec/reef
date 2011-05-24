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

import org.totalgrid.reef.japi.client.Response
import org.totalgrid.reef.sapi.client.{ Response => ScalaResponse }

import scala.collection.JavaConversions._

class ResponseWrapper[A](rsp: ScalaResponse[A]) extends Response[A] {

  final override def isSuccess = rsp.success

  final override def expectOne(): A = rsp.expectOne()

  final override def expectMany(): java.util.List[A] = rsp.expectMany()

}