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

import org.totalgrid.reef.client.service.proto.FEP.EndpointConnection
import org.totalgrid.reef.protocol.api.ProtocolManager
import org.totalgrid.reef.client.{ Client, SubscriptionBinding }
import org.totalgrid.reef.app.KeyedMap
import net.agileautomata.executor4s._

/**
 * Takes the add/remove EndpointConnection calls from the outside manager and shunts them to the correct ProtocolManager
 * implementation. It also creates a new client for each endpoint and manages binding the commandHandler to the client.
 */
class FrontEndConnections(protocolManagers: Map[String, ProtocolManager], newClient: => Client) extends KeyedMap[EndpointConnection] {

  case class EndpointComponent(commandAdapter: SubscriptionBinding)

  private var endpointComponents = Map.empty[String, EndpointComponent]

  def getKey(c: EndpointConnection) = c.getId.getValue

  def hasChangedEnoughForReload(updated: EndpointConnection, existing: EndpointConnection) = {
    updated.hasRouting != existing.hasRouting ||
      (updated.hasRouting && updated.getRouting.getServiceRoutingKey != existing.getRouting.getServiceRoutingKey)
  }

  def addEntry(c: EndpointConnection) = try {

    val client = newClient
    val services = client.getService(classOf[FrontEndProviderServices])

    val protocolName = c.getEndpoint.getProtocol
    val endpointName = c.getEndpoint.getName

    val cmdHandler = getProtocol(protocolName).addEndpoint(client, c)

    val commandHandlingService = services.bindCommandHandler(c.getEndpoint.getUuid, cmdHandler).await
    endpointComponents += (endpointName -> EndpointComponent(commandHandlingService))

    logger.info("Added endpoint: " + endpointName + " on protocol: " + protocolName + ", routing key: " + c.getRouting.getServiceRoutingKey)
  } catch {
    case ex: Exception =>
      logger.error("Can't add endpoint: " + c.getEndpoint.getName, ex)
  }

  def removeEntry(c: EndpointConnection) = try {

    val protocolName = c.getEndpoint.getProtocol
    val endpointName = c.getEndpoint.getName

    logger.info("Removing endpoint: " + endpointName)

    endpointComponents.get(endpointName) match {
      case None => logger.warn("Endpoint unknown: " + endpointName)
      case Some(components) =>
        // need to make sure we close the addressable service so no new commands
        // are sent to endpoint while we are removing it
        components.commandAdapter.cancel()
        endpointComponents -= endpointName

        getProtocol(protocolName).removeEndpoint(c)

        logger.info("Removed endpoint: " + endpointName + " on protocol: " + protocolName)
    }
  } catch {
    case ex: Exception =>
      logger.error("Can't remove endpoint: " + c.getEndpoint.getName, ex)
  }

  private def getProtocol(protocolName: String): ProtocolManager = {
    protocolManagers.get(protocolName) match {
      case None =>
        throw new IllegalArgumentException("Unknown protocol: " + protocolName + " expected: " + protocolManagers.keys)
      case Some(p) => p
    }
  }
}

