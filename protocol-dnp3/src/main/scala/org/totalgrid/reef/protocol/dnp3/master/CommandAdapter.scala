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

import scala.collection.mutable

import org.totalgrid.reef.util.SafeExecution

import org.totalgrid.reef.client.service.proto.{ Mapping, Commands }

import org.totalgrid.reef.protocol.dnp3._
import com.typesafe.scalalogging.slf4j.Logging
import org.totalgrid.reef.protocol.api.{ Publisher, CommandHandler => ProtocolCommandHandler }

/**
 * Command adapter acts as a command response acceptor, forwarding translated responses to an actor
 *
 * @param cfg Measurement mapping configuration
 * @param cmd Command acceptor interface to to forward Commands
 * @param accept Function that processes Command responses
 */
class CommandAdapter(cfg: Mapping.IndexMapping, cmd: ICommandAcceptor)
    extends IResponseAcceptor with ProtocolCommandHandler with Logging with SafeExecution {

  case class ResponseInfo(id: String, publisher: Publisher[Commands.CommandStatus], obj: Object)

  private val map = MapGenerator.getCommandMap(cfg)
  private var sequence = 0
  private val idMap = mutable.Map.empty[Int, ResponseInfo]
  /// maps sequence numbers to id's

  override def AcceptResponse(rsp: CommandResponse, seq: Int) = safeExecute {
    idMap.get(seq) match {
      case Some(ResponseInfo(id, pub, obj)) =>
        logger.info("Got command response: " + rsp.getMResult.toString + " seq: " + seq)
        idMap -= seq //remove from the map
        pub.publish(DNPTranslator.translateResponseToStatus(rsp)) //send the response to the sender
      case None => logger.warn("Unknown command response with sequence " + seq)
    }
  }

  def issue(cr: Commands.CommandRequest, publisher: Publisher[Commands.CommandStatus]): Unit = safeExecute {
    map.get(cr.getCommand.getName) match {
      case Some(x) =>
        val index = x.getIndex
        if (x.getType == Mapping.CommandType.SETPOINT) {
          val st = DNPTranslator.translateSetpoint(cr)
          val cid = nextSeq(cr.getCorrelationId, publisher, st)

          logger.info("Sending setpoint request: " + x.toString + " index: " + index + " cid: " + cid)
          cmd.AcceptCommand(st, index, cid, this)
        } else {
          val bo = DNPTranslator.translateBinaryOutput(x)
          val cid = nextSeq(cr.getCorrelationId, publisher, bo)

          logger.info("Sending control request: " + x.toString + " index: " + index + " cid: " + cid)
          cmd.AcceptCommand(bo, index, cid, this)
        }

      case None => logger.warn("Unregisterd command request: " + cr.toString)
    }
  }

  /// obj is just held to stop garbage collector destroying reference
  private def nextSeq(id: String, publisher: Publisher[Commands.CommandStatus], obj: Object): Int = {
    sequence += 1
    idMap.put(sequence, ResponseInfo(id, publisher, obj))
    sequence
  }
}