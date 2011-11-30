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

import org.totalgrid.reef.client.exceptions.{ BadRequestException, ReefServiceException }

import org.totalgrid.reef.services._
import org.totalgrid.reef.measurementstore.{ InMemoryMeasurementStore }

import com.google.protobuf.ByteString

import org.totalgrid.reef.proto.FEP._
import org.totalgrid.reef.proto.Model._

import org.totalgrid.reef.services.ServiceResponseTestingHelpers._
import org.totalgrid.reef.services.core.util.UUIDConversions._
import java.util.UUID

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.totalgrid.reef.client.proto.Envelope.Status
import org.totalgrid.reef.models.{ FrontEndPort, DatabaseUsingTestBase }
import org.totalgrid.reef.client.sapi.client.BasicRequestHeaders

import org.totalgrid.reef.services.core.SyncServiceShims._

@RunWith(classOf[JUnitRunner])
class CommunicationEndpointServiceTest extends DatabaseUsingTestBase {

  val rtDb = new InMemoryMeasurementStore()
  val modelFac = new ModelFactories(new ServiceDependenciesDefaults(cm = rtDb))

  val endpointService = new CommunicationEndpointService(modelFac.endpoints)
  val connectionService = new CommunicationEndpointConnectionService(modelFac.fepConn)

  val configFileService = new ConfigFileService(modelFac.configFiles)
  val pointService = new PointService(modelFac.points)
  val commandService = new CommandService(modelFac.cmds)
  val portService = new FrontEndPortService(modelFac.fepPort)

  val headers = BasicRequestHeaders.empty.setUserName("user")

  def getEndpoint(name: String = "device", protocol: String = "benchmark") = {
    Endpoint.newBuilder().setProtocol(protocol).setName(name)
  }
  def getSinkEndpoint(name: String = "device", protocol: String = "benchmark") = {
    Endpoint.newBuilder().setProtocol(protocol).setName(name).setDataSource(false)
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
  def getConnection(name: String = "device", enabled: Option[Boolean] = None, state: Option[EndpointConnection.State] = None) = {
    val b = EndpointConnection.newBuilder.setEndpoint(getEndpoint(name))
    enabled.foreach(b.setEnabled(_))
    state.foreach(b.setState(_))
    b
  }

  def makeEndpoint(port: Option[CommChannel] = Some(getIPPort().build),
    configFile: Option[ConfigFile] = Some(getConfigFile().build),
    ownerships: EndpointOwnership = getOwnership().build) = {
    val b = getEndpoint()
    port.foreach(b.setChannel(_))
    configFile.foreach(b.addConfigFiles(_))
    b.setOwnerships(ownerships)
    b.build
  }

  test("Add parts seperatley (id)") {

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
    intercept[BadRequestException] {
      configFileService.put(getConfigFile(Some("test1"), None, None).build)
    }
    intercept[BadRequestException] {
      configFileService.put(getConfigFile(Some("test1"), Some("data"), None).build)
    }
    intercept[BadRequestException] {
      configFileService.put(getConfigFile(Some("test1"), None, Some("data")).build)
    }
    intercept[BadRequestException] {
      configFileService.put(getConfigFile(None, Some("data"), Some("data")).build)
    }
    intercept[BadRequestException] {
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

  test("Can only delete disabled offline endpoints") {
    val point = pointService.put(getPoint().build).expectOne()
    val command = commandService.put(getCommand().build).expectOne()
    val configFile = configFileService.put(getConfigFile().build).expectOne()
    val port = portService.put(getIPPort().build).expectOne()

    val endpoint = endpointService.put(makeEndpoint(Some(port), Some(configFile))).expectOne()

    val initialConnectionState = connectionService.get(getConnection().build).expectOne()
    initialConnectionState.getState should equal(EndpointConnection.State.COMMS_DOWN)
    initialConnectionState.getEnabled should equal(true)

    // cannot delete enabled endpoints
    intercept[BadRequestException] { endpointService.delete(endpoint) }

    // cannot delete endpoint because it is "enabled" and "online", would confuse Feps
    connectionService.put(getConnection(state = Some(EndpointConnection.State.COMMS_UP)).build, headers)
    intercept[BadRequestException] { endpointService.delete(endpoint) }

    // cannot delete endpoint because even though it has been disabled it is still "online"
    connectionService.put(getConnection(enabled = Some(false)).build, headers)
    intercept[BadRequestException] { endpointService.delete(endpoint) }

    // we can now delete because endpoint is "disabled" and "offline"
    connectionService.put(getConnection(state = Some(EndpointConnection.State.COMMS_DOWN)).build, headers)
    endpointService.delete(endpoint).expectOne(Status.DELETED)
  }

  test("Can't remove points or commands owned by endpoint") {
    val point = pointService.put(getPoint().build).expectOne()
    val command = commandService.put(getCommand().build).expectOne()
    val configFile = configFileService.put(getConfigFile().build).expectOne()
    val port = portService.put(getIPPort().build).expectOne()

    val endpoint = endpointService.put(makeEndpoint(Some(port), Some(configFile))).expectOne()

    // cannot deleted resources used by endpoint
    intercept[BadRequestException] { pointService.delete(point) }
    intercept[BadRequestException] { commandService.delete(command) }
    intercept[BadRequestException] { portService.delete(port) }

    //intercept[BadRequestException] { configFileService.delete(configFile) }

    // we can now delete because endpoint is "disabled" and "offline"
    connectionService.put(getConnection(enabled = Some(false)).build, headers)

    // now remove endpoint "unlocking" other resources
    endpointService.delete(endpoint).expectOne(Status.DELETED)

    // now that the endpoint is deleted we can remove the other objects
    pointService.delete(point).expectOne(Status.DELETED)
    commandService.delete(command).expectOne(Status.DELETED)
    configFileService.delete(configFile).expectOne(Status.DELETED)
    portService.delete(port).expectOne(Status.DELETED)
  }

  test("Add parts seperatley pre-configured uuids") {

    val pointUUID: ReefUUID = UUID.randomUUID
    val commandUUID: ReefUUID = UUID.randomUUID
    val configFileUUID: ReefUUID = UUID.randomUUID

    pointService.put(getPoint().setUuid(pointUUID).build).expectOne()
    commandService.put(getCommand().setUuid(commandUUID).build).expectOne()
    val cf = configFileService.put(getConfigFile().setUuid(configFileUUID).build).expectOne()
    val port = portService.put(getIPPort().build).expectOne()

    val endpoint = getEndpoint().setChannel(port).addConfigFiles(cf).setOwnerships(getOwnership())

    endpointService.put(endpoint.build).expectOne()

    pointService.get(getPoint("*").build).expectOne().getUuid should equal(pointUUID)
    commandService.get(getCommand("*").build).expectOne().getUuid should equal(commandUUID)

    configFileService.get(getConfigFile(Some("*")).build).expectOne().getUuid should equal(configFileUUID)
  }

  test("Add Sink Endpoint") {

    pointService.put(getPoint().build).expectOne()
    commandService.put(getCommand().build).expectOne()

    val endpoint = getSinkEndpoint().setOwnerships(getOwnership())

    val returned = endpointService.put(endpoint.build).expectOne()

    returned.getOwnerships.getPointsCount should equal(1)
    returned.getOwnerships.getCommandsCount should equal(1)
  }
}
