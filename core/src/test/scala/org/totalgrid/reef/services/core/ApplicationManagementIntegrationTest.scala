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

import org.scalatest.{ FunSuite, BeforeAndAfterAll, BeforeAndAfterEach }
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.totalgrid.reef.models.RunTestsInsideTransaction

import org.squeryl.PrimitiveTypeMode._

import org.totalgrid.reef.persistence.squeryl.{ DbConnector, DbInfo }
import org.totalgrid.reef.models._

import org.totalgrid.reef.util.SyncVar
import org.totalgrid.reef.proto.ProcessStatus._

import org.totalgrid.reef.messaging.mock.AMQPFixture
import org.totalgrid.reef.proto.Application.{ ApplicationConfig, HeartbeatConfig }

import org.totalgrid.reef.api.{ RequestEnv, ServiceHandlerHeaders, ServiceTypes }
import ServiceTypes.Event
import org.totalgrid.reef.messaging.AMQPProtoFactory
import org.totalgrid.reef.messaging.serviceprovider.ServiceEventPublisherRegistry

import ServiceHandlerHeaders.convertRequestEnvToServiceHeaders
import org.totalgrid.reef.proto.ReefServicesList

@RunWith(classOf[JUnitRunner])
class ApplicationManagementTest extends FunSuite with ShouldMatchers with BeforeAndAfterAll with BeforeAndAfterEach with RunTestsInsideTransaction {
  override def beforeAll() {
    DbConnector.connect(DbInfo.loadInfo("test"))
  }

  override def beforeEach() {
    transaction { ApplicationSchema.reset }
  }

  class Fixture(amqp: AMQPProtoFactory) {

    val start = System.currentTimeMillis

    val modelFac = new ModelFactories(new ServiceEventPublisherRegistry(amqp, ReefServicesList), new SilentSummaryPoints)

    val processStatusService = new ProcessStatusService(modelFac.procStatus)

    val applicationConfigService = new ApplicationConfigService(modelFac.appConfig)

    val processStatusCoordinator = new ProcessStatusCoordinator(modelFac.procStatus)

    amqp.bindService("app_config", applicationConfigService.respond)
    amqp.bindService("process_status", processStatusService.respond)

    val client = amqp.getProtoServiceClient(ReefServicesList, 5000)

    /// current state of the StatusSnapshot
    var lastSnapShot = new SyncVar[Option[StatusSnapshot]](None: Option[StatusSnapshot])

    /// register the application with the services handler
    val appConfig = registerInstance()

    /// use the appConfig information to setup the heartbeat publisher
    val hbeatSink = processStatusCoordinator.handleRawStatus _

    /// setup the subscription to the Snapshot service so we track the current status of the application
    subscribeSnapshotStatus()

    def registerInstance(): ApplicationConfig = {
      val b = ApplicationConfig.newBuilder()
      b.setUserName("proc").setInstanceName("proc01").setNetwork("any").setLocation("farm1").addCapabilites("Processing")
      b.setHeartbeatCfg(HeartbeatConfig.newBuilder.setPeriodMs(100)) // override the default period
      client.putOneOrThrow(b.build)
    }

    private def subscribeSnapshotStatus() {
      val eventQueueName = new SyncVar("")
      val hbeatSource = amqp.getEventQueue(StatusSnapshot.parseFrom, { evt: Event[StatusSnapshot] => lastSnapShot.update(Some(evt.result)) }, { q => eventQueueName.update(q) })

      // wait for the queue name to get populated (actor startup delay)
      eventQueueName.waitWhile("")

      val env = new RequestEnv
      env.setSubscribeQueue(eventQueueName.current)
      val config = client.getOneOrThrow(StatusSnapshot.newBuilder.setInstanceName(appConfig.getInstanceName).build, env)
      // do some basic checks to make sure we got the correct initial state
      config.getInstanceName should equal(appConfig.getInstanceName)
      config.getOnline should equal(true)

      lastSnapShot.update(Some(config))
    }

    /// wait up to 5 seconds for the condition to be satisfied (for actor messaging delays)
    def waitUntilSnapshot(f: StatusSnapshot => Boolean) {
      val wrap = { o: Option[StatusSnapshot] => if (o.isDefined) f(o.get) else false }
      if (wrap(lastSnapShot.current)) return true
      lastSnapShot.waitFor(wrap)
    }

    /// simulate doing sending a heartbeat from the application
    def doHeartBeat(online: Boolean, time: Long) {
      val msg = StatusSnapshot.newBuilder
        .setUid(appConfig.getHeartbeatCfg.getUid)
        .setInstanceName(appConfig.getInstanceName)
        .setTime(time)
        .setOnline(online).build
      hbeatSink(msg)
    }

    def checkTimeouts(time: Long) = processStatusCoordinator.checkTimeouts(time)

  }

  test("Application Timesout") {
    AMQPFixture.mock(true) { amqp =>
      val fix = new Fixture(amqp)

      fix.checkTimeouts(fix.start + 1000000)

      fix.waitUntilSnapshot(_.getOnline == false)
    }
  }

  test("Application Stays Online w/ Heartbeats") {
    AMQPFixture.mock(true) { amqp =>
      val fix = new Fixture(amqp)

      fix.checkTimeouts(fix.start + 1)
      fix.waitUntilSnapshot(_.getOnline == true)

      fix.doHeartBeat(true, fix.start + 1090)
      fix.checkTimeouts(fix.start + 1100)
      fix.waitUntilSnapshot(_.getOnline == true)

      fix.doHeartBeat(true, fix.start + 1190)
      fix.checkTimeouts(fix.start + 1200)
      fix.waitUntilSnapshot(_.getOnline == true)
    }
  }

  test("Application can go offline cleanly") {
    AMQPFixture.mock(true) { amqp =>
      val fix = new Fixture(amqp)

      fix.doHeartBeat(false, fix.start + 200)
      fix.waitUntilSnapshot(_.getOnline == false)
    }
  }

  test("Application can go online/offline") {
    AMQPFixture.mock(true) { amqp =>
      val fix = new Fixture(amqp)

      fix.doHeartBeat(false, fix.start + 200)
      fix.waitUntilSnapshot(_.getOnline == false)

      fix.registerInstance()
      fix.waitUntilSnapshot(_.getOnline == true)

      fix.doHeartBeat(false, fix.start + 5000)
      fix.waitUntilSnapshot(_.getOnline == false)

      fix.registerInstance()
      fix.waitUntilSnapshot(_.getOnline == true)
    }
  }

}