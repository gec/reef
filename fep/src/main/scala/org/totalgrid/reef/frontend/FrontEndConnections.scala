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

import org.totalgrid.reef.proto.FEP.{ CommEndpointConnection => ConnProto }
import org.totalgrid.reef.proto.FEP.CommChannel
import scala.collection.JavaConversions._
import org.totalgrid.reef.util.Conversion.convertIterableToMapified

import org.totalgrid.reef.protocol.api._
import org.totalgrid.reef.sapi._
import org.totalgrid.reef.proto.Model.ReefUUID
import org.totalgrid.reef.proto.Measurements.MeasurementBatch
import org.totalgrid.reef.broker.CloseableChannel
import org.totalgrid.reef.japi.Envelope
import org.totalgrid.reef.messaging.{ OrderedServiceTransmitter, Connection }
import org.totalgrid.reef.app.KeyedMap

// Data structure for handling the life cycle of connections
class FrontEndConnections(comms: Seq[Protocol], conn: Connection) extends KeyedMap[ConnProto] {

  val retries = 3

  def getKey(c: ConnProto) = c.getUid

  val protocols = comms.mapify { _.name }

  case class EndpointComponent(commandAdapter: CloseableChannel, transmitter: OrderedServiceTransmitter)

  var endpointComponents = Map.empty[String, EndpointComponent]

  val pool = conn.getSessionPool

  private def getProtocol(name: String): Protocol = protocols.get(name) match {
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

    val transmitter = new OrderedServiceTransmitter(pool)

    val batchPublisher = newMeasBatchPublisher(transmitter, c.getRouting.getServiceRoutingKey)
    val channelListener = newChannelStatePublisher(transmitter, port.getUuid)
    val endpointListener = newEndpointStatePublisher(transmitter, c.getUid)

    // add the device, get the command issuer callback
    if (protocol.requiresChannel) protocol.addChannel(port, channelListener)
    val cmdHandler = protocol.addEndpoint(endpoint.getName, port.getName, endpoint.getConfigFilesList.toList, batchPublisher, endpointListener)
    logger.info("Added endpoint: " + c.getEndpoint.getName + " on protocol: " + protocol.name + ", routing key: " + c.getRouting.getServiceRoutingKey)

    val service = conn.bindService(new SingleEndpointCommandService(cmdHandler), AddressableDestination(c.getRouting.getServiceRoutingKey))
    endpointComponents += c.getEndpoint.getName -> EndpointComponent(service, transmitter)
  }

  def removeEntry(c: ConnProto) {
    logger.debug("Removing endpoint: " + c.getEndpoint.getName)
    val protocol = getProtocol(c.getEndpoint.getProtocol)

    // need to make sure we close the addressable service so no new commands
    // are sent to endpoint while we are removing it
    endpointComponents.get(c.getEndpoint.getName) match {
      case Some(EndpointComponent(serviceBinding, t)) => serviceBinding.close
      case None =>
    }

    protocol.removeEndpoint(c.getEndpoint.getName)
    if (protocol.requiresChannel) protocol.removeChannel(c.getEndpoint.getChannel.getName)

    // now that all of the endpoints callbacks should have fired we stop the transmitter and
    // flush the pending messages
    endpointComponents.get(c.getEndpoint.getName) match {
      case Some(EndpointComponent(s, transmitter)) => transmitter.shutdown
      case None =>
    }
    endpointComponents -= c.getEndpoint.getName
    logger.info("Removed endpoint: " + c.getEndpoint.getName + " on protocol: " + protocol.name)
  }

  private def newMeasBatchPublisher(tx: OrderedServiceTransmitter, routingKey: String) =
    new IdentityOrderedPublisher[MeasurementBatch](tx, Envelope.Verb.POST, AddressableDestination(routingKey), retries)

  private def newEndpointStatePublisher(tx: OrderedServiceTransmitter, connectionUid: String) = {
    def transform(x: ConnProto.State): ConnProto = ConnProto.newBuilder.setUid(connectionUid).setState(x).build
    new OrderedPublisher(tx, Envelope.Verb.POST, AnyNodeDestination, retries)(transform)
  }

  private def newChannelStatePublisher(tx: OrderedServiceTransmitter, channelUid: ReefUUID) = {
    def transform(x: CommChannel.State): CommChannel = CommChannel.newBuilder.setUuid(channelUid).setState(x).build
    new OrderedPublisher(tx, Envelope.Verb.POST, AnyNodeDestination, retries)(transform)
  }
}

