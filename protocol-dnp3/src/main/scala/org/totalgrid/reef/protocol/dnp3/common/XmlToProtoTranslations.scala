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

import org.totalgrid.reef.client.service.proto.FEP
import org.totalgrid.reef.protocol.dnp3.xml.{ LogLevel, Log }
import org.totalgrid.reef.protocol.dnp3._

/**
 * Converts common dnp3 xml settings to their proto or dnp3 equivilants
 */
object XmlToProtoTranslations {

  def filterLevel(log: Log) = {
    Option(log).map { l => translateFilterLevel(l.getFilter) }.getOrElse(FilterLevel.LEV_WARNING)
  }

  private def translateFilterLevel(xmlLevel: LogLevel): FilterLevel = {
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

  def getSerialSettings(channel: FEP.SerialPort): SerialSettings = {
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