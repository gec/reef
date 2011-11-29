/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the GNU Affero General Public License
 * Version 3.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.frontend

import scala.collection.JavaConversions._

import org.totalgrid.reef.proto.Measurements.MeasurementBatch

import org.totalgrid.reef.app.KeyedMap
import org.totalgrid.reef.proto.FEP.{ CommEndpointConnection, CommChannel }
import net.agileautomata.executor4s.Cancelable
import org.totalgrid.reef.clientapi.AddressableDestination

import net.agileautomata.executor4s.{ Failure, Success }
import org.totalgrid.reef.proto.Model.{ ReefID, ReefUUID }
import org.totalgrid.reef.client.rpc.commands.{ CommandResultCallback, CommandRequestHandler }
import org.totalgrid.reef.proto.Commands.{ CommandStatus, CommandRequest }
import org.totalgrid.reef.protocol.api.{ CommandHandler, Protocol }

// Data structure for handling the life cycle of connections
class FrontEndConnections(comms: Seq[Protocol], client: FrontEndProviderServices) extends KeyedMap[CommEndpointConnection] {

  case class EndpointComponent(commandAdapter: Cancelable)

  var endpointComponents = Map.empty[String, EndpointComponent]

  private def getProtocol(name: String): Protocol = protocols.get(name) match {
    case Some(p) => p
    case None => throw new IllegalArgumentException("Unknown protocol: " + name)
  }

  def getKey(c: CommEndpointConnection) = c.getId.getValue

  val protocols = comms.map(p => p.name -> p).toMap

  def hasChangedEnoughForReload(updated: CommEndpointConnection, existing: CommEndpointConnection) = {
    updated.hasRouting != existing.hasRouting ||
      (updated.hasRouting && updated.getRouting.getServiceRoutingKey != existing.getRouting.getServiceRoutingKey)
  }

  def addEntry(c: CommEndpointConnection) = try {

    val protocol = getProtocol(c.getEndpoint.getProtocol)
    val endpoint = c.getEndpoint
    val port = c.getEndpoint.getChannel

    val endpointName = c.getEndpoint.getName

    val batchPublisher = newMeasBatchPublisher(c.getRouting.getServiceRoutingKey)
    val channelListener = newChannelStatePublisher(port.getUuid, port.getName)
    val endpointListener = newEndpointStatePublisher(c.getId, endpointName)

    // add the device, get the command issuer callback
    if (protocol.requiresChannel) protocol.addChannel(port, channelListener)
    val cmdHandler = protocol.addEndpoint(endpointName, port.getName, endpoint.getConfigFilesList.toList, batchPublisher, endpointListener)

    val service = client.bindCommandHandler(c.getEndpoint.getUuid, createCommandRequestHandler(cmdHandler)).await
    endpointComponents += endpointName -> EndpointComponent(service)

    logger.info("Added endpoint: " + endpointName + " on protocol: " + protocol.name + ", routing key: " + c.getRouting.getServiceRoutingKey)
  } catch {
    case ex: Exception =>
      logger.error("Can't add endpoint: " + c.getEndpoint.getName, ex)
  }

  def removeEntry(c: CommEndpointConnection) = try {
    val endpointName = c.getEndpoint.getName

    logger.info("Removing endpoint: " + endpointName)
    val protocol = getProtocol(c.getEndpoint.getProtocol)

    // need to make sure we close the addressable service so no new commands
    // are sent to endpoint while we are removing it
    endpointComponents.get(endpointName).foreach { _.commandAdapter.cancel() }

    protocol.removeEndpoint(endpointName)
    if (protocol.requiresChannel) protocol.removeChannel(c.getEndpoint.getChannel.getName)

    endpointComponents -= endpointName
    logger.info("Removed endpoint: " + endpointName + " on protocol: " + protocol.name)
  } catch {
    case ex: Exception =>
      logger.error("Can't remove endpoint: " + c.getEndpoint.getName, ex)
  }

  // TODO -fail the process if we can't publish measurements or state?

  private def newMeasBatchPublisher(routingKey: String) = new Protocol.BatchPublisher {
    def publish(value: MeasurementBatch) = {
      client.publishMeasurements(value, new AddressableDestination(routingKey)).extract match {
        case Success(x) => logger.debug("Published a measurement batch of size: " + value.getMeasCount)
        case Failure(ex) => logger.error("Couldn't publish measurements: " + ex.getMessage)
      }
    }
  }

  private def newEndpointStatePublisher(connectionId: ReefID, endpointName: String) = new Protocol.EndpointPublisher {
    def publish(state: CommEndpointConnection.State) = {
      client.alterEndpointConnectionState(connectionId, state).extract match {
        case Success(x) => logger.info("Updated endpoint state: " + endpointName + " state: " + x.getState)
        case Failure(ex) => logger.error("Couldn't update endpointState: " + ex.getMessage)
      }
    }
  }

  private def newChannelStatePublisher(channelUuid: ReefUUID, channelName: String) = new Protocol.ChannelPublisher {
    def publish(state: CommChannel.State) = {
      client.alterCommunicationChannelState(channelUuid, state).extract match {
        case Success(x) => logger.info("Updated channel state: " + x.getName + " state: " + x.getState)
        case Failure(ex) => logger.error("Couldn't update channelState: " + ex.getMessage)
      }
    }
  }

  private def createCommandRequestHandler(cmdHandler: CommandHandler) = new CommandRequestHandler {
    def handleCommandRequest(cmdRequest: CommandRequest, resultCallback: CommandResultCallback) {
      cmdHandler.issue(cmdRequest, new Protocol.ResponsePublisher {
        def publish(value: CommandStatus) {
          resultCallback.setCommandResult(value)
        }
      })
    }
  }
}

