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

import org.totalgrid.reef.models.RunTestsInsideTransaction

import org.squeryl.PrimitiveTypeMode._

import org.totalgrid.reef.models.ApplicationSchema
import org.totalgrid.reef.persistence.squeryl.{ DbConnector, DbInfo }
import org.totalgrid.reef.services._

import org.totalgrid.reef.measproc.MeasurementStreamProcessingNode

import org.totalgrid.reef.proto.Measurements._
import org.totalgrid.reef.proto.FEP._
import org.totalgrid.reef.proto.Processing._
import org.totalgrid.reef.proto.Model._
import org.totalgrid.reef.proto.Application._

import org.totalgrid.reef.services.ServiceResponseTestingHelpers._

import org.totalgrid.reef.api.service.sync.SyncServiceBase

import org.totalgrid.reef.reactor.mock.InstantReactor
import _root_.scala.collection.JavaConversions._

import org.scalatest.{ FunSuite, BeforeAndAfterAll, BeforeAndAfterEach }
import org.scalatest.matchers.ShouldMatchers

import org.totalgrid.reef.measurementstore.{ MeasurementStore, InMemoryMeasurementStore }
import org.totalgrid.reef.util.{ Logging, SyncVar }
import org.totalgrid.reef.messaging.AMQPProtoFactory
import org.totalgrid.reef.messaging.serviceprovider.{ SilentEventPublishers, PublishingSubscriptionActor, ServiceSubscriptionHandler, ServiceEventPublisherMap }
import org.totalgrid.reef.proto.{ ReefServicesList }
import org.totalgrid.reef.api._
import ServiceTypes.Event

abstract class EndpointRelatedTestBase extends FunSuite with ShouldMatchers with BeforeAndAfterAll with BeforeAndAfterEach with RunTestsInsideTransaction with Logging {
  override def beforeAll() {
    DbConnector.connect(DbInfo.loadInfo("test"))
  }
  override def beforeEach() {
    transaction { ApplicationSchema.reset }
  }

  class LockStepServiceEventPublisherRegistry(amqp: AMQPProtoFactory, lookup: ServiceList) extends ServiceEventPublisherMap(lookup) {

    def createPublisher(exchange: String): ServiceSubscriptionHandler = {
      val reactor = new InstantReactor {}
      val pubsub = new PublishingSubscriptionActor(exchange, reactor)
      amqp.add(pubsub)
      pubsub
    }

  }

  class MockMeasProc(measProcConnection: MeasurementProcessingConnectionService, rtDb: MeasurementStore, amqp: AMQPProtoFactory) {

    val mb = new SyncVar(Nil: List[(String, MeasurementBatch)])

    def onMeasProcAssign(event: Event[MeasurementProcessingConnection]): Unit = {

      val measProcAssign = event.result
      if (event.event != Envelope.Event.ADDED) return

      val measProc = new org.totalgrid.reef.measproc.ProcessingNode {
        def process(m: MeasurementBatch) {
          rtDb.set(m.getMeasList.toList)
          mb.atomic(x => ((measProcAssign.getLogicalNode.getName, m) :: x).reverse)
        }

        def add(over: MeasOverride) {}
        def remove(over: MeasOverride) {}

        def add(set: TriggerSet) {}
        def remove(set: TriggerSet) {}
      }
      MeasurementStreamProcessingNode.attachNode(measProc, measProcAssign, amqp, new InstantReactor {})

      info { "attaching measProcConnection + " + measProcAssign.getRouting + " uid " + measProcAssign.getUid }

      measProcConnection.put(measProcAssign.toBuilder.setReadyTime(System.currentTimeMillis).build)
    }
  }

  class CoordinatorFixture(amqp: AMQPProtoFactory, publishEvents: Boolean = true) {
    val startTime = System.currentTimeMillis - 1

