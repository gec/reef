/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Green Energy Corp licenses this file
 * to you under the GNU Affero General Public License Version 3.0
 * (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.services.core

import org.totalgrid.reef.api.ReefServiceException

import org.squeryl.PrimitiveTypeMode._

import org.totalgrid.reef.models.ApplicationSchema
import org.totalgrid.reef.persistence.squeryl.{ DbConnector, DbInfo }
import org.totalgrid.reef.services._
import org.totalgrid.reef.measurementstore.{ InMemoryMeasurementStore }

import com.google.protobuf.ByteString

import org.totalgrid.reef.proto.FEP._
import org.totalgrid.reef.proto.Model._

import org.totalgrid.reef.services.ServiceResponseTestingHelpers._

import org.scalatest.{ FunSuite, BeforeAndAfterAll, BeforeAndAfterEach }
import org.scalatest.matchers.ShouldMatchers
import org.totalgrid.reef.messaging.serviceprovider.SilentEventPublishers

class CommunicationEndpointServiceTest extends FunSuite with ShouldMatchers with BeforeAndAfterAll with BeforeAndAfterEach {
  override def beforeAll() {
    DbConnector.connect(DbInfo.loadInfo("test"))
  }
  override def beforeEach() {
    transaction { ApplicationSchema.reset }
  }

  val pubs = new SilentEventPublishers
  val rtDb = new InMemoryMeasurementStore()
  val modelFac = new core.ModelFactories(pubs, new SilentSummaryPoints, rtDb)

  val endpointService = new CommunicationEndpointService(modelFac.endpoints)

  val configFileService = new ConfigFileService(modelFac.configFiles)
  val pointService = new PointService(modelFac.points)
  val commandService = new CommandService(modelFac.cmds)
  val portService = new FrontEndPortService(modelFac.fepPort)

  def getEndpoint(name: String = "device", protocol: String = "benchmark") = {
    CommunicationEndpointConfig.newBuilder().setProtocol(protocol).setName(name)
  }
  def getIPPort(name: String = "device") = {
    Port.newBuilder.setName(name + "-port").setIp(IpPort.newBuilder.setNetwork("any").setAddress("localhost").setPort(1200))
  }
  def getSerialPort(name: String = "device") = {
    Port.newBuilder.setName(name + "-serial").setSerial(SerialPort.newBuilder.setLocation("any").setPortName("COM1"))
  }
  def getOwnership(name: String = "device", pointNames: List[String] = List("test_point"), controlNames: List[String] = List("test_command")) = {
    val owners = EndpointOwnership.newBuilder
    pointNames.foreach(pname => owners.addPoints(name + "." + pname))
    controlNames.foreach(pname => owners.addCommands(name + "." + pname))
    owners
  }
  def getPoint(name: String = "device.test_point") = {
    Point.newBuilder.setName(name)
  }
  def getCommand(name: String = "device.test_command") = {
    Command.newBuilder.setName(name)
  }
  def getConfigFile(name: Option[String] = Some("test1"), text: Option[String] = Some("Something"), mimeType: Option[String] = Some("text/xml")) = {
    val cf = ConfigFile.newBuilder
    name.foreach(nm => cf.setName(nm))
    text.foreach(text => cf.setFile(ByteString.copyFromUtf8(text)))
    mimeType.foreach(typ => cf.setMimeType(typ))
    cf
  }

  test("Add parts seperatley (uid)") {

    val pt = one(pointService.put(getPoint().build))
    val cmd = one(commandService.put(getCommand().build))
    val cf = one(configFileService.put(getConfigFile().build))
    val port = one(portService.put(getIPPort().build))

    val endpoint = getEndpoint().setPort(port).addConfigFiles(cf).setOwnerships(getOwnership())

    val returned = one(endpointService.put(endpoint.build))

    returned.getConfigFilesCount should equal(1)
    returned.hasPort should equal(true)

    returned.getPort.getUid should equal(port.getUid)
    returned.getConfigFiles(0).getUid should equal(cf.getUid)

    returned.getOwnerships.getPointsCount should equal(1)
    returned.getOwnerships.getCommandsCount should equal(1)

    one(pointService.get(getPoint("*").build))
    one(commandService.get(getCommand("*").build))
  }

  test("Endpoint put adds all needed entries") {
    val endpoint = getEndpoint().setPort(getIPPort()).addConfigFiles(getConfigFile()).setOwnerships(getOwnership())

    val returned = one(endpointService.put(endpoint.build))

    returned.getConfigFilesCount should equal(1)
    returned.hasPort should equal(true)

    returned.getPort.hasUid should equal(true)
    returned.getConfigFiles(0).hasUid should equal(true)

    returned.getOwnerships.getPointsCount should equal(1)
    returned.getOwnerships.getCommandsCount should equal(1)

    one(pointService.get(getPoint("*").build))
    one(commandService.get(getCommand("*").build))
  }

  test("Add with no port") {
    val endpoint = getEndpoint().addConfigFiles(getConfigFile()).setOwnerships(getOwnership())

    val returned = one(endpointService.put(endpoint.build))

    returned.hasPort should equal(false)

    returned.getConfigFilesCount should equal(1)
    returned.getConfigFiles(0).hasUid should equal(true)

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
      endpointService.put(getEndpoint().setPort(getIPPort()).addConfigFiles(getConfigFile(None, None, None)).build)
    }
  }

  test("Endpoint with no ownerships blows up") {
    intercept[ReefServiceException] {
      endpointService.put(getEndpoint().build)
    }
  }
}
