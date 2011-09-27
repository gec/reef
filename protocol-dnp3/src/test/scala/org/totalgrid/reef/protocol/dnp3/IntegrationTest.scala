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

import mock.InstantCommandResponder
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import org.totalgrid.reef.proto.FEP.IpPort
import org.totalgrid.reef.protocol.dnp3.xml.{ LinkLayer, AppLayer, Stack, Master }
import com.google.protobuf.ByteString
import org.totalgrid.reef.proto.{ Model, FEP }

import org.totalgrid.reef.proto.Measurements.MeasurementBatch
import org.totalgrid.reef.util.{ Logging, EmptySyncVar, XMLHelper }
import org.totalgrid.reef.protocol.api.{ CommandHandler, Publisher }
import org.totalgrid.reef.promise.{ FixedPromise, Promise }
import org.scalatest.{ BeforeAndAfterAll, FunSuite }
import org.totalgrid.reef.proto.Commands.{ CommandStatus => CommandStatusProto, CommandRequest => CommandRequestProto, CommandResponse => CommandResponseProto }

@RunWith(classOf[JUnitRunner])
class IntegrationTest extends FunSuite with ShouldMatchers with BeforeAndAfterAll with Logging {

  val slave = new StackManager
  val commandAcceptor = new InstantCommandResponder(CommandStatus.CS_SUCCESS)
  val numSlaves = 1
  val portStart = 50000
  val portEnd = portStart + numSlaves - 1

  final override def beforeAll() {
    logger.info("starting slave")
    val config = new SlaveStackConfig
    config.setDevice(new DeviceTemplate(10, 10, 10, 10, 10, 10, 10))
    val s = new PhysLayerSettings(FilterLevel.LEV_WARNING, 1000)
    val dataSinks = (portStart to portEnd).map { port =>
      val server = "server-" + port
      slave.AddTCPServer(server, s, "0.0.0.0", port)
      slave.AddSlave(server, server, FilterLevel.LEV_WARNING, commandAcceptor, config)
    }
  }

  final override def afterAll() {
    logger.info("stopping slave")
    slave.Shutdown()
  }

  test("Endpoints online/offline") {

    val configFiles = makeMappingFile(10, 10, 10, 10, 10, 10, 10) :: makeConfigFile() :: Nil

    val protocol = new Dnp3Protocol
    val listeners = (portStart to portEnd).map { port =>
      val channelName = "port" + port

      val endpointListener = new LastValueListener[FEP.CommEndpointConnection.State]
      val measListener = new LastValueListener[MeasurementBatch](false)
      val portListener = new LastValueListener[FEP.CommChannel.State]

      protocol.addChannel(getClient(port, channelName), portListener)

      val commandAdapter = protocol.addEndpoint("endpoint" + port, channelName, configFiles, measListener, endpointListener)

      (portListener, endpointListener, measListener, commandAdapter)
    }

    listeners.foreach {
      case (portListener, endpointListener, measListener, commandAdapter) =>

        portListener.lastValue.waitUntil(FEP.CommChannel.State.OPEN)
        endpointListener.lastValue.waitUntil(FEP.CommEndpointConnection.State.COMMS_UP)
        measListener.lastValue.waitFor(m => m.getMeasCount > 0)

        issueAndWaitForCommandResponse(commandAdapter, makeControl("control0", "00"))
    }

    (portStart to portEnd).map { port =>
      val channelName = "port" + port

      protocol.removeEndpoint("endpoint" + port)
      protocol.removeChannel(channelName)
    }

    listeners.foreach {
      case (portListener, endpointListener, measListener, commandAdapter) =>
        endpointListener.lastValue.waitUntil(FEP.CommEndpointConnection.State.COMMS_DOWN)
        portListener.lastValue.waitUntil(FEP.CommChannel.State.CLOSED)
    }

  }

  class LastValueListener[A](verbose: Boolean = true) extends Publisher[A] {
    val lastValue = new EmptySyncVar[A]
    def publish(proto: A): Promise[Boolean] = {
      if (verbose) logger.info(proto.toString)
      lastValue.update(proto)
      new FixedPromise(true)
    }
  }

  def issueAndWaitForCommandResponse(cmdAcceptor: CommandHandler, commandRequest: CommandRequestProto) {
    val response = new EmptySyncVar[CommandStatusProto]
    val rspHandler = new Publisher[CommandResponseProto] {
      def publish(proto: CommandResponseProto): Promise[Boolean] = {
        response.update(proto.getStatus)
        new FixedPromise(true)
      }
    }
    cmdAcceptor.issue(commandRequest, rspHandler)
    response.waitUntil(CommandStatusProto.SUCCESS)
  }

  private def makeControl(name: String, id: String) = {
    CommandRequestProto.newBuilder().setName(name).setType(CommandRequestProto.ValType.NONE).setCorrelationId(id).build
  }

  private def getClient(port: Int, name: String) = {
    FEP.CommChannel.newBuilder().setName(name).setIp(FEP.IpPort.newBuilder.setAddress("127.0.0.1")
      .setMode(IpPort.Mode.CLIENT).setPort(port).setNetwork("any")).build
  }

  private def makeMappingFile(numBinary: Int, numAnalog: Int, numCounter: Int, numControlStatus: Int, numSetpointStatus: Int, numControl: Int, numSetpoint: Int) = {
    import org.totalgrid.reef.proto.Mapping._

    val index = IndexMapping.newBuilder

    def add(i: Int, n: String, t: DataType) {
      index.addMeasmap(MeasMap.newBuilder.setIndex(i).setPointName(n + i).setType(t).setUnit("raw"))
    }
    def addC(i: Int, n: String, t: CommandType) {
      index.addCommandmap(CommandMap.newBuilder.setCommandName(n + i).setType(t).setIndex(i))
    }

    (0 to numBinary).foreach(i => add(i, "binary", DataType.BINARY))
    (0 to numAnalog).foreach(i => add(i, "analog", DataType.ANALOG))
    (0 to numCounter).foreach(i => add(i, "counter", DataType.COUNTER))
    (0 to numControlStatus).foreach(i => add(i, "contolStatus", DataType.CONTROL_STATUS))
    (0 to numSetpointStatus).foreach(i => add(i, "setpointStatus", DataType.SETPOINT_STATUS))

    (0 to numControl).foreach(i => addC(i, "control", CommandType.LATCH_ON))
    (0 to numSetpoint).foreach(i => addC(i, "setpoint", CommandType.SETPOINT))

    val indexProto = index.build

    Model.ConfigFile.newBuilder().setName("mapping.pi")
      .setMimeType("application/vnd.google.protobuf; proto=reef.proto.Mapping.IndexMapping")
      .setFile(indexProto.toByteString).build
  }

  private def makeConfigFile() = {
    val data = XMLHelper.writeToString(getMasterSettings(), classOf[Master])
    Model.ConfigFile.newBuilder().setName("master.xml").setMimeType("text/xml").setFile(ByteString.copyFrom(data, "UTF-8")).build
  }

  private def getMasterSettings(): Master = {
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
    //    val scan = new Master.ScanList.ExceptionScan
    //    scan.setClass1(true); scan.setClass2(true); scan.setClass3(true);
    //    scan.setPeriodMS(5000)
    //    list.getExceptionScan.add(scan)
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
    link.setLocalAddress(1)
    link.setRemoteAddress(1024)
    link.setAckTimeoutMS(1000)
    stack.setLinkLayer(link)

    xml.setStack(stack)

    xml
  }
}