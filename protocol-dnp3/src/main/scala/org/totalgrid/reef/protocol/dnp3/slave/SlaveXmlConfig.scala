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

import scala.collection.JavaConversions._

import org.totalgrid.reef.protocol.dnp3.common.XmlToProtoTranslations
import org.totalgrid.reef.util.XMLHelper
import org.totalgrid.reef.protocol.api.Protocol
import org.totalgrid.reef.protocol.dnp3.xml.Slave.SlaveConfig
import org.totalgrid.reef.client.service.proto.Mapping.{ DataType, IndexMapping, CommandType }
import org.totalgrid.reef.client.service.proto.Model.{ ConfigFile }
import org.totalgrid.reef.protocol.dnp3.master.{ MasterXmlConfig }
import org.totalgrid.reef.protocol.dnp3.xml.Slave.SlaveConfig.{ TimeIINTask, UnsolDefaults }
import org.totalgrid.reef.protocol.dnp3.xml._
import org.totalgrid.reef.protocol.dnp3.{ GrpVar, CommandModes, ClassMask, ControlRecord, PointRecord, EventPointRecord, DeadbandPointRecord, DeviceTemplate, PointClass, EventMaxConfig, SlaveConfig => DnpSlaveConfig, FilterLevel, SlaveStackConfig }

object SlaveXmlConfig {
  def getSlaveConfigFromConfigFiles(files: List[ConfigFile], mapping: IndexMapping): (SlaveStackConfig, FilterLevel) = {
    val configFile = Protocol.find(files, "text/xml") //there is should be only one XML file
    val xml = XMLHelper.read(configFile.getFile.toByteArray, classOf[Slave])
    createSlaveConfig(xml, mapping)
  }

  def createSlaveConfig(xml: Slave, mapping: IndexMapping): (SlaveStackConfig, FilterLevel) = {
    (loadConfig(xml, createDeviceTemplate(mapping)), XmlToProtoTranslations.filterLevel(xml.getLog))
  }

  private def createDeviceTemplate(mapping: IndexMapping) = {
    val dt = new DeviceTemplate()

    val pointClass = PointClass.PC_CLASS_1
    mapping.getMeasmapList.toList.foreach { e =>

      // TODO: make slave config configure pointClass and deadband?

      val name = e.getPointName
      e.getType match {
        case DataType.ANALOG => dt.getMAnalog.add(new DeadbandPointRecord(name, pointClass, 0.0))
        case DataType.BINARY => dt.getMBinary.add(new EventPointRecord(name, pointClass))
        case DataType.COUNTER => dt.getMCounter.add(new EventPointRecord(name, pointClass))
        case DataType.CONTROL_STATUS => dt.getMControlStatus.add(new PointRecord(name))
        case DataType.SETPOINT_STATUS => dt.getMSetpointStatus.add(new PointRecord(name))
        case _ => throw new RuntimeException("unknown meas type")
      }
    }
    mapping.getCommandmapList.toList.foreach { e =>

      // TODO: make slave config controls SBO or DO configurable?

      val mode = CommandModes.CM_SBO_OR_DO
      val name = e.getCommandName
      e.getType match {
        case CommandType.SETPOINT => dt.getMSetpoints.add(new ControlRecord(name, mode))
        case _ => dt.getMControls.add(new ControlRecord(name, mode))
      }
    }
    dt
  }

  private def loadConfig(xml: Slave, deviceTemplate: DeviceTemplate) = {
    val cfg = new SlaveStackConfig

    cfg.setApp(MasterXmlConfig.configure(xml.getStack.getAppLayer))
    cfg.setLink(MasterXmlConfig.configure(xml.getStack.getLinkLayer))

    cfg.setSlave(configure(xml.getSlaveConfig))
    cfg.setDevice(deviceTemplate)

    cfg
  }

  private def configure(xml: SlaveConfig) = {
    val cfg = new DnpSlaveConfig

    cfg.setMAllowTimeSync(xml.getTimeIINTask.isDoTask)
    cfg.setMTimeSyncPeriod(xml.getTimeIINTask.getPeriodMS)

    cfg.setMEventMaxConfig(configureEvents(xml))

    cfg.setMUnsolMask(new ClassMask(xml.getUnsolDefaults.isDoClass1, xml.getUnsolDefaults.isDoClass2, xml.getUnsolDefaults.isDoClass3))
    cfg.setMUnsolPackDelay(xml.getUnsolDefaults.getPackDelayMS)
    cfg.setMUnsolRetryDelay(xml.getUnsolDefaults.getRetryMS)

    // TODO: make slave config configure default event types?

    // use double by default
    cfg.setMEventAnalog(new GrpVar(32, 8))
    cfg.setMStaticAnalog(new GrpVar(30, 6))

    cfg
  }

  private def configureEvents(xml: SlaveConfig) = {
    val events = new EventMaxConfig()
    events.setMMaxAnalogEvents(xml.getMaxAnalogEvents)
    events.setMMaxBinaryEvents(xml.getMaxBinaryEvents)
    events.setMMaxCounterEvents(xml.getMaxCounterEvents)
    events
  }

  def defaultXml() = {
    val xml = new Slave

    val stack = new Stack
    val app = new AppLayer
    app.setMaxFragSize(2048)
    app.setTimeoutMS(5000)
    stack.setAppLayer(app)

    val link = new LinkLayer
    link.setIsMaster(false)
    link.setUseConfirmations(false)
    link.setNumRetries(3)
    link.setLocalAddress(500)
    link.setRemoteAddress(100)
    link.setAckTimeoutMS(1000)
    stack.setLinkLayer(link)

    xml.setStack(stack)

    val slaveConfig = new SlaveConfig

    val unsol = new UnsolDefaults
    unsol.setDoClass1(true)
    unsol.setDoClass2(true)
    unsol.setDoClass3(true)
    unsol.setPackDelayMS(50)
    unsol.setRetryMS(5000)
    slaveConfig.setUnsolDefaults(unsol)

    val time = new TimeIINTask
    time.setDoTask(true)
    time.setPeriodMS(60000)

    slaveConfig.setTimeIINTask(time)
    slaveConfig.setMaxAnalogEvents(100)
    slaveConfig.setMaxBinaryEvents(100)
    slaveConfig.setMaxCounterEvents(100)

    slaveConfig.setOpenLayerDelay(1000)

    xml.setSlaveConfig(slaveConfig)

    val log = new Log
    log.setFilter(LogLevel.LOG_WARNING)
    xml.setLog(log)

    xml
  }
}