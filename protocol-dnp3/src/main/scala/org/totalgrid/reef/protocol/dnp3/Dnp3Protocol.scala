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

import org.totalgrid.reef.protocol.api.{ ICommandHandler => ProtocolCommandHandler }

import org.totalgrid.reef.proto.{ FEP, Mapping, Model }
import org.totalgrid.reef.xml.dnp3.{ Master, AppLayer, LinkLayer }
import org.totalgrid.reef.util.XMLHelper

import scala.collection.immutable
import scala.collection.JavaConversions._
import org.totalgrid.reef.proto.Measurements.MeasurementBatch

class Dnp3Protocol extends BaseProtocol with EndpointAlwaysOnline with ChannelAlwaysOnline {

  override def name = "dnp3"

  override def requiresChannel = true

  // There's some kind of problem with swig directors. This MeasAdapter is
  // getting garbage collected since the C++ world is the only thing holding onto
  // this object. Keep a map of meas adapters around by name to prevent this.
  private var map = immutable.Map.empty[String, (MeasAdapter, IListener[MeasurementBatch])]

  // TODO: fix Protocol trait to send nonop data on same channel as meas data
  private val log = new LogAdapter
  private val dnp3 = new StackManager(true)
  dnp3.AddLogHook(log)

  override def _addChannel(p: FEP.CommChannel, listener: IListener[FEP.CommChannel.State]) = {

    val settings = new PhysLayerSettings(FilterLevel.LEV_WARNING, 1000)

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

  override def _removeChannel(channel: String) = {
    logger.debug("removing channel with name: " + channel)
    dnp3.RemovePort(channel)
    logger.info("Removed channel with name: " + channel)
  }

  override def _addEndpoint(endpoint: String,
    channelName: String,
    files: List[Model.ConfigFile],
    publisher: IListener[MeasurementBatch],
    listener: IListener[FEP.CommEndpointConnection.State]): ProtocolCommandHandler = {

    logger.info("Adding device with uid: " + endpoint + " onto channel " + channelName)

    val master = getMasterConfig(IProtocol.find(files, "text/xml")) //there is should be only one XML file
    val mapping = Mapping.IndexMapping.parseFrom(IProtocol.find(files, "application/vnd.google.protobuf; proto=reef.proto.Mapping.IndexMapping").getFile)

    val meas_adapter = new MeasAdapter(mapping, publisher.onUpdate)
    map += endpoint -> (meas_adapter, publisher)
    val cmd = dnp3.AddMaster(channelName, endpoint, FilterLevel.LEV_WARNING, meas_adapter, master)
    new CommandAdapter(mapping, cmd)
  }

  override def _removeEndpoint(endpoint: String) = {

    logger.debug("Not removing stack " + endpoint + " as per workaround")
    /* BUG in the DNP3 bindings causes removing endpoints to deadlock until integrity poll
    times out.
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

  private def getMasterConfig(file: Model.ConfigFile): MasterStackConfig = {
    val xml = XMLHelper.read(file.getFile.toByteArray, classOf[Master])
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
}