    val pubs = if (publishEvents) new LockStepServiceEventPublisherRegistry(amqp, ReefServicesList) else new SilentEventPublishers
    val rtDb = new InMemoryMeasurementStore()
    val modelFac = new core.ModelFactories(pubs, new SilentSummaryPoints, rtDb)

    def attachServices(endpoints: Seq[SyncServiceBase[_]]): Unit = endpoints.foreach { ep =>
      val exch = ReefServicesList.getServiceInfo(ep.descriptor.getKlass).exchange
      amqp.bindService(exch, ep.respond, true)
    }

    val heartbeatCoordinator = new ProcessStatusCoordinator(modelFac.procStatus)

    val processStatusService = new ProcessStatusService(modelFac.procStatus)
    val appService = new ApplicationConfigService(modelFac.appConfig)
    val frontendService = new FrontEndProcessorService(modelFac.fep)
    val portService = new FrontEndPortService(modelFac.fepPort)
    val commEndpointService = new core.CommunicationEndpointService(modelFac.endpoints)
    val entityService = new EntityService
    val pointService = new core.PointService(modelFac.points)
    val frontEndConnection = new CommunicationEndpointConnectionService(modelFac.fepConn)
    val measProcConnection = new MeasurementProcessingConnectionService(modelFac.measProcConn)

    val services = List(
      processStatusService,
      appService,
      frontendService,
      portService,
      commEndpointService,
      entityService,
      pointService,
      frontEndConnection,
      measProcConnection)

    attachServices(services)

    var measProcMap = Map.empty[String, MockMeasProc]

    def addApp(name: String, caps: List[String], network: String = "any", location: String = "any"): ApplicationConfig = {
      val b = ApplicationConfig.newBuilder()
      caps.foreach(b.addCapabilites)
      b.setUserName(name).setInstanceName(name).setNetwork(network)
        .setLocation(location)
      val cfg = one(appService.put(b.build))
      if (caps.find(_ == "Processing").isDefined) setupMockMeasProc(name, cfg)
      cfg
    }

    def addProtocols(app: ApplicationConfig, protocols: List[String] = List("dnp3", "benchmark")): FrontEndProcessor = {
      val b = FrontEndProcessor.newBuilder
      protocols.foreach(b.addProtocols(_))
      b.setAppConfig(app)

      one(frontendService.put(b.build))
    }

    def addFep(name: String, protocols: List[String] = List("dnp3", "benchmark")): FrontEndProcessor = {
      addProtocols(addApp(name, List("FEP")), protocols)
    }

    def addMeasProc(name: String): ApplicationConfig = {
      addApp(name, List("Processing"))
    }
    def setupMockMeasProc(name: String, meas: ApplicationConfig) {

      val mockMeas = new MockMeasProc(measProcConnection, rtDb, amqp)

      val queueName = new SyncVar("")
      val sub = amqp.getEventQueue(MeasurementProcessingConnection.parseFrom, mockMeas.onMeasProcAssign _)
      sub.observe((online: Boolean, qname: String) => queueName.update(qname))

      queueName.waitWhile("")

      val env = new ServiceHandlerHeaders(new RequestEnv)
      env.setSubscribeQueue(queueName.current)

      val conns = measProcConnection.get(MeasurementProcessingConnection.newBuilder.setMeasProc(meas).build, env.env)

      conns.foreach(c => mockMeas.onMeasProcAssign(new Event(Envelope.Event.ADDED, c)))

      measProcMap += (name -> mockMeas)

      meas
    }

    def addDevice(name: String, pname: String = "test_point"): CommunicationEndpointConfig = {
      val owns = EndpointOwnership.newBuilder.addPoints(name + "." + pname).addCommands(name + ".test_commands")
      val send = CommunicationEndpointConfig.newBuilder()
        .setName(name).setProtocol("benchmark").setOwnerships(owns).build
      one(commEndpointService.put(send))
    }

