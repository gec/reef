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
package org.totalgrid.reef.protocol.dnp3

import org.totalgrid.reef.protocol.api._

import org.totalgrid.reef.protocol.api.{ CommandHandler => ProtocolCommandHandler }

import org.totalgrid.reef.proto.{ FEP, Mapping, Model }
import org.totalgrid.reef.xml.dnp3.{ Master, AppLayer, LinkLayer, LogLevel }

import scala.collection.immutable
import scala.collection.JavaConversions._
import org.totalgrid.reef.proto.Measurements.MeasurementBatch
import org.totalgrid.reef.util.{ Logging, XMLHelper }

class Dnp3Protocol extends Protocol with Logging {

  import Protocol._

  final override def name = "dnp3"
  final override def requiresChannel = true

  // There's some kind of problem with swig directors. This MeasAdapter is
  // getting garbage collected since the C++ world is the only thing holding onto
  // this object. Keep a map of meas adapters around by name to prevent this.
  private var map = immutable.Map.empty[String, (MeasAdapter, Publisher[MeasurementBatch], IStackObserver)]

  private var physMonitorMap = immutable.Map.empty[String, IPhysicalLayerObserver]

  // TODO: fix Protocol trait to send nonop data on same channel as meas data
  private val log = new LogAdapter
  private val dnp3 = new StackManager
  dnp3.AddLogHook(log)

  final def Shutdown() = dnp3.Shutdown()

