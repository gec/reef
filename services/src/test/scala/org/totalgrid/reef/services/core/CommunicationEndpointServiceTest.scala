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

import org.totalgrid.reef.client.exception.{ BadRequestException, ReefServiceException }

import org.totalgrid.reef.services._
import core.SubscriptionTools.AuthRequest
import org.totalgrid.reef.measurementstore.{ InMemoryMeasurementStore }

import com.google.protobuf.ByteString

import org.totalgrid.reef.client.service.proto.FEP._
import org.totalgrid.reef.client.service.proto.Model._

import org.totalgrid.reef.services.ServiceResponseTestingHelpers._
import org.totalgrid.reef.models.UUIDConversions._
import java.util.UUID

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.totalgrid.reef.client.proto.Envelope.Status
import org.totalgrid.reef.models.{ FrontEndPort, DatabaseUsingTestBase }
import org.totalgrid.reef.client.sapi.client.BasicRequestHeaders

import org.totalgrid.reef.client.proto.Envelope.SubscriptionEventType._
import org.totalgrid.reef.client.service.proto.Processing.MeasurementProcessingConnection
import org.totalgrid.reef.client.service.proto.Measurements.Measurement

@RunWith(classOf[JUnitRunner])
class CommunicationEndpointServiceTest extends DatabaseUsingTestBase {

  class Fixture extends SubscriptionTools.SubscriptionTesting {
    def _dbConnection = dbConnection
    val rtDb = new InMemoryMeasurementStore()
    val modelFac = new ModelFactories(rtDb, contextSource)

    val endpointService = new SyncService(new CommunicationEndpointService(modelFac.endpoints), contextSource)
    val connectionService = new SyncService(new CommunicationEndpointConnectionService(modelFac.fepConn), contextSource)

    val configFileService = new SyncService(new ConfigFileService(modelFac.configFiles), contextSource)
    val pointService = new SyncService(new PointService(modelFac.points), contextSource)
    val commandService = new SyncService(new CommandService(modelFac.cmds), contextSource)
    val portService = new SyncService(new FrontEndPortService(modelFac.fepPort), contextSource)

    def checkAuth(auth: AuthRequest) { this.popAuth should equal(List(auth)) }
    def checkAuth(auths: List[AuthRequest]) { this.popAuth should equal(auths) }
  }

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
    val f = new Fixture

    f.pointService.put(getPoint().build).expectOne()
    f.commandService.put(getCommand().build).expectOne()
    val cf = f.configFileService.put(getConfigFile().build).expectOne()
    val port = f.portService.put(getIPPort().build).expectOne()

    val endpoint = getEndpoint().setChannel(port).addConfigFiles(cf).setOwnerships(getOwnership())

    val returned = f.endpointService.put(endpoint.build).expectOne()

    returned.getConfigFilesCount should equal(1)
    returned.hasChannel should equal(true)

    returned.getChannel.getUuid should equal(port.getUuid)
    returned.getConfigFiles(0).getUuid should equal(cf.getUuid)

    returned.getOwnerships.getPointsCount should equal(1)
    returned.getOwnerships.getCommandsCount should equal(1)

