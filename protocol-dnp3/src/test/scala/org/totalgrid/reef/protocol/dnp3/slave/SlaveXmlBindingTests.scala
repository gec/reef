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

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.{ BeforeAndAfterAll, FunSuite }
import org.totalgrid.reef.protocol.dnp3.xml.Slave
import org.totalgrid.reef.util.XMLHelper
import java.io.{ File }
import org.totalgrid.reef.protocol.dnp3.{ GrpVar, FilterLevel, DNPTestHelpers }

@RunWith(classOf[JUnitRunner])
class SlaveXmlBindingTests extends FunSuite with ShouldMatchers with BeforeAndAfterAll {

  test("Load in file") {
    val file = new File(getClass.getResource("/sample-slave-config.xml").getPath)
    val xml = XMLHelper.read(file, classOf[Slave])
    val mapping = DNPTestHelpers.makeMappingProto(1, 2, 3, 4, 5, 6, 7)

    val (slaveConfig, filterLevel) = SlaveXmlConfig.createSlaveConfig(xml, mapping)

    filterLevel should equal(FilterLevel.LEV_WARNING)

    slaveConfig.getDevice.getMBinary.size should equal(1)
    slaveConfig.getDevice.getMAnalog.size should equal(2)
    slaveConfig.getDevice.getMCounter.size should equal(3)
    slaveConfig.getDevice.getMControlStatus.size should equal(4)
    slaveConfig.getDevice.getMSetpointStatus.size should equal(5)
    slaveConfig.getDevice.getMControls.size should equal(6)
    slaveConfig.getDevice.getMSetpoints.size should equal(7)
  }

  test("Overriden response types") {
    val file = new File(getClass.getResource("/sample-slave-config-no-floats.xml").getPath)
    val xml = XMLHelper.read(file, classOf[Slave])
    val mapping = DNPTestHelpers.makeMappingProto(1, 0, 0, 0, 0, 0, 0)

    val (slaveConfig, filterLevel) = SlaveXmlConfig.createSlaveConfig(xml, mapping)

    val slave = slaveConfig.getSlave

    def toGrpVar(grpVar: GrpVar) = (grpVar.getGrp, grpVar.getVar)

    toGrpVar(slave.getMStaticAnalog) should equal((30, 2))
    toGrpVar(slave.getMEventAnalog) should equal((32, 2))
  }
}