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

import org.totalgrid.reef.protocol.dnp3.xml._

import scala.collection.JavaConversions._
import org.totalgrid.reef.proto.Model.ConfigFile
import org.totalgrid.reef.protocol.api.Protocol
import org.totalgrid.reef.util.XMLHelper

/**
 * Converts xml for the master stack to the dnp3 configuration objects
 */
object MasterXmlConfig {

  def getMasterConfig(files: List[ConfigFile]): (MasterStackConfig, FilterLevel) = {
    val configFile = Protocol.find(files, "text/xml") //there is should be only one XML file
    val xml = XMLHelper.read(configFile.getFile.toByteArray, classOf[Master])
    (loadConfig(xml), XmlToProtoTranslations.filterLevel(xml.getLog))
  }

  private def loadConfig(xml: Master): MasterStackConfig = {
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
}