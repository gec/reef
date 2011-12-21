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
package org.totalgrid.reef.protocol.dnp3.common

import scala.collection.JavaConversions._

import net.agileautomata.executor4s.Cancelable
import org.totalgrid.reef.client.service.proto.{ FEP, Mapping, Model }

import org.totalgrid.reef.protocol.api._
import org.totalgrid.reef.protocol.dnp3._
import com.weiglewilczek.slf4s.Logging
import org.totalgrid.reef.client.sapi.client.rest.Client
import org.totalgrid.reef.client.service.proto.FEP.{ CommChannel, EndpointConnection }

abstract class Dnp3ProtocolBase[ObjectContainer <: Cancelable] extends Protocol with Logging {

  import XmlToProtoTranslations._

  final override def requiresChannel = true

  // There's some kind of problem with swig directors. This MeasAdapter is
  // getting garbage collected since the C++ world is the only thing holding onto
  // this object. Keep a map of meas adapters around by name to prevent this.
  protected var map = Map.empty[String, ObjectContainer]

  private var physMonitorMap = Map.empty[String, IPhysicalLayerObserver]

  // TODO: fix Protocol trait to send nonop data on same channel as meas data
  protected val log = new LogAdapter
  protected val dnp3 = new StackManager
  dnp3.AddLogHook(log)

  final def Shutdown() = dnp3.Shutdown()

  override def addChannel(p: FEP.CommChannel, publisher: Publisher[CommChannel.State], client: Client) = {

    val physMonitor = createChannelObserver(p.getName, publisher)

    physMonitorMap += p.getName -> physMonitor

    val settings = new PhysLayerSettings(FilterLevel.LEV_WARNING, 1000, physMonitor)

    if (p.hasIp) {
      val ip = p.getIp
      ip.getMode match {
        case FEP.IpPort.Mode.CLIENT => dnp3.AddTCPClient(p.getName, settings, ip.getAddress, ip.getPort)
        case FEP.IpPort.Mode.SERVER => dnp3.AddTCPServer(p.getName, settings, ip.getAddress, ip.getPort)
      }
    } else if (p.hasSerial) {
      dnp3.AddSerial(p.getName, settings, getSerialSettings(p.getSerial))
    } else {
      throw new Exception("Invalid channel info, no type set")
    }
    logger.info("Added channel with name: " + p.getName)
  }

  override def removeChannel(channel: String) = {
    logger.debug("Removing channel with name: " + channel)
    dnp3.RemovePort(channel)
    physMonitorMap -= channel
    logger.info("Removed channel with name: " + channel)
  }

  override def removeEndpoint(endpointName: String) = {
    logger.info { "removing stack with name: " + endpointName }

    map.get(endpointName).foreach { _.cancel() }

    dnp3.RemoveStack(endpointName)

    map -= endpointName

    logger.info { "removed stack with name: " + endpointName }
  }

  protected def getMappingProto(files: List[Model.ConfigFile]) = {
    val configFile = Protocol.find(files, "application/vnd.google.protobuf; proto=reef.proto.Mapping.IndexMapping")
    Mapping.IndexMapping.parseFrom(configFile.getFile)
  }

  protected def createStackObserver(endpointPublisher: Publisher[EndpointConnection.State]) = {
    new IStackObserver {
      override def OnStateChange(state: StackStates) = {
        endpointPublisher.publish(translateStackState(state))
      }
    }
  }

  protected def createChannelObserver(channelName: String, publisher: Publisher[CommChannel.State]) = {
    new IPhysicalLayerObserver {
      override def OnStateChange(state: PhysicalLayerState) = {
        logger.error("Channel " + channelName + " transitioned to state: " + state)
        publisher.publish(translatePhysicalLayerState(state))
      }
    }
  }

  private def translateStackState(state: StackStates): FEP.EndpointConnection.State = state match {
    case StackStates.SS_COMMS_DOWN => FEP.EndpointConnection.State.COMMS_DOWN
    case StackStates.SS_COMMS_UP => FEP.EndpointConnection.State.COMMS_UP
    case StackStates.SS_UNKNOWN => FEP.EndpointConnection.State.UNKNOWN
  }

  private def translatePhysicalLayerState(state: PhysicalLayerState): FEP.CommChannel.State = state match {
    case PhysicalLayerState.PLS_CLOSED => FEP.CommChannel.State.CLOSED
    case PhysicalLayerState.PLS_OPEN => FEP.CommChannel.State.OPEN
    case PhysicalLayerState.PLS_OPENING => FEP.CommChannel.State.OPENING
    // TODO: which state CommChannel.State is stopped and waiting
    case PhysicalLayerState.PLS_SHUTDOWN => FEP.CommChannel.State.CLOSED
    case PhysicalLayerState.PLS_WAITING => FEP.CommChannel.State.ERROR
  }
}