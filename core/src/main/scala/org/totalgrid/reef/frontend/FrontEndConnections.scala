/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Green Energy Corp licenses this file
 * to you under the GNU Affero General Public License Version 3.0
 * (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.frontend

import org.totalgrid.reef.proto.Envelope
import org.totalgrid.reef.proto.{ Commands, Measurements }
import org.totalgrid.reef.proto.FEP.{ CommunicationEndpointConfig => ConfigProto, CommunicationEndpointConnection => ConnProto }
import org.totalgrid.reef.messaging.ProtoRegistry
import org.totalgrid.reef.protoapi.client.ServiceClient

import scala.collection.JavaConversions._
import org.totalgrid.reef.util.Conversion.convertIterableToMapified
import org.totalgrid.reef.app.ServiceHandler

import org.totalgrid.reef.protocol.api.{ IProtocol => Protocol }

// Data structure for handling the life cycle of connections
class FrontEndConnections(comms: Seq[Protocol], registry: ProtoRegistry, handler: ServiceHandler) extends KeyedMap[ConnProto] {

  def getKey(c: ConnProto) = c.getUid

  val protocols = comms.mapify { _.name }

  val maxAttemptsToRetryMeasurements = 1

  private def getProtocol(name: String): Protocol = protocols.get(name) match {
    case Some(p) => p
    case None => throw new IllegalArgumentException("Unknown protocol: " + name)
  }

  def hasChangedEnoughForReload(updated: ConnProto, existing: ConnProto) = {
    updated.getRouting.getServiceRoutingKey != existing.getRouting.getServiceRoutingKey
  }

  def addEntry(c: ConnProto) = {

    val protocol = getProtocol(c.getEndpoint.getProtocol)
    val endpoint = c.getEndpoint
    val port = c.getEndpoint.getPort

    // addressable client for the measurement stream
    val measurementClient = registry.getServiceClient(Measurements.MeasurementBatch.parseFrom, c.getRouting.getServiceRoutingKey)

    // extra client to break circular client -> handler -> issuer -> client chain
    val commandClient = registry.getServiceClient(Commands.UserCommandRequest.parseFrom)

    // add the device, get the command issuer callback
    if (protocol.requiresPort) protocol.addPort(port)
    val issuer = protocol.addEndpoint(endpoint.getName, port.getName, endpoint.getConfigFilesList.toList, batchPublish(measurementClient, 0), responsePublish(commandClient))

    info("Added endpoint " + c.getEndpoint.getName + " on protocol " + protocol.name + " routing key: " + c.getRouting.getServiceRoutingKey)

    // TODO: subscribe to command requests by entity
    val subProto = Commands.UserCommandRequest.newBuilder.setStatus(Commands.CommandStatus.EXECUTING).build
    handler.addService(registry, 5000, Commands.UserCommandRequest.parseFrom, subProto, initialCommands(issuer), newCommands(issuer))
  }

  def removeEntry(c: ConnProto) {
    val protocol = getProtocol(c.getEndpoint.getProtocol)
    protocol.removeEndpoint(c.getEndpoint.getName)
    if (protocol.requiresPort) protocol.removePort(c.getEndpoint.getPort.getName)
    info("Removed endpoint " + c.getEndpoint.getName + " on protocol " + protocol.name)
  }

  /**
   * push measurement batchs to the addressable service
   */
  private def batchPublish(client: ServiceClient, attempts: Int)(x: Measurements.MeasurementBatch): Unit = {
    try {
      client.putOrThrow(x)
    } catch {
      case e: Exception =>
        if (attempts >= maxAttemptsToRetryMeasurements) error(e)
        else {
          info("Retrying publishing measurements : " + x.getMeasCount)
          batchPublish(client, attempts + 1)(x)
        }
    }
  }

  /**
   * send command responses back to the server
   */
  private def responsePublish(client: ServiceClient)(x: Commands.CommandResponse): Unit = {
    try {
      val cr = Commands.CommandRequest.newBuilder.setCorrelationId(x.getCorrelationId)
      val msg = Commands.UserCommandRequest.newBuilder.setCommandRequest(cr).setStatus(x.getStatus).build
      client.putOrThrow(msg)
    } catch {
      case e: Exception => error(e)
    }
  }

  /**
   * handle the "integrity poll" of commands that are still to be worked on
   * TODO: ignore old commands? only return non-expired commands?
   */
  private def initialCommands(issuer: Protocol.Issue)(crs: List[Commands.UserCommandRequest]) {
    crs.foreach { x =>
      issuer(x.getCommandRequest)
    }
  }

  /**
   * handle new added command commands, issuer will only respond to correct commands
   */
  private def newCommands(issuer: Protocol.Issue)(evt: Envelope.Event, cr: Commands.UserCommandRequest) {
    if (evt == Envelope.Event.ADDED) issuer(cr.getCommandRequest)
  }
}

