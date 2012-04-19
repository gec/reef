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

import org.totalgrid.reef.client.service.proto.Measurements._
import org.totalgrid.reef.client.service.proto.FEP._
import org.totalgrid.reef.client.service.proto.Processing._
import org.totalgrid.reef.client.service.proto.Model._
import org.totalgrid.reef.client.service.proto.Application._

import org.totalgrid.reef.services.ServiceResponseTestingHelpers._

import collection.JavaConversions._

import org.totalgrid.reef.client.service.proto.OptionalProtos._

import org.totalgrid.reef.measurementstore.{ MeasSink, InMemoryMeasurementStore }
import com.weiglewilczek.slf4s.Logging
import org.totalgrid.reef.util.SyncVar
import org.totalgrid.reef.client.proto.Envelope
import org.totalgrid.reef.client.sapi._
import org.totalgrid.reef.models.{ RunTestsInsideTransaction, DatabaseUsingTestNotTransactionSafe }
import org.totalgrid.reef.event.SystemEventSink
import org.totalgrid.reef.measproc.{ MeasBatchProcessor, AddressableMeasurementBatchService }
import org.totalgrid.reef.services.{ ServiceDependencies, ServiceBootstrap }
import org.totalgrid.reef.client.service.proto.Descriptors
import org.totalgrid.reef.client.sapi.service.SyncServiceBase
import org.totalgrid.reef.client.service.proto.Events
import org.totalgrid.reef.client.sapi.client.rest.{ Client, Connection }
import org.totalgrid.reef.client.sapi.client.{ Event, BasicRequestHeaders }
import org.totalgrid.reef.client.service.proto.Commands.UserCommandRequest
import org.totalgrid.reef.client.AddressableDestination
import org.totalgrid.reef.client.service.proto.ProcessStatus.StatusSnapshot

abstract class EndpointRelatedTestBase extends DatabaseUsingTestNotTransactionSafe with RunTestsInsideTransaction with Logging {

  class CountingEventSink extends SystemEventSink {
    import scala.collection.mutable.{ Map, ListBuffer }
    val received = Map.empty[String, ListBuffer[Events.Event]]

    def publishSystemEvent(evt: Events.Event) = this.synchronized {
      val eventName = evt.getEventType
      received.get(eventName) match {
        case Some(l) => l.append(evt)
        case None =>
          val lb = new ListBuffer[Events.Event]
          lb.append(evt)
          received.put(eventName, lb)
      }
    }

    def getTotalEventCount(): Int = {
      received.foldLeft(0) { (sum, e) => sum + e._2.size }
    }

    def getEventCount(eventName: String): Int = {
      received.get(eventName) match {
        case Some(l) => l.size
        case None => 0
      }
    }

  }

  class MockMeasProc(measProcConnection: SyncService[MeasurementProcessingConnection], rtDb: MeasSink, amqp: Connection, client: Client) {

    // endpoint name, measurementbatch
    val mb = new SyncVar(Nil: List[(String, MeasurementBatch)])

    def onMeasProcAssign(event: Event[MeasurementProcessingConnection]): Unit = {

      val measProcAssign = event.value
      if (event.event != Envelope.SubscriptionEventType.ADDED) return

      val measProc = new MeasBatchProcessor {
        def process(m: MeasurementBatch) {
          rtDb.set(m.getMeasList.toList)
          val endpointName = measProcAssign.getLogicalNode.getName
          mb.atomic(x => ((endpointName, m) :: x).reverse)
        }
      }

      val measBatchService = new AddressableMeasurementBatchService(measProc)
      val exchange = measBatchService.descriptor.id
      val destination = new AddressableDestination(measProcAssign.getRouting.getServiceRoutingKey)

      amqp.bindService(measBatchService, client, destination, false)

      logger.info { "attaching measProcConnection + " + measProcAssign.getRouting + " id " + measProcAssign.getId }

      measProcConnection.put(measProcAssign.toBuilder.setReadyTime(System.currentTimeMillis).build)
    }
  }

  class CoordinatorFixture(amqp: Connection, publishEvents: Boolean = true) {
    val startTime = System.currentTimeMillis - 1
    val client = amqp.login("")

    val rtDb = new InMemoryMeasurementStore()
    val eventSink = new CountingEventSink

    val deps = new ServiceDependenciesDefaults(dbConnection, amqp, amqp, rtDb, eventSink)
    val contextSource = new MockRequestContextSource(deps)

    val modelFac = new ModelFactories(deps)

    val heartbeatCoordinator = new ProcessStatusCoordinator(modelFac.procStatus, contextSource)

