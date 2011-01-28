/**
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
//import org.totalgrid.reef.protocol.dnp3.{ ICommandAcceptor, IResponseAcceptor, CommandResponse }
import org.totalgrid.reef.proto.{ Mapping, Commands }

/** Command adapter acts as a command response acceptor, forwarding translated responses to an actor
 * 
 * @param cfg Measurement mapping configuration
 * @param cmd Command acceptor interface to to forward Commands
 * @param accept Function that processes Command responses
 */
class CommandAdapter(cfg: Mapping.IndexMapping, cmd: ICommandAcceptor, accept: Commands.CommandResponse => Unit)
    extends IResponseAcceptor with Logging {

  private val map = MapGenerator.getCommandMap(cfg)
  private var sequence = 0
  private val idMap = mutable.Map.empty[Int, String] /// maps sequence numbers to id's

  override def AcceptResponse(rsp: CommandResponse, seq: Int) = {
    idMap.get(seq) match {
      case Some(id) => {
        info { "Got command response: " + rsp.toString + " seq: " + seq }
        idMap -= seq //remove from the map
        accept(DNPTranslator.translate(rsp, id)) //send the response to the sender
      }
      case None => warn { "Unknown command response with sequence " + seq }
    }
  }

  def send(r: Commands.CommandRequest): Unit = {

    map.get(r.getName) match {
      case Some(x) => {
        info { "Sending command request: " + x.toString }
        if (x.getType == Mapping.CommandType.SETPOINT)
          cmd.AcceptCommand(DNPTranslator.translateSetpoint(r), x.getIndex, nextSeq(r.getCorrelationId), this)
        else
          cmd.AcceptCommand(DNPTranslator.translateBinaryOutput(x), x.getIndex, nextSeq(r.getCorrelationId), this)
      }
      case None => //warn { "Unregisterd command request: " + r.toString } 
    }
  }

  private def nextSeq(id: String): Int = {
    sequence += 1
    idMap.put(sequence, id)
    sequence
  }
}