    def addDnp3Device(name: String, network: Option[String] = Some("any"), location: Option[String] = None): CommunicationEndpointConfig = {
      val netPort = network.map { net => Port.newBuilder.setName(name + "-port").setIp(IpPort.newBuilder.setNetwork(net).setAddress("localhost").setPort(1200)).build }
      val locPort = location.map { loc => Port.newBuilder.setName(name + "-serial").setSerial(SerialPort.newBuilder.setLocation(loc).setPortName("COM1")).build }
      val port = one(portService.put(netPort.getOrElse(locPort.get)))
      val owns = EndpointOwnership.newBuilder.addPoints(name + ".test_point").addCommands(name + ".test_commands")
      val send = CommunicationEndpointConfig.newBuilder()
        .setName(name).setProtocol("dnp3").setPort(port).setOwnerships(owns).build
      one(commEndpointService.put(send))
    }

    def getPoint(device: String): Point = {
      one(pointService.get(Point.newBuilder.setName(device + ".test_point").build))
    }

    def getValue(pname: String): Long = {
      rtDb.get(pname).map(_.getIntVal).getOrElse(0)
    }

    def updatePoint(pname: String, value: Int = 10) {
      // simulate a measproc shoving a measurement into the rtDatabase
      import org.totalgrid.reef.measproc.ProtoHelper._
      rtDb.set(makeInt(pname, value) :: Nil)
    }

    def pointsInDatabase(): Int = {
      rtDb.numPoints
    }

    def pointsWithBadQuality(): Int = {
      rtDb.allCurrent.filter { m => m.getQuality.getValidity != Quality.Validity.GOOD }.size
    }

    def checkPoints(numTotal: Int, numBad: Int = -1) {
      pointsInDatabase should equal(numTotal)
      if (numBad != -1) pointsWithBadQuality should equal(numBad)
    }

    def listenForMeasurements(measProcName: String) = {
      measProcMap.get(measProcName).get.mb
    }

    def checkFeps(feps: List[CommunicationEndpointConnection], online: Boolean, frontEndUid: Option[FrontEndProcessor], hasServiceRouting: Boolean) {
      feps.forall { f => f.hasEndpoint == true } should equal(true)
      feps.forall { f => f.getOnline == online } should equal(true)
      feps.forall { f => f.hasFrontEnd == frontEndUid.isDefined && (frontEndUid.isEmpty || frontEndUid.get.getUid == f.getFrontEnd.getUid) } should equal(true)
      //feps.forall { f => f.hasFrontEnd == hasFrontEnd } should equal(true)
      feps.forall { f => f.hasRouting == hasServiceRouting } should equal(true)
    }

    def checkMeasProcs(procs: List[MeasurementProcessingConnection], measProcUid: Option[ApplicationConfig], serviceRouting: Boolean) {
      procs.forall { f => f.hasLogicalNode == true } should equal(true)
      procs.forall { f => f.hasMeasProc == measProcUid.isDefined && (measProcUid.isEmpty || measProcUid.get.getUid == f.getMeasProc.getUid) } should equal(true)
      procs.forall { f => f.hasRouting == serviceRouting } should equal(true)
    }

    def checkAssignments(num: Int, fepFrontEndUid: Option[FrontEndProcessor], measProcUid: Option[ApplicationConfig]) {
      val feps = many(num, frontEndConnection.get(CommunicationEndpointConnection.newBuilder.setUid("*").build))
      val procs = many(num, measProcConnection.get(MeasurementProcessingConnection.newBuilder.setUid("*").build))

      checkFeps(feps, false, fepFrontEndUid, measProcUid.isDefined)
      checkMeasProcs(procs, measProcUid, measProcUid.isDefined)
    }

    def subscribeFepAssignements(expected: Int, fep: FrontEndProcessor) = {
      val (updates, env) = getEventQueueWithCode[CommunicationEndpointConnection](amqp, CommunicationEndpointConnection.parseFrom)
      many(expected, frontEndConnection.get(CommunicationEndpointConnection.newBuilder.setFrontEnd(fep).build, env))
      updates.size should equal(0)
      updates
    }
  }
}
