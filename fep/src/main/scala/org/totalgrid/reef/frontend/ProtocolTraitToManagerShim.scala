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

import org.totalgrid.reef.client.service.proto.Measurements.MeasurementBatch
import net.agileautomata.executor4s.{ Failure, Success }
import org.totalgrid.reef.client.service.proto.Model.{ ReefUUID, ReefID }
import org.totalgrid.reef.client.service.proto.FEP.{ CommChannel, EndpointConnection }
import org.totalgrid.reef.client.service.command.{ CommandResultCallback, CommandRequestHandler }
import org.totalgrid.reef.client.service.proto.Commands
import org.totalgrid.reef.client.service.proto.Commands.{ CommandStatus, CommandRequest }
import org.totalgrid.reef.protocol.api.{ Protocol, CommandHandler, Publisher, ProtocolManager }
import org.totalgrid.reef.client.{ Client, AddressableDestination }
import com.weiglewilczek.slf4s.Logging

import scala.collection.JavaConversions._

/**
 * shim layer that converts from the "general purpose" ProtocolManager to the "old style" Protocol Trait api
 */
class ProtocolTraitToManagerShim(protocol: Protocol) extends ProtocolManager with Logging {

  def addEndpoint(client: Client, c: EndpointConnection) = {
    val endpoint = c.getEndpoint

    val endpointName = c.getEndpoint.getName

    val services = client.getService(classOf[FrontEndProviderServices])
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

  def removeEndpoint(c: EndpointConnection) {
    val endpointName = c.getEndpoint.getName
    protocol.removeEndpoint(endpointName)
    if (c.getEndpoint.hasChannel) protocol.removeChannel(c.getEndpoint.getChannel.getName)
  }

  private def newMeasBatchPublisher(services: FrontEndProviderServices, routingKey: String) = new Publisher[MeasurementBatch] {
    def publish(value: MeasurementBatch) {
      try {
        services.publishMeasurements(value, new AddressableDestination(routingKey)).await()
        logger.debug("Published a measurement batch of size: " + value.getMeasCount)
      } catch {
        case ex => logger.error("Couldn't publish measurements: " + ex.getMessage)
      }
    }
  }

  private def newEndpointStatePublisher(services: FrontEndProviderServices, connectionId: ReefID, endpointName: String) = new Publisher[EndpointConnection.State] {
    def publish(state: EndpointConnection.State) {
      try {
        val result = services.alterEndpointConnectionState(connectionId, state).await()
        logger.info("Updated endpoint state: " + endpointName + " state: " + result.getState)
      } catch {
        case ex => logger.error("Couldn't update endpointState: " + ex.getMessage)
      }
    }
  }

  private def newChannelStatePublisher(services: FrontEndProviderServices, channelUuid: ReefUUID, channelName: String) = new Publisher[CommChannel.State] {
    def publish(state: CommChannel.State) {
      try {
        val result = services.alterCommunicationChannelState(channelUuid, state).await()
        logger.info("Updated channel state: " + result.getName + " state: " + result.getState)
      } catch {
        case ex => logger.error("Couldn't update channelState: " + ex.getMessage)
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