    val processStatusService = new SyncService(new ProcessStatusService(modelFac.procStatus, false), contextSource)
    val appService = new SyncService(new ApplicationConfigService(modelFac.appConfig), contextSource)
    val frontendService = new SyncService(new FrontEndProcessorService(modelFac.fep), contextSource)
    val portService = new SyncService(new FrontEndPortService(modelFac.fepPort), contextSource)
    val commEndpointService = new SyncService(new CommunicationEndpointService(modelFac.endpoints), contextSource)
    val entityService = new EntityService(modelFac.entities)
    val pointService = new SyncService(new PointService(modelFac.points), contextSource)
    val commandService = new SyncService(new CommandService(modelFac.cmds), contextSource)
    val frontEndConnection = new SyncService(new CommunicationEndpointConnectionService(modelFac.fepConn), contextSource)
    val measProcConnection = new SyncService(new MeasurementProcessingConnectionService(modelFac.measProcConn), contextSource)
    val measSnapshotService = new SyncService(new MeasurementSnapshotService(rtDb), contextSource)
    val batchService = new SyncService(new MeasurementBatchService(), contextSource)

    var measProcMap = Map.empty[String, MockMeasProc]

    def addApp(name: String, caps: List[String], networks: List[String] = List("any"), location: String = "any"): ApplicationConfig = {
      val b = ApplicationConfig.newBuilder()
      caps.foreach(b.addCapabilites)
      b.setUserName(name).setInstanceName(name).setLocation(location)
      networks.foreach { b.addNetworks(_) }
      val cfg = appService.put(b.build).expectOne()
      if (caps.find(_ == "Processing").isDefined) setupMockMeasProc(name, cfg)
      cfg
    }

    def addProtocols(app: ApplicationConfig, protocols: List[String] = List("dnp3", "benchmark")): FrontEndProcessor = {
      val b = FrontEndProcessor.newBuilder
      protocols.foreach(b.addProtocols(_))
      b.setAppConfig(app)

      frontendService.put(b.build).expectOne()
    }

    def addFep(name: String, protocols: List[String] = List("dnp3", "benchmark")): FrontEndProcessor = {
      addProtocols(addApp(name, List("FEP")), protocols)
    }
    def timeoutApplication(appConfig: ApplicationConfig) {
      processStatusService.put(StatusSnapshot.newBuilder
        .setProcessId(appConfig.getProcessId)
        .setInstanceName(appConfig.getInstanceName)
        .setTime(System.currentTimeMillis + 600000)
        .setOnline(false).build).expectOne()
    }

    def addMeasProc(name: String): ApplicationConfig = {
      addApp(name, List("Processing"))
    }
    def setupMockMeasProc(name: String, meas: ApplicationConfig) {

      val measSink = new MeasSink {
        def set(meas: Seq[Measurement]) {
          rtDb.set(meas)
          meas.foreach { m =>
            amqp.publishEvent(Envelope.SubscriptionEventType.MODIFIED, m, m.getName)
          }
        }
      }

      val mockMeas = new MockMeasProc(measProcConnection, measSink, amqp, client)

      val env = getSubscriptionQueue(client, Descriptors.measurementProcessingConnection, mockMeas.onMeasProcAssign _)

      val conns = measProcConnection.get(MeasurementProcessingConnection.newBuilder.setMeasProc(meas).build, env).expectMany()

      conns.foreach(c => mockMeas.onMeasProcAssign(Event(Envelope.SubscriptionEventType.ADDED, c)))

      measProcMap += (name -> mockMeas)

      meas
    }

    def addDevice(name: String, pname: String = "test_point", autoAssigned: Boolean = true): Endpoint = {
      val send = Endpoint.newBuilder.setName(name).setProtocol("benchmark").setAutoAssigned(autoAssigned)
      addEndpointPointsAndCommands(send, List(name + "." + pname), List(name + ".test_commands"))
    }

    def addDnp3Device(name: String, network: Option[String] = Some("any"), location: Option[String] = None, portName: Option[String] = None): Endpoint = {
      val netPort = network.map { net => CommChannel.newBuilder.setName(portName.getOrElse(name + "-port")).setIp(IpPort.newBuilder.setNetwork(net).setAddress("localhost").setPort(1200)).build }
      val locPort = location.map { loc => CommChannel.newBuilder.setName(portName.getOrElse(name + "-serial")).setSerial(SerialPort.newBuilder.setLocation(loc).setPortName("COM1")).build }
      val port = portService.put(netPort.getOrElse(locPort.get)).expectOne()
      val send = Endpoint.newBuilder.setName(name).setProtocol("dnp3").setChannel(port)
      addEndpointPointsAndCommands(send, List(name + ".test_point"), List(name + ".test_commands"))
    }

    def addEndpointPointsAndCommands(ce: Endpoint.Builder, pointNames: List[String], commandNames: List[String]) = {
      val owns = EndpointOwnership.newBuilder
      pointNames.foreach { pname =>
        owns.addPoints(pname)
        val pointProto = Point.newBuilder().setName(pname).setType(PointType.ANALOG).setUnit("raw").build
        pointService.put(pointProto).expectOne()
      }
      commandNames.foreach { cname =>
        owns.addCommands(cname)
        val cmdProto = Command.newBuilder().setName(cname).setDisplayName(cname).setType(CommandType.CONTROL).build
        commandService.put(cmdProto).expectOne()
      }
      ce.setOwnerships(owns)
      commEndpointService.put(ce.build).expectOne()
    }

