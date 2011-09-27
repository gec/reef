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

import org.scalatest.{ BeforeAndAfterAll, FunSuite }
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import scala.collection.JavaConversions._

import org.totalgrid.reef.protocol.dnp3.xml.{ Master, Stack, AppLayer, LinkLayer }
import java.io.{ FileWriter, File }
import org.totalgrid.reef.util.XMLHelper

@RunWith(classOf[JUnitRunner])
class XmlBindingTests extends FunSuite with ShouldMatchers with BeforeAndAfterAll {

  val fname = "master.xml.tmp"

  override def afterAll() {
    val f = new File(fname)
    if (f.exists()) if (!f.delete) throw new Exception("couldn't delete file")
  }

  /// This test shows that the startup/teardown behavior is working without crashing
  test("XMLSerialization") {
    val xml = new Master

    val settings = new Master.MasterSettings
    settings.setAllowTimeSync(true)
    settings.setTaskRetryMS(5000)
    settings.setIntegrityPeriodMS(60000)
    xml.setMasterSettings(settings)

    val unsol = new Master.Unsol
    unsol.setDoTask(true)
    unsol.setEnable(true)
    unsol.setClass1(true)
    unsol.setClass2(true)
    unsol.setClass3(true)
    xml.setUnsol(unsol)

    val list = new Master.ScanList
    val scan = new Master.ScanList.ExceptionScan
    scan.setClass1(true); scan.setClass2(true); scan.setClass3(true);
    scan.setPeriodMS(5000)
    list.getExceptionScan.add(scan)
    xml.setScanList(list)

    val stack = new Stack
    val app = new AppLayer
    app.setMaxFragSize(2048)
    app.setTimeoutMS(5000)
    stack.setAppLayer(app)

    val link = new LinkLayer
    link.setIsMaster(true)
    link.setUseConfirmations(true)
    link.setNumRetries(3)
    link.setLocalAddress(500)
    link.setRemoteAddress(100)
    link.setAckTimeoutMS(1000)
    stack.setLinkLayer(link)

    xml.setStack(stack)

    //now that we're fully formed, let's generate
    val fw = new FileWriter(fname)
    XMLHelper.writeToFile(xml, classOf[Master], fw)
    fw.close
  }

  test("XMLDeserialization") {
    val master: Master = XMLHelper.read(new File(fname), classOf[Master])

    //selectively read some parts of the above configuration
    master.getMasterSettings.getIntegrityPeriodMS should equal(60000)
    assert(master.getUnsol.isClass3)

    val x = master.getScanList.getExceptionScan

    master.getScanList.getExceptionScan.size should equal(1)
    master.getScanList.getExceptionScan.apply(0).getPeriodMS should equal(5000)

    master.getStack.getLinkLayer.getRemoteAddress should equal(100)
    master.getStack.getAppLayer.getMaxFragSize should equal(2048)
  }

}
