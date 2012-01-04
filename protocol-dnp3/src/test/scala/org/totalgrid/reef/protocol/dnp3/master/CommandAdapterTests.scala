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
package org.totalgrid.reef.protocol.dnp3.master

import org.totalgrid.reef.client.service.proto.{ Mapping, Commands }
import scala.collection.mutable

import org.scalatest.Suite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.totalgrid.reef.protocol.dnp3.mock.MockCommandAcceptor
import org.totalgrid.reef.protocol.dnp3._
import org.totalgrid.reef.client.service.proto.Model.Command
import org.totalgrid.reef.protocol.api.Publisher

@RunWith(classOf[JUnitRunner])
class CommandAdapterTests extends Suite with ShouldMatchers {

  class MockResponseHandler extends Publisher[Commands.CommandStatus] {
    val responses = new mutable.Queue[Commands.CommandStatus]
    final override def publish(rsp: Commands.CommandStatus) = {
      responses += rsp
    }
  }

  def pop[A](responses: mutable.Queue[A])(f: A => Unit) = f(responses.dequeue)

  def testCommandAndResponse {
    val assoc = Mapping.CommandMap.newBuilder
    assoc.setIndex(5)
    assoc.setCommandName("testCommand")
    assoc.setType(Mapping.CommandType.PULSE)

    runATest(List(assoc)) { (adapt, acceptor, rspHandler) =>
      val request = Commands.CommandRequest.newBuilder
      request.setType(Commands.CommandRequest.ValType.NONE)
      request.setCommand(Command.newBuilder.setName("testCommand"))
      request.setCorrelationId("testCommandID")

      adapt.issue(request.build, rspHandler)

      acceptor.setpoints.size should equal(0)
      acceptor.commands.size should equal(1)
      pop(acceptor.commands) { cmd =>
        cmd._2 should equal(5)
        cmd._3 should equal(1)
      }

      val resp = new CommandResponse(CommandStatus.CS_SUCCESS)
      adapt.AcceptResponse(resp, 1)
      adapt.AcceptResponse(resp, 1) // Duplicate should be ignored

      rspHandler.responses.size should equal(1)
      pop(rspHandler.responses) { rsp =>
        rsp should equal(Commands.CommandStatus.SUCCESS)
      }

    }
  }

