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

import org.totalgrid.reef.protocol.dnp3.common.Dnp3ProtocolBase
import org.totalgrid.reef.client.service.proto.Model
import org.totalgrid.reef.protocol.api.{ Publisher, CommandHandler => ProtocolCommandHandler }
import org.totalgrid.reef.client.service.proto.Measurements.MeasurementBatch
import org.totalgrid.reef.protocol.dnp3.IStackObserver
import net.agileautomata.executor4s.Cancelable
import org.totalgrid.reef.client.sapi.client.rest.Client
import org.totalgrid.reef.client.service.proto.FEP.EndpointConnection

case class MasterObjectsContainer(dataObserver: MeasAdapter, stackObserver: IStackObserver,
  batchPublisher: Publisher[MeasurementBatch], commandAdapter: CommandAdapter)
    extends Cancelable {
  def cancel() = {}
}

class Dnp3MasterProtocol extends Dnp3ProtocolBase[MasterObjectsContainer] {

  final override val name = "dnp3"

  override def addEndpoint(endpointName: String,
    channelName: String,
    files: List[Model.ConfigFile],
    batchPublisher: Publisher[MeasurementBatch],
    endpointPublisher: Publisher[EndpointConnection.State],
    client: Client): ProtocolCommandHandler = {

    logger.info("Adding device with id: " + endpointName + " onto channel " + channelName)

    val (masterConfig, filterLevel) = MasterXmlConfig.getMasterConfig(files)

    val stackObserver = createStackObserver(endpointPublisher)
    masterConfig.getMaster.setMpObserver(stackObserver)

    val mapping = getMappingProto(files)
    val dataObserver = new MeasAdapter(mapping, batchPublisher.publish)

    val cmdAcceptor = dnp3.AddMaster(channelName, endpointName, filterLevel, dataObserver, masterConfig)

    val commandAdapter = new CommandAdapter(mapping, cmdAcceptor)
    map += endpointName -> MasterObjectsContainer(dataObserver, stackObserver, batchPublisher, commandAdapter)
    commandAdapter
  }
}