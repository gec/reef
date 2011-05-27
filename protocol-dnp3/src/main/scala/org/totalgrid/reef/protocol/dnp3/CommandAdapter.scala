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
package org.totalgrid.reef.protocol.dnp3

import scala.collection.mutable

import org.totalgrid.reef.util.Logging

import org.totalgrid.reef.proto.{ Mapping, Commands }

import org.totalgrid.reef.protocol.api.{ ICommandHandler => IProtocolCommandHandler, IListener }

/**
 * Command adapter acts as a command response acceptor, forwarding translated responses to an actor
 *
 * @param cfg Measurement mapping configuration
 * @param cmd Command acceptor interface to to forward Commands
 * @param accept Function that processes Command responses
 */
class CommandAdapter(cfg: Mapping.IndexMapping, cmd: ICommandAcceptor)
    extends IResponseAcceptor with IProtocolCommandHandler with Logging {

  case class ResponseInfo(id: String, handler: IListener[Commands.CommandResponse])

  private val map = MapGenerator.getCommandMap(cfg)
  private var sequence = 0
  private val idMap = mutable.Map.empty[Int, ResponseInfo]
  /// maps sequence numbers to id's

  override def AcceptResponse(rsp: CommandResponse, seq: Int) = idMap.get(seq) match {
    case Some(ResponseInfo(id, handler)) =>
      logger.info("Got command response: " + rsp.toString + " seq: " + seq)
      idMap -= seq //remove from the map
      handler.onUpdate(DNPTranslator.translate(rsp, id)) //send the response to the sender
    case None => logger.warn("Unknown command response with sequence " + seq)
  }

  def issue(cr: Commands.CommandRequest, rspHandler: IListener[Commands.CommandResponse]): Unit = map.get(cr.getName) match {
    case Some(x) =>
      logger.info("Sending command request: " + x.toString)
      if (x.getType == Mapping.CommandType.SETPOINT)
        cmd.AcceptCommand(DNPTranslator.translateSetpoint(cr), x.getIndex, nextSeq(cr.getCorrelationId, rspHandler), this)
      else
        cmd.AcceptCommand(DNPTranslator.translateBinaryOutput(x), x.getIndex, nextSeq(cr.getCorrelationId, rspHandler), this)

    case None => logger.warn("Unregisterd command request: " + cr.toString)
  }

  private def nextSeq(id: String, handler: IListener[Commands.CommandResponse]): Int = {
    sequence += 1
    idMap.put(sequence, ResponseInfo(id, handler))
    sequence
  }
}