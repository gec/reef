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

import org.totalgrid.reef.client.service.proto.Measurements.MeasurementBatch

import org.totalgrid.reef.app.KeyedMap
import org.totalgrid.reef.client.service.proto.FEP.{ EndpointConnection, CommChannel }
import org.totalgrid.reef.client.{ SubscriptionBinding, AddressableDestination }

import net.agileautomata.executor4s.{ Failure, Success }
import org.totalgrid.reef.client.service.proto.Model.{ ReefID, ReefUUID }
import org.totalgrid.reef.client.service.command.{ CommandResultCallback, CommandRequestHandler }
import org.totalgrid.reef.client.service.proto.Commands.{ CommandStatus, CommandRequest }
import org.totalgrid.reef.client.sapi.client.rest.Client
import org.totalgrid.reef.client.service.proto.Commands
import org.totalgrid.reef.protocol.api.{ ProtocolManager, Publisher, CommandHandler, Protocol }
import org.totalgrid.reef.client.javaimpl.ClientWrapper

object ProtocolInterface {
  sealed trait ProtocolInterface
  case class TraitInterface(protocol: Protocol) extends ProtocolInterface
  case class ManagerInterface(protocol: ProtocolManager) extends ProtocolInterface
}

// Data structure for handling the life cycle of connections
class FrontEndConnections(comms: Seq[Protocol], mgrs: Map[String, ProtocolManager], newClient: => Client) extends KeyedMap[EndpointConnection] {
  import ProtocolInterface._

  case class EndpointComponent(commandAdapter: SubscriptionBinding)

  var endpointComponents = Map.empty[String, EndpointComponent]

  def getKey(c: EndpointConnection) = c.getId.getValue

  val protocols: Map[String, ProtocolInterface] = comms.map(p => p.name -> TraitInterface(p)).toMap ++ mgrs.mapValues(m => ManagerInterface(m))

  def hasChangedEnoughForReload(updated: EndpointConnection, existing: EndpointConnection) = {
    updated.hasRouting != existing.hasRouting ||
      (updated.hasRouting && updated.getRouting.getServiceRoutingKey != existing.getRouting.getServiceRoutingKey)
  }

  private def addProtocolTrait(protocol: Protocol, client: Client, services: FrontEndProviderServices, c: EndpointConnection): CommandRequestHandler = {
    val endpoint = c.getEndpoint

    val endpointName = c.getEndpoint.getName

    val batchPublisher = newMeasBatchPublisher(services, c.getRouting.getServiceRoutingKey)
    val endpointListener = newEndpointStatePublisher(services, c.getId, endpointName)

    val channelName = if (c.getEndpoint.hasChannel) {
      val port = c.getEndpoint.getChannel
      val channelListener = newChannelStatePublisher(services, port.getUuid, port.getName)
      protocol.addChannel(port, channelListener, client)
      port.getName
    } else {
      ""
    }
    // add the device, get the command issuer callback
    val cmdHandler = protocol.addEndpoint(endpointName, channelName, endpoint.getConfigFilesList.toList, batchPublisher, endpointListener, client)

    createCommandRequestHandler(cmdHandler)
  }

  def addEntry(c: EndpointConnection) = try {

    val client = newClient
    val services = client.getRpcInterface(classOf[FrontEndProviderServices])

    val protocolName = c.getEndpoint.getProtocol
    val endpointName = c.getEndpoint.getName

    val cmdHandler = protocols.get(protocolName) match {
      case None => throw new IllegalArgumentException("Unknown protocol: " + protocolName)
      case Some(p) => p match {
        case TraitInterface(protocol) => addProtocolTrait(protocol, client, services, c)
        case ManagerInterface(mgr) => mgr.addEndpoint(new ClientWrapper(client), c)
      }
    }

    val service = services.bindCommandHandler(c.getEndpoint.getUuid, cmdHandler).await
    endpointComponents += (endpointName -> EndpointComponent(service))

    logger.info("Added endpoint: " + endpointName + " on protocol: " + protocolName + ", routing key: " + c.getRouting.getServiceRoutingKey)
  } catch {
    case ex: Exception =>
      logger.error("Can't add endpoint: " + c.getEndpoint.getName, ex)
  }

  def removeEntry(c: EndpointConnection) = try {

    val protocolName = c.getEndpoint.getProtocol
    val endpointName = c.getEndpoint.getName

    logger.info("Removing endpoint: " + endpointName)

    protocols.get(protocolName) match {
      case None => throw new IllegalArgumentException("Unknown protocol: " + protocolName)
      case Some(p) =>

        // need to make sure we close the addressable service so no new commands
        // are sent to endpoint while we are removing it
        endpointComponents.get(endpointName).foreach { _.commandAdapter.cancel() }

        p match {
          case TraitInterface(protocol) => {
            protocol.removeEndpoint(endpointName)
            if (c.getEndpoint.hasChannel) protocol.removeChannel(c.getEndpoint.getChannel.getName)
          }
          case ManagerInterface(mgr) => {
            mgr.removeEndpoint(c)
          }
        }

        endpointComponents -= endpointName
    }

    logger.info("Removed endpoint: " + endpointName + " on protocol: " + protocolName)

  } catch {
    case ex: Exception =>
      logger.error("Can't remove endpoint: " + c.getEndpoint.getName, ex)
  }

  // TODO -fail the process if we can't publish measurements or state?

  private def newMeasBatchPublisher(services: FrontEndProviderServices, routingKey: String) = new Publisher[MeasurementBatch] {
    def publish(value: MeasurementBatch) = {
      services.publishMeasurements(value, new AddressableDestination(routingKey)).extract match {
        case Success(x) => logger.debug("Published a measurement batch of size: " + value.getMeasCount)
        case Failure(ex) => logger.error("Couldn't publish measurements: " + ex.getMessage)
      }
    }
  }

  private def newEndpointStatePublisher(services: FrontEndProviderServices, connectionId: ReefID, endpointName: String) = new Publisher[EndpointConnection.State] {
    def publish(state: EndpointConnection.State) = {
      services.alterEndpointConnectionState(connectionId, state).extract match {
        case Success(x) => logger.info("Updated endpoint state: " + endpointName + " state: " + x.getState)
        case Failure(ex) => logger.error("Couldn't update endpointState: " + ex.getMessage)
      }
    }
  }

  private def newChannelStatePublisher(services: FrontEndProviderServices, channelUuid: ReefUUID, channelName: String) = new Publisher[CommChannel.State] {
    def publish(state: CommChannel.State) = {
      services.alterCommunicationChannelState(channelUuid, state).extract match {
        case Success(x) => logger.info("Updated channel state: " + x.getName + " state: " + x.getState)
        case Failure(ex) => logger.error("Couldn't update channelState: " + ex.getMessage)
      }
    }
  }

  private def createCommandRequestHandler(cmdHandler: CommandHandler) = new CommandRequestHandler {
    def handleCommandRequest(cmdRequest: CommandRequest, resultCallback: CommandResultCallback) {
      cmdHandler.issue(cmdRequest, new Publisher[Commands.CommandStatus] {
        def publish(value: CommandStatus) {
          resultCallback.setCommandResult(value)
        }
      })
    }
  }
}

