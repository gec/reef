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

import org.totalgrid.reef.proto.{ Commands, Measurements }
import org.totalgrid.reef.proto.FEP.{ CommEndpointConnection => ConnProto }
import org.totalgrid.reef.messaging.Connection
import org.totalgrid.reef.api.scalaclient.ClientSession

import scala.collection.JavaConversions._
import org.totalgrid.reef.util.Conversion.convertIterableToMapified
import org.totalgrid.reef.app.ServiceHandler

import org.totalgrid.reef.protocol.api.{ IProtocol, IPublisher, ICommandHandler, IResponseHandler, NullChannelListener, NullEndpointListener }
import org.totalgrid.reef.api.{ Envelope, IDestination, AddressableService }

// Data structure for handling the life cycle of connections
class FrontEndConnections(comms: Seq[IProtocol], conn: Connection) extends KeyedMap[ConnProto] {

  def getKey(c: ConnProto) = c.getUid

  val protocols = comms.mapify { _.name }

  val maxAttemptsToRetryMeasurements = 1

  private def getProtocol(name: String): IProtocol = protocols.get(name) match {
    case Some(p) => p
    case None => throw new IllegalArgumentException("Unknown protocol: " + name)
  }

  def hasChangedEnoughForReload(updated: ConnProto, existing: ConnProto) = {
    updated.hasRouting != existing.hasRouting ||
      (updated.hasRouting && updated.getRouting.getServiceRoutingKey != existing.getRouting.getServiceRoutingKey)
  }

  def addEntry(c: ConnProto) = {

    val protocol = getProtocol(c.getEndpoint.getProtocol)
    val endpoint = c.getEndpoint
    val port = c.getEndpoint.getChannel

    val publisher = getPublisher(conn.getClientSession(), c.getRouting.getServiceRoutingKey)

    // add the device, get the command issuer callback
    if (protocol.requiresChannel) protocol.addChannel(port, NullChannelListener)
    val cmdHandler = protocol.addEndpoint(endpoint.getName, port.getName, endpoint.getConfigFilesList.toList, publisher, NullEndpointListener)
    val service = new SingleEndpointCommandService(cmdHandler)
    conn.bindService(service, AddressableService(c.getRouting.getServiceRoutingKey))

    info("Added endpoint " + c.getEndpoint.getName + " on protocol " + protocol.name + " routing key: " + c.getRouting.getServiceRoutingKey)
  }

  def removeEntry(c: ConnProto) {
    val protocol = getProtocol(c.getEndpoint.getProtocol)
    protocol.removeEndpoint(c.getEndpoint.getName)
    if (protocol.requiresChannel) protocol.removeChannel(c.getEndpoint.getChannel.getName)
    info("Removed endpoint " + c.getEndpoint.getName + " on protocol " + protocol.name)
  }

  /**
   * push measurement batchs to the addressable service
   */
  private def batchPublish(client: ClientSession, attempts: Int, dest: IDestination)(x: Measurements.MeasurementBatch): Unit = {
    try {
      client.putOrThrow(x, destination = dest)
    } catch {
      case e: Exception =>
        if (attempts >= maxAttemptsToRetryMeasurements) error(e)
        else {
          info("Retrying publishing measurements : " + x.getMeasCount)
          batchPublish(client, attempts + 1, dest)(x)
        }
    }
  }

  private def getPublisher(client: ClientSession, routingKey: String) = new IPublisher {
    override def publish(batch: Measurements.MeasurementBatch) = batchPublish(client, 0, AddressableService(routingKey))(batch)
  }

  private def getResponseHandler(client: ClientSession) = new IResponseHandler {
    override def onResponse(rsp: Commands.CommandResponse) = responsePublish(client)(rsp)
  }

  /**
   * send command responses back to the server
   */
  private def responsePublish(client: ClientSession)(x: Commands.CommandResponse): Unit = {
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
  private def initialCommands(cmdHandler: ICommandHandler, rspHandler: IResponseHandler)(crs: List[Commands.UserCommandRequest]) =
    crs.foreach { x => cmdHandler.issue(x.getCommandRequest, rspHandler) }

  /**
   * handle new added commands, issuer will only respond to correct commands
   */
  private def newCommands(cmdHandler: ICommandHandler, rspHandler: IResponseHandler)(evt: Envelope.Event, cr: Commands.UserCommandRequest) =
    if (evt == Envelope.Event.ADDED) cmdHandler.issue(cr.getCommandRequest, rspHandler)

}

