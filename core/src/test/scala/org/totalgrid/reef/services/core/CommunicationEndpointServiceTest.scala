/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the GNU Affero General Public License
 * Version 3.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.services.core

import org.totalgrid.reef.japi.{ BadRequestException, ReefServiceException }

import org.totalgrid.reef.services._
import org.totalgrid.reef.measurementstore.{ InMemoryMeasurementStore }

import com.google.protobuf.ByteString

import org.totalgrid.reef.proto.FEP._
import org.totalgrid.reef.proto.Model._

import org.totalgrid.reef.services.ServiceResponseTestingHelpers._
import org.totalgrid.reef.messaging.serviceprovider.SilentEventPublishers

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.totalgrid.reef.models.DatabaseUsingTestBase

@RunWith(classOf[JUnitRunner])
class CommunicationEndpointServiceTest extends DatabaseUsingTestBase {

  val pubs = new SilentEventPublishers
  val rtDb = new InMemoryMeasurementStore()
  val modelFac = new core.ModelFactories(ServiceDependencies(pubs, new SilentSummaryPoints, rtDb))

  val endpointService = new CommunicationEndpointService(modelFac.endpoints)

  val configFileService = new ConfigFileService(modelFac.configFiles)
  val pointService = new PointService(modelFac.points)
  val commandService = new CommandService(modelFac.cmds)
  val portService = new FrontEndPortService(modelFac.fepPort)

  def getEndpoint(name: String = "device", protocol: String = "benchmark") = {
    CommEndpointConfig.newBuilder().setProtocol(protocol).setName(name)
  }
  def getIPPort(name: String = "device") = {
    CommChannel.newBuilder.setName(name + "-port").setIp(IpPort.newBuilder.setNetwork("any").setAddress("localhost").setPort(1200))
  }
  def getSerialPort(name: String = "device") = {
    CommChannel.newBuilder.setName(name + "-serial").setSerial(SerialPort.newBuilder.setLocation("any").setPortName("COM1"))
  }
  def getOwnership(name: String = "device", pointNames: List[String] = List("test_point"), controlNames: List[String] = List("test_command")) = {
    val owners = EndpointOwnership.newBuilder
    pointNames.foreach(pname => owners.addPoints(name + "." + pname))
    controlNames.foreach(pname => owners.addCommands(name + "." + pname))
    owners
  }
  def getPoint(name: String = "device.test_point") = {
    Point.newBuilder.setName(name).setUnit("raw").setType(PointType.ANALOG)
  }
  def getCommand(name: String = "device.test_command") = {
    Command.newBuilder.setName(name).setType(CommandType.CONTROL).setDisplayName(name)
  }
  def getConfigFile(name: Option[String] = Some("test1"), text: Option[String] = Some("Something"), mimeType: Option[String] = Some("text/xml")) = {
    val cf = ConfigFile.newBuilder
    name.foreach(nm => cf.setName(nm))
    text.foreach(text => cf.setFile(ByteString.copyFromUtf8(text)))
    mimeType.foreach(typ => cf.setMimeType(typ))
    cf
  }

  test("Add parts seperatley (uid)") {

    pointService.put(getPoint().build).expectOne()
    commandService.put(getCommand().build).expectOne()
    val cf = configFileService.put(getConfigFile().build).expectOne()
    val port = portService.put(getIPPort().build).expectOne()

    val endpoint = getEndpoint().setChannel(port).addConfigFiles(cf).setOwnerships(getOwnership())

    val returned = endpointService.put(endpoint.build).expectOne()

    returned.getConfigFilesCount should equal(1)
    returned.hasChannel should equal(true)

    returned.getChannel.getUuid should equal(port.getUuid)
    returned.getConfigFiles(0).getUuid should equal(cf.getUuid)

    returned.getOwnerships.getPointsCount should equal(1)
    returned.getOwnerships.getCommandsCount should equal(1)

    pointService.get(getPoint("*").build).expectOne()
    commandService.get(getCommand("*").build).expectOne()
  }

  test("Endpoint put with unkown points/commands fails") {
    intercept[BadRequestException] {
      val endpoint = getEndpoint().setChannel(getIPPort()).addConfigFiles(getConfigFile()).setOwnerships(getOwnership())
      endpointService.put(endpoint.build).expectOne()
    }
  }

  test("Add with no port") {
    pointService.put(getPoint().build).expectOne()
    commandService.put(getCommand().build).expectOne()
    val endpoint = getEndpoint().addConfigFiles(getConfigFile()).setOwnerships(getOwnership())

    val returned = endpointService.put(endpoint.build).expectOne()

    returned.hasChannel should equal(false)

    returned.getConfigFilesCount should equal(1)
    returned.getConfigFiles(0).hasUuid should equal(true)

    returned.getOwnerships.getPointsCount should equal(1)
    returned.getOwnerships.getCommandsCount should equal(1)
  }

  test("Config file without needed fields blows up") {
    intercept[ReefServiceException] {
      configFileService.put(getConfigFile(Some("test1"), None, None).build)
    }
    intercept[ReefServiceException] {
      configFileService.put(getConfigFile(Some("test1"), Some("data"), None).build)
    }
    intercept[ReefServiceException] {
      configFileService.put(getConfigFile(Some("test1"), None, Some("data")).build)
    }
    intercept[ReefServiceException] {
      configFileService.put(getConfigFile(None, Some("data"), Some("data")).build)
    }
    intercept[ReefServiceException] {
      endpointService.put(getEndpoint().setChannel(getIPPort()).addConfigFiles(getConfigFile(None, None, None)).build)
    }
  }

  test("Shared Config file") {
    pointService.put(getPoint("d1.test_point").build).expectOne()
    pointService.put(getPoint("d2.test_point").build).expectOne()
    commandService.put(getCommand("d1.test_command").build).expectOne()
    commandService.put(getCommand("d2.test_command").build).expectOne()
    val endpoint1 = getEndpoint("d1").addConfigFiles(getConfigFile(Some("shared"))).setOwnerships(getOwnership("d1"))
    val endpoint2 = getEndpoint("d2").addConfigFiles(getConfigFile(Some("shared"))).setOwnerships(getOwnership("d2"))

    val returned1 = endpointService.put(endpoint1.build).expectOne()
    val returned2 = endpointService.put(endpoint2.build).expectOne()

    returned1.getConfigFilesCount should equal(1)
    returned2.getConfigFilesCount should equal(1)

    returned1.getConfigFiles(0).getUuid should equal(returned2.getConfigFiles(0).getUuid)
  }

  test("Endpoint with no ownerships blows up") {
    intercept[ReefServiceException] {
      endpointService.put(getEndpoint().build)
    }
  }
}