    f.pointService.get(getPoint("*").build).expectOne()
    f.commandService.get(getCommand("*").build).expectOne()
  }

  test("Endpoint put with unkown points/commands fails") {
    val f = new Fixture
    intercept[BadRequestException] {
      val endpoint = getEndpoint().setChannel(getIPPort()).addConfigFiles(getConfigFile()).setOwnerships(getOwnership())
      f.endpointService.put(endpoint.build).expectOne()
    }
  }

  test("Add with no port") {
    val f = new Fixture
    f.pointService.put(getPoint().build).expectOne()
    f.commandService.put(getCommand().build).expectOne()
    val endpoint = getEndpoint().addConfigFiles(getConfigFile()).setOwnerships(getOwnership())

    val returned = f.endpointService.put(endpoint.build).expectOne()

    returned.hasChannel should equal(false)

    returned.getConfigFilesCount should equal(1)
    returned.getConfigFiles(0).hasUuid should equal(true)

    returned.getOwnerships.getPointsCount should equal(1)
    returned.getOwnerships.getCommandsCount should equal(1)
  }

  test("Config file without needed fields blows up") {
    val f = new Fixture
    intercept[BadRequestException] {
      f.configFileService.put(getConfigFile(Some("test1"), None, None).build)
    }
    intercept[BadRequestException] {
      f.configFileService.put(getConfigFile(Some("test1"), Some("data"), None).build)
    }
    intercept[BadRequestException] {
      f.configFileService.put(getConfigFile(Some("test1"), None, Some("data")).build)
    }
    intercept[BadRequestException] {
      f.configFileService.put(getConfigFile(None, Some("data"), Some("data")).build)
    }
    intercept[BadRequestException] {
      f.endpointService.put(getEndpoint().setChannel(getIPPort()).addConfigFiles(getConfigFile(None, None, None)).build)
    }
  }

  test("Shared Config file") {
    val f = new Fixture
    f.pointService.put(getPoint("d1.test_point").build).expectOne()
    f.pointService.put(getPoint("d2.test_point").build).expectOne()
    f.commandService.put(getCommand("d1.test_command").build).expectOne()
    f.commandService.put(getCommand("d2.test_command").build).expectOne()
    val endpoint1 = getEndpoint("d1").addConfigFiles(getConfigFile(Some("shared"))).setOwnerships(getOwnership("d1"))
    val endpoint2 = getEndpoint("d2").addConfigFiles(getConfigFile(Some("shared"))).setOwnerships(getOwnership("d2"))

    val returned1 = f.endpointService.put(endpoint1.build).expectOne()
    val returned2 = f.endpointService.put(endpoint2.build).expectOne()

    returned1.getConfigFilesCount should equal(1)
    returned2.getConfigFilesCount should equal(1)

    returned1.getConfigFiles(0).getUuid should equal(returned2.getConfigFiles(0).getUuid)
  }

  test("Can only delete disabled offline endpoints") {
    val f = new Fixture
    val point = f.pointService.put(getPoint().build).expectOne()
    val command = f.commandService.put(getCommand().build).expectOne()
    val configFile = f.configFileService.put(getConfigFile().build).expectOne()
    val port = f.portService.put(getIPPort().build).expectOne()

    val endpoint = f.endpointService.put(makeEndpoint(Some(port), Some(configFile))).expectOne()

    val initialConnectionState = f.connectionService.get(getConnection().build).expectOne()
    initialConnectionState.getState should equal(EndpointConnection.State.COMMS_DOWN)
    initialConnectionState.getEnabled should equal(true)

    // cannot delete enabled endpoints
    intercept[BadRequestException] { f.endpointService.delete(endpoint) }

    // cannot delete endpoint because it is "enabled" and "online", would confuse Feps
    f.connectionService.put(getConnection(state = Some(EndpointConnection.State.COMMS_UP)).build)
    intercept[BadRequestException] { f.endpointService.delete(endpoint) }

    // cannot delete endpoint because even though it has been disabled it is still "online"
    f.connectionService.put(getConnection(enabled = Some(false)).build)
    intercept[BadRequestException] { f.endpointService.delete(endpoint) }

    // we can now delete because endpoint is "disabled" and "offline"
    f.connectionService.put(getConnection(state = Some(EndpointConnection.State.COMMS_DOWN)).build)
    f.endpointService.delete(endpoint).expectOne(Status.DELETED)
  }

  test("Can't remove points or commands owned by endpoint") {
    val f = new Fixture

    val point = f.pointService.put(getPoint().build).expectOne()
    val command = f.commandService.put(getCommand().build).expectOne()
    val configFile = f.configFileService.put(getConfigFile().build).expectOne()
    val port = f.portService.put(getIPPort().build).expectOne()

    val endpoint = f.endpointService.put(makeEndpoint(Some(port), Some(configFile))).expectOne()

    // cannot deleted resources used by endpoint
    intercept[BadRequestException] { f.pointService.delete(point) }
    intercept[BadRequestException] { f.commandService.delete(command) }
    intercept[BadRequestException] { f.portService.delete(port) }

    //intercept[BadRequestException] { configFileService.delete(configFile) }

    // we can now delete because endpoint is "disabled" and "offline"
    f.connectionService.put(getConnection(enabled = Some(false)).build)

    // now remove endpoint "unlocking" other resources
    f.endpointService.delete(endpoint).expectOne(Status.DELETED)

    // now that the endpoint is deleted we can remove the other objects
    f.pointService.delete(point).expectOne(Status.DELETED)
    f.commandService.delete(command).expectOne(Status.DELETED)
    f.configFileService.delete(configFile).expectOne(Status.DELETED)
    f.portService.delete(port).expectOne(Status.DELETED)

    val eventList = List(
      (ADDED, classOf[Entity]), (ADDED, classOf[Point]), (ADDED, classOf[Measurement]),
      (ADDED, classOf[Entity]), (ADDED, classOf[Command]),
      (ADDED, classOf[Entity]), (ADDED, classOf[ConfigFile]),
      (ADDED, classOf[Entity]), (ADDED, classOf[CommChannel]),
      (ADDED, classOf[MeasurementProcessingConnection]), (ADDED, classOf[EndpointConnection]),
      (ADDED, classOf[Entity]), (ADDED, classOf[Endpoint]),
      (ADDED, classOf[EntityEdge]), (ADDED, classOf[EntityEdge]), (ADDED, classOf[EntityEdge]),
      (MODIFIED, classOf[EndpointConnection]),
      (REMOVED, classOf[MeasurementProcessingConnection]), (REMOVED, classOf[EndpointConnection]),
      (REMOVED, classOf[Endpoint]), (REMOVED, classOf[Entity]),
      (REMOVED, classOf[EntityEdge]), (REMOVED, classOf[EntityEdge]), (REMOVED, classOf[EntityEdge]),
      (REMOVED, classOf[Point]), (REMOVED, classOf[Measurement]), (REMOVED, classOf[Entity]),
      (REMOVED, classOf[Command]), (REMOVED, classOf[Entity]),
      (REMOVED, classOf[ConfigFile]), (REMOVED, classOf[Entity]),
      (REMOVED, classOf[CommChannel]), (REMOVED, classOf[Entity]))

    f.eventCheck should equal(eventList)
  }

  test("Add parts seperatley pre-configured uuids") {
    val f = new Fixture

    val pointUUID: ReefUUID = UUID.randomUUID
    val commandUUID: ReefUUID = UUID.randomUUID
    val configFileUUID: ReefUUID = UUID.randomUUID

    f.pointService.put(getPoint().setUuid(pointUUID).build).expectOne()
    f.commandService.put(getCommand().setUuid(commandUUID).build).expectOne()
    val cf = f.configFileService.put(getConfigFile().setUuid(configFileUUID).build).expectOne()
    val port = f.portService.put(getIPPort().build).expectOne()

    val endpoint = getEndpoint().setChannel(port).addConfigFiles(cf).setOwnerships(getOwnership())

    f.endpointService.put(endpoint.build).expectOne()

    f.pointService.get(getPoint("*").build).expectOne().getUuid should equal(pointUUID)
    f.commandService.get(getCommand("*").build).expectOne().getUuid should equal(commandUUID)

    f.configFileService.get(getConfigFile(Some("*")).build).expectOne().getUuid should equal(configFileUUID)
  }

  test("Add Sink Endpoint") {
    val f = new Fixture

    f.pointService.put(getPoint().build).expectOne()
    f.commandService.put(getCommand().build).expectOne()

    val endpoint = getSinkEndpoint().setOwnerships(getOwnership())

    val returned = f.endpointService.put(endpoint.build).expectOne()

    returned.getOwnerships.getPointsCount should equal(1)
    returned.getOwnerships.getCommandsCount should equal(1)
  }

  test("Endpoint auth requests") {
    val f = new Fixture
    val point = f.pointService.put(getPoint().build).expectOne()
    val command = f.commandService.put(getCommand().build).expectOne()
    val configFile = f.configFileService.put(getConfigFile().build).expectOne()
    val port = f.portService.put(getIPPort().build).expectOne()

    f.popAuth // don't care about setup objects

    val endpoint = f.endpointService.put(makeEndpoint(Some(port), Some(configFile))).expectOne()
    f.checkAuth(AuthRequest("endpoint", "create", List("device")))

    val initialConnectionState = f.connectionService.get(getConnection().build).expectOne()
    initialConnectionState.getState should equal(EndpointConnection.State.COMMS_DOWN)
    initialConnectionState.getEnabled should equal(true)
    f.checkAuth(AuthRequest("endpoint_connection", "read", List("device")))

    f.connectionService.put(getConnection(state = Some(EndpointConnection.State.COMMS_UP)).build)
    f.checkAuth(AuthRequest("endpoint_state", "update", List("device")))

    f.connectionService.put(getConnection(enabled = Some(false)).build)
    f.checkAuth(AuthRequest("endpoint_enabled", "update", List("device")))

    f.connectionService.put(getConnection(state = Some(EndpointConnection.State.COMMS_DOWN)).build)
    f.checkAuth(AuthRequest("endpoint_state", "update", List("device")))

    f.endpointService.delete(endpoint).expectOne(Status.DELETED)
    f.checkAuth(AuthRequest("endpoint", "deleted", List("device")))

    println(f.popAuth)
  }

}
