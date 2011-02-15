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
package org.totalgrid.reef.api.request

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import scala.collection.JavaConversions._
import org.scalatest.FunSuite
import org.totalgrid.reef.proto.Commands.{ CommandStatus, CommandRequest, UserCommandRequest }

@RunWith(classOf[JUnitRunner])
class DocumenterTest extends FunSuite with ShouldMatchers {

  test("Doctest") {
    //val req = Commands.allowAccessForCommand("StaticSubstation.Breaker02.Trip")

    val req = UserCommandRequest.newBuilder
      .setCommandRequest(
        CommandRequest.newBuilder.setName("StaticSubstation.Breaker02.Trip"))
      .build

    val resp = UserCommandRequest.newBuilder
      .setUid("502")
      .setStatus(CommandStatus.EXECUTING)
      .setUser("core")
      .setCommandRequest(CommandRequest.newBuilder.setName("StaticSubstation.Breaker02.Trip"))
      .setTimeoutMs(502394324)
      .build

    println(Documenter.messageToXml(req))
    println(Documenter.messageToXml(resp))

    /*val doc = new Documenter("doctest.xml", "Test Doc", "DOCUMENTATION PARAGRAPH")
    doc.addCase("User command request", "Blah blah blah blah", req, resp)
    doc.addCase("Second thing that happens", "Blah blah blah blah", req, resp)
    doc.save*/
  }
}