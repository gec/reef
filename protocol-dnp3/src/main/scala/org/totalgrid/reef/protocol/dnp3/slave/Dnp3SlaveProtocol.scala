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
package org.totalgrid.reef.protocol.dnp3.slave

import org.totalgrid.reef.protocol.dnp3.common.Dnp3ProtocolBase
import org.totalgrid.reef.proto.Model.ConfigFile
import org.totalgrid.reef.protocol.api.Protocol._
import org.totalgrid.reef.sapi.request.impl.AllScadaServiceImpl
import org.totalgrid.reef.executor.Executor
import org.totalgrid.reef.protocol.dnp3.{ ICommandAcceptor, IStackObserver }
import org.totalgrid.reef.protocol.api.{ CommandHandler => ProtocolCommandHandler }
import org.totalgrid.reef.proto.Commands.CommandRequest
import org.totalgrid.reef.util.Cancelable

case class SlaveObjectsContainer(stackObserver: IStackObserver, commandProxy: ICommandAcceptor, measProxy: SlaveMeasurementProxy)
    extends Cancelable {
  // we need to cancel measProxy to stop subscription
  def cancel() = measProxy.stop()
}

class Dnp3SlaveProtocol(services: AllScadaServiceImpl, exe: Executor) extends Dnp3ProtocolBase[SlaveObjectsContainer] {

  final override val name = "dnp3-slave"

  override def addEndpoint(endpointName: String,
    channelName: String,
    files: List[ConfigFile],
    batchPublisher: BatchPublisher,
    endpointPublisher: EndpointPublisher): ProtocolCommandHandler = {

    logger.info("Adding device with uid: " + endpointName + " onto channel " + channelName)

    val mapping = getMappingProto(files)
    val (slaveConfig, filterLevel) = SlaveXmlConfig.getSlaveConfigFromConfigFiles(files, mapping)

    val stackObserver = createStackObserver(endpointPublisher)
    slaveConfig.getSlave.setMpObserver(stackObserver)

    val commandReceiver = new SlaveCommandProxy(services, mapping, exe)

    val measAcceptor = dnp3.AddSlave(channelName, endpointName, filterLevel, commandReceiver, slaveConfig)

    val measProxy = new SlaveMeasurementProxy(services, mapping, measAcceptor, exe)
    map += endpointName -> SlaveObjectsContainer(stackObserver, commandReceiver, measProxy)

    // do nothing, no commands associated with "dnp3-slave" endpoint
    new ProtocolCommandHandler {
      def issue(cmd: CommandRequest, publisher: ResponsePublisher) {}
    }
  }
}