    def claimEndpoint(endpointName: String, fepName: Option[String]): EndpointConnection = {
      val b = EndpointConnection.newBuilder.setEndpoint(Endpoint.newBuilder.setName(endpointName))
      b.setFrontEnd(FrontEndProcessor.newBuilder.setAppConfig(ApplicationConfig.newBuilder.setInstanceName(fepName.getOrElse("-"))))
      frontEndConnection.put(b.build).expectOne()
    }

    def getPoint(device: String): Point =
      pointService.get(Point.newBuilder.setName(device + ".test_point").build).expectOne()

    def getValue(pname: String): Long = rtDb.get(pname).map(_.getIntVal).getOrElse(0)

    def updatePoint(pname: String, value: Int = 10) {
      // simulate a measproc shoving a measurement into the rtDatabase
      import org.totalgrid.reef.measproc.ProtoHelper._
      rtDb.set(makeInt(pname, value) :: Nil)
    }

    def pointsInDatabase(): Int = rtDb.numPoints

    def pointsWithBadQuality(): Int =
      rtDb.allCurrent.filter { m => m.getQuality.getValidity != Quality.Validity.GOOD }.size

    def checkPoints(numTotal: Int, numBad: Int = -1) {
      pointsInDatabase should equal(numTotal)
      if (numBad != -1) pointsWithBadQuality should equal(numBad)
    }

    def listenForMeasurements(measProcName: String) = measProcMap.get(measProcName).get.mb

    def checkFeps(fep: EndpointConnection, online: Boolean, frontEndId: Option[FrontEndProcessor], hasServiceRouting: Boolean): Unit =
      checkFeps(List(fep), online, frontEndId, hasServiceRouting)

    def checkFeps(feps: List[EndpointConnection], online: Boolean, frontEndId: Option[FrontEndProcessor], hasServiceRouting: Boolean): Unit = {
      feps.forall { f => f.hasEndpoint == true } should equal(true)

      val fepNames = feps.map { _.frontEnd.appConfig.instanceName }.distinct
      if (frontEndId.isDefined) fepNames should equal(List(Some(frontEndId.get.getAppConfig.getInstanceName)))
      else fepNames should equal(List(None))

      feps.forall { f => f.hasRouting == hasServiceRouting } should equal(true)
    }

    def checkMeasProcs(procs: List[MeasurementProcessingConnection], measProcId: Option[ApplicationConfig], serviceRouting: Boolean) {
      procs.forall { f => f.hasLogicalNode == true } should equal(true)
      procs.forall { f => f.hasMeasProc == measProcId.isDefined && (measProcId.isEmpty || measProcId.get.getUuid == f.getMeasProc.getUuid) } should equal(true)
      procs.forall { f => f.hasRouting == serviceRouting } should equal(true)
    }

    def checkAssignments(num: Int, fepFrontEndId: Option[FrontEndProcessor], measProcId: Option[ApplicationConfig]) {
      val feps = frontEndConnection.get(EndpointConnection.newBuilder.setId("*").build).expectMany(num)
      val procs = measProcConnection.get(MeasurementProcessingConnection.newBuilder.setId("*").build).expectMany(num)

      checkFeps(feps, false, fepFrontEndId, measProcId.isDefined)
      checkMeasProcs(procs, measProcId, measProcId.isDefined)
    }

    def subscribeFepAssignements(expected: Int, fep: FrontEndProcessor) = {
      val (updates, env) = getEventQueueWithCode(client, Descriptors.endpointConnection)
      frontEndConnection.get(EndpointConnection.newBuilder.setFrontEnd(fep).build, env).expectMany(expected)
      updates.size should equal(0)
      updates
    }

    def setEndpointEnabled(ce: EndpointConnection, enabled: Boolean) = {
      val ret = frontEndConnection.put(ce.toBuilder.setEnabled(enabled).build).expectOne()
      ret.getEnabled should equal(enabled)
      ret
    }

    def setEndpointState(ce: EndpointConnection, state: EndpointConnection.State) = {
      val ret = frontEndConnection.put(ce.toBuilder.setState(state).build).expectOne()
      ret.getState should equal(state)
      ret
    }

    def bindCommandHandler(service: SyncServiceBase[UserCommandRequest], key: String) {
      amqp.bindService(service, client, new AddressableDestination(key), false)
    }

    def subscribeMeasurements() = {
      val (updates, env) = getEventQueue(client, Descriptors.measurement)
      measSnapshotService.get(MeasurementSnapshot.newBuilder.addPointNames("*").build, env).expectMany()
      updates.size should equal(0)
      updates
    }

    def publishMeas(meas: MeasurementBatch): MeasurementBatch = {
      batchService.put(meas).expectOne()
    }
  }
}