  override def addChannel(p: FEP.CommChannel, publisher: ChannelPublisher) = {

    val physMonitor = new IPhysicalLayerObserver {
      override def OnStateChange(state: PhysicalLayerState) = {
        logger.error("Transition: " + state)
        publisher.publish(translate(state))
      }
    }

    physMonitorMap += p.getName -> physMonitor

    val settings = new PhysLayerSettings(FilterLevel.LEV_WARNING, 1000, physMonitor)

    if (p.hasIp) {
      val ip = p.getIp
      ip.getMode match {
        case FEP.IpPort.Mode.CLIENT => dnp3.AddTCPClient(p.getName, settings, ip.getAddress, ip.getPort)
        case FEP.IpPort.Mode.SERVER => dnp3.AddTCPServer(p.getName, settings, ip.getAddress, ip.getPort)
      }
    } else if (p.hasSerial) {
      dnp3.AddSerial(p.getName, settings, configure(p.getSerial))
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

  override def addEndpoint(endpoint: String,
    channelName: String,
    files: List[Model.ConfigFile],
    batchPublisher: BatchPublisher,
    endpointPublisher: EndpointPublisher): ProtocolCommandHandler = {

    logger.info("Adding device with uid: " + endpoint + " onto channel " + channelName)

    val configFile = Protocol.find(files, "text/xml") //there is should be only one XML file
    val xml = XMLHelper.read(configFile.getFile.toByteArray, classOf[Master])
    val master = getMasterConfig(xml)

    val observer = new IStackObserver {
      override def OnStateChange(state: StackStates) = {
        endpointPublisher.publish(translate(state))
      }
    }
    master.getMaster.setMpObserver(observer)

    val filterLevel = Option(xml.getLog).map { logElem => configure(logElem.getFilter) }.getOrElse(FilterLevel.LEV_WARNING)

    val mapping = Mapping.IndexMapping.parseFrom(Protocol.find(files, "application/vnd.google.protobuf; proto=reef.proto.Mapping.IndexMapping").getFile)

    val meas_adapter = new MeasAdapter(mapping, batchPublisher.publish)
    map += endpoint -> (meas_adapter, batchPublisher, observer)
    val cmd = dnp3.AddMaster(channelName, endpoint, filterLevel, meas_adapter, master)
    new CommandAdapter(mapping, cmd)
  }

  override def removeEndpoint(endpoint: String) = {

    logger.debug("Not removing stack " + endpoint + " as per workaround")
    /* BUG in the DNP3 bindings causes removing endpoints to deadlock until integrity poll
    times out.
    stack will get removed when port is removed
     */

    /*info { "removing stack with name: " + endpoint }
    try {
      dnp3.RemoveStack(endpoint)
      map -= endpoint
    } catch {
      case x => println("From remove stack: " + x)
    }
    info { "removed stack with name: " + endpoint }*/
  }

  private def getMasterConfig(xml: Master): MasterStackConfig = {
    val config = new MasterStackConfig
    config.setMaster(configure(xml, xml.getStack.getAppLayer.getMaxFragSize))
    config.setApp(configure(xml.getStack.getAppLayer))
    config.setLink(configure(xml.getStack.getLinkLayer))
    config
  }

  private def configure(xml: LinkLayer): LinkConfig = {
    val cfg = new LinkConfig(xml.isIsMaster, xml.isUseConfirmations)
    cfg.setNumRetry(xml.getNumRetries)
    cfg.setRemoteAddr(xml.getRemoteAddress)
    cfg.setLocalAddr(xml.getLocalAddress)
    cfg.setTimeout(xml.getAckTimeoutMS)
    cfg
  }

  private def configure(xml: AppLayer): AppConfig = {
    val cfg = new AppConfig
    cfg.setFragSize(xml.getMaxFragSize)
    cfg.setRspTimeout(xml.getTimeoutMS)
    cfg
  }

  private def configure(xml: Master, fragSize: Int): MasterConfig = {
    val cfg = new MasterConfig
    cfg.setAllowTimeSync(xml.getMasterSettings.isAllowTimeSync)
    cfg.setTaskRetryRate(xml.getMasterSettings.getTaskRetryMS)
    cfg.setIntegrityRate(xml.getMasterSettings.getIntegrityPeriodMS)

    cfg.setDoUnsolOnStartup(xml.getUnsol.isDoTask)
    cfg.setEnableUnsol(xml.getUnsol.isEnable)

    var unsol_class = 0
    if (xml.getUnsol.isClass1) unsol_class = unsol_class | PointClass.PC_CLASS_1.swigValue
    if (xml.getUnsol.isClass2) unsol_class = unsol_class | PointClass.PC_CLASS_2.swigValue
    if (xml.getUnsol.isClass3) unsol_class = unsol_class | PointClass.PC_CLASS_3.swigValue
    cfg.setUnsolClassMask(unsol_class)

    cfg.setFragSize(fragSize)

    xml.getScanList.getExceptionScan.foreach { scan =>
      var point_class = PointClass.PC_CLASS_0.swigValue
      if (scan.isClass1) point_class = point_class | PointClass.PC_CLASS_1.swigValue
      if (scan.isClass2) point_class = point_class | PointClass.PC_CLASS_2.swigValue
      if (scan.isClass3) point_class = point_class | PointClass.PC_CLASS_3.swigValue
      cfg.AddExceptionScan(point_class, scan.getPeriodMS)
    }

    cfg
  }

  private def configure(xmlLevel: LogLevel): FilterLevel = {
    xmlLevel match {
      case LogLevel.LOG_EVENT => FilterLevel.LEV_EVENT
      case LogLevel.LOG_ERROR => FilterLevel.LEV_ERROR
      case LogLevel.LOG_WARNING => FilterLevel.LEV_WARNING
      case LogLevel.LOG_INFO => FilterLevel.LEV_INFO
      case LogLevel.LOG_INTERPRET => FilterLevel.LEV_INTERPRET
      case LogLevel.LOG_COMM => FilterLevel.LEV_COMM
      case LogLevel.LOG_DEBUG => FilterLevel.LEV_DEBUG
      case _ => FilterLevel.LEV_WARNING
    }
  }

  private def configure(channel: FEP.SerialPort): SerialSettings = {
    val ss = new SerialSettings
    ss.setMBaud(channel.getBaudRate)
    ss.setMDataBits(channel.getDataBits)
    ss.setMDevice(channel.getPortName)
    ss.setMFlowType(channel.getFlow match {
      case FEP.SerialPort.Flow.FLOW_NONE => FlowType.FLOW_NONE
      case FEP.SerialPort.Flow.FLOW_HARDWARE => FlowType.FLOW_HARDWARE
      case FEP.SerialPort.Flow.FLOW_XONXOFF => FlowType.FLOW_XONXOFF
    })
    ss.setMParity(channel.getParity match {
      case FEP.SerialPort.Parity.PAR_NONE => ParityType.PAR_NONE
      case FEP.SerialPort.Parity.PAR_EVEN => ParityType.PAR_EVEN
      case FEP.SerialPort.Parity.PAR_ODD => ParityType.PAR_ODD
    })
    ss.setMStopBits(channel.getStopBits)
    ss
  }

  private def translate(state: StackStates): FEP.CommEndpointConnection.State = state match {
    case StackStates.SS_COMMS_DOWN => FEP.CommEndpointConnection.State.COMMS_DOWN
    case StackStates.SS_COMMS_UP => FEP.CommEndpointConnection.State.COMMS_UP
    case StackStates.SS_UNKNOWN => FEP.CommEndpointConnection.State.UNKNOWN
  }

  private def translate(state: PhysicalLayerState): FEP.CommChannel.State = state match {
    case PhysicalLayerState.PLS_CLOSED => FEP.CommChannel.State.CLOSED
    case PhysicalLayerState.PLS_OPEN => FEP.CommChannel.State.OPEN
    case PhysicalLayerState.PLS_OPENING => FEP.CommChannel.State.OPENING
    // TODO: which state CommChannel.State is stopped and waiting
    case PhysicalLayerState.PLS_SHUTDOWN => FEP.CommChannel.State.CLOSED
    case PhysicalLayerState.PLS_WAITING => FEP.CommChannel.State.ERROR
  }
}