  def testSetpointAndResponse {
    val assoc = Mapping.CommandMap.newBuilder
    assoc.setIndex(7)
    assoc.setCommandName("testSetpoint")
    assoc.setType(Mapping.CommandType.SETPOINT)

    runATest(List(assoc)) { (adapt, acceptor, rspHandler) =>
      val request = Commands.CommandRequest.newBuilder
      request.setType(Commands.CommandRequest.ValType.INT)
      request.setCommand(Command.newBuilder.setName("testSetpoint"))
      request.setCorrelationId("testSetpointID")

      adapt.issue(request.build, rspHandler)

      acceptor.commands.size should equal(0)
      acceptor.setpoints.size should equal(1)

      pop(acceptor.setpoints) { cmd =>
        cmd._2 should equal(7)
        cmd._3 should equal(1)
      }

      val resp = new CommandResponse(CommandStatus.CS_SUCCESS)
      adapt.AcceptResponse(resp, 1)

      rspHandler.responses.size should equal(1)
      pop(rspHandler.responses) { rsp =>
        rsp should equal(Commands.CommandStatus.SUCCESS)
      }
    }
  }
  def testSequencing {
    val a = new mutable.ArrayBuffer[Mapping.CommandMap.Builder]
    a += Mapping.CommandMap.newBuilder.setIndex(2).setCommandName("testCommand1").setType(Mapping.CommandType.PULSE)
    a += Mapping.CommandMap.newBuilder.setIndex(3).setCommandName("testCommand2").setType(Mapping.CommandType.PULSE)

    runATest(a.toList) { (adapt, acceptor, rspHandler) =>
      var request = Commands.CommandRequest.newBuilder
      request.setType(Commands.CommandRequest.ValType.NONE)
      request.setCommand(Command.newBuilder.setName("testCommand1"))
      request.setCorrelationId("testCommandID1")
      adapt.issue(request.build, rspHandler)

      request = Commands.CommandRequest.newBuilder
      request.setType(Commands.CommandRequest.ValType.NONE)
      request.setCommand(Command.newBuilder.setName("testCommand2"))
      request.setCorrelationId("testCommandID2")
      adapt.issue(request.build, rspHandler)

      acceptor.setpoints.size should equal(0)
      acceptor.commands.size should equal(2)

      pop(acceptor.commands) { cmd =>
        cmd._2 should equal(2)
        cmd._3 should equal(1)
      }

      pop(acceptor.commands) { cmd =>
        cmd._2 should equal(3)
        cmd._3 should equal(2)
      }

      val resp2 = new CommandResponse(CommandStatus.CS_SUCCESS)
      adapt.AcceptResponse(resp2, 2)
      val resp1 = new CommandResponse(CommandStatus.CS_ALREADY_ACTIVE)
      adapt.AcceptResponse(resp1, 1)

      rspHandler.responses.size should equal(2)
      pop(rspHandler.responses) { rsp =>
        rsp should equal(Commands.CommandStatus.SUCCESS)
      }
      pop(rspHandler.responses) { rsp =>
        rsp should equal(Commands.CommandStatus.ALREADY_ACTIVE)
      }
    }
  }
  def testSequencingSameCommand {
    val a = new mutable.ArrayBuffer[Mapping.CommandMap.Builder]
    a += Mapping.CommandMap.newBuilder.setIndex(2).setCommandName("testCommand").setType(Mapping.CommandType.PULSE)

    runATest(a.toList) { (adapt, acceptor, rspHandler) =>
      var request = Commands.CommandRequest.newBuilder
      request.setType(Commands.CommandRequest.ValType.NONE)
      request.setCommand(Command.newBuilder.setName("testCommand"))
      request.setCorrelationId("testCommandID1")
      adapt.issue(request.build, rspHandler)
      request = Commands.CommandRequest.newBuilder
      request.setType(Commands.CommandRequest.ValType.NONE)
      request.setCommand(Command.newBuilder.setName("testCommand"))
      request.setCorrelationId("testCommandID2")
      adapt.issue(request.build, rspHandler)

      acceptor.setpoints.size should equal(0)
      acceptor.commands.size should equal(2)

      pop(acceptor.commands) { cmd =>
        cmd._2 should equal(2)
        cmd._3 should equal(1)
      }
      pop(acceptor.commands) { cmd =>
        cmd._2 should equal(2)
        cmd._3 should equal(2)
      }

      val resp2 = new CommandResponse(CommandStatus.CS_SUCCESS)
      adapt.AcceptResponse(resp2, 2)
      val resp1 = new CommandResponse(CommandStatus.CS_ALREADY_ACTIVE)
      adapt.AcceptResponse(resp1, 1)

      rspHandler.responses.size should equal(2)
      pop(rspHandler.responses) { rsp =>
        rsp should equal(Commands.CommandStatus.SUCCESS)
      }
      pop(rspHandler.responses) { rsp =>
        rsp should equal(Commands.CommandStatus.ALREADY_ACTIVE)
      }

    }
  }

  def runATest(mappings: List[Mapping.CommandMap.Builder])(testfun: (CommandAdapter, MockCommandAcceptor, MockResponseHandler) => Unit) = {
    val map = Mapping.IndexMapping.newBuilder
    map.setDeviceId("test")
    for (mapping <- mappings) map.addCommandmap(mapping)
    val acceptor = new MockCommandAcceptor
    testfun(new CommandAdapter(map.build, acceptor), acceptor, new MockResponseHandler)
  }
}
