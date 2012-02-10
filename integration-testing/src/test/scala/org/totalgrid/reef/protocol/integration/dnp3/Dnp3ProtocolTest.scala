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
package org.totalgrid.reef.protocol.integration.dnp3

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.totalgrid.reef.loader.commons.{ LoaderServices, ModelDeleter }
import org.totalgrid.reef.loader.LoadManager
import org.totalgrid.reef.client.service.proto.FEP.EndpointConnection
import org.totalgrid.reef.util.SyncVar
import org.totalgrid.reef.client.service.proto.Model.{ CommandType, ReefUUID }
import org.totalgrid.reef.client.{ SubscriptionEvent, SubscriptionEventAcceptor, SubscriptionResult }
import org.totalgrid.reef.client.sapi.rpc.AllScadaService
import org.totalgrid.reef.client.service.proto.FEP.EndpointConnection.State._
import org.totalgrid.reef.client.service.proto.Measurements.Measurement
import org.totalgrid.reef.client.service.proto.Measurements.Quality.Validity
import org.totalgrid.reef.client.sapi.rpc.impl.util.ServiceClientSuite

@RunWith(classOf[JUnitRunner])
class Dnp3ProtocolTest extends ServiceClientSuite {

  val dnp3ModelFile = "../protocol-dnp3/src/test/resources/sample-model.xml"

  val stream = Some(Console.out)

  def loaderServices = session.getRpcInterface(classOf[LoaderServices])
  def services = session.getRpcInterface(classOf[AllScadaService])

  test("Clear system") {
    ModelDeleter.deleteEverything(loaderServices, false, stream)
  }

  test("Load dnp3 model") {
    LoadManager.loadFile(loaderServices, dnp3ModelFile, false, false, false)
  }

  test("Cycle endpoints") {
    val endpoints = services.getEndpoints().await.toList

    endpoints.isEmpty should equal(false)

    val result = services.subscribeToEndpointConnections().await

    val map = new EndpointConnectionStateMap(result)

    // make sure everything starts comms_up and enabled
    map.checkAllState(true, COMMS_UP)

    (1 to 5).foreach { i =>

      val start = System.currentTimeMillis()
      endpoints.foreach { e => services.disableEndpointConnection(e.getUuid).await }

      map.checkAllState(false, COMMS_DOWN)
      val disabled = System.currentTimeMillis()
      println("Disabled to COMMS_DOWN in: " + (disabled - start))

      endpoints.foreach { e => services.enableEndpointConnection(e.getUuid).await }

      map.checkAllState(true, COMMS_UP)
      val enabled = System.currentTimeMillis()
      println("Enabled to COMMS_UP in: " + (enabled - disabled))

      waitForRepublishedMeasurement()
      println("COMMS_UP to first measurement roundtripped in: " + (System.currentTimeMillis() - enabled))
    }
  }

  test("Issue Commands") {
    val endpoint = services.getEndpointByName("DNPInput").await
    val commands = services.getCommandsBelongingToEndpoint(endpoint.getUuid).await.toList

    val lock = services.createCommandExecutionLock(commands).await
    try {
      commands.foreach { cmd =>
        cmd.getType match {
          case CommandType.CONTROL => services.executeCommandAsControl(cmd)
          case CommandType.SETPOINT_DOUBLE => services.executeCommandAsSetpoint(cmd, 55.55)
          case CommandType.SETPOINT_INT => services.executeCommandAsSetpoint(cmd, 100)
          case CommandType.SETPOINT_STRING => services.executeCommandAsSetpoint(cmd, "TestString")
        }
      }
    } finally {
      services.deleteCommandLock(lock)
    }
  }

  test("Reset system") {
    // test that we can remove the dnp3 endpoint configuration
    ModelDeleter.deleteEverything(loaderServices, false, stream)

    // reload the standard model for other tests
    LoadManager.loadFile(loaderServices, modelFile, false, false, false)

    // wait for all endpoints to be up before continuing
    val result = client.subscribeToEndpointConnections().await
    val map = new EndpointConnectionStateMap(result)
    map.checkAllState(true, EndpointConnection.State.COMMS_UP)
  }

  def waitForRepublishedMeasurement() {

    val measList = new SyncVar(List.empty[Measurement])

    def validMeas(meas: Measurement) = meas.getQuality.getValidity == Validity.GOOD

    val subResult = services.subscribeToMeasurementsByNames("RoundtripSubstation.Line01.Current" :: Nil).await
    subResult.getSubscription.start(new SubscriptionEventAcceptor[Measurement] {
      def onEvent(event: SubscriptionEvent[Measurement]) {
        measList.atomic(a => if (validMeas(event.getValue)) event.getValue :: a else a)
      }
    })

    // wait for the original substation to publish a measurement
    val originalList = new SyncVar(List.empty[Measurement])

    val originalResult = services.subscribeToMeasurementsByNames("OriginalSubstation.Line01.Current" :: Nil).await
    originalResult.getSubscription.start(new SubscriptionEventAcceptor[Measurement] {
      def onEvent(event: SubscriptionEvent[Measurement]) {
        originalList.atomic(a => if (validMeas(event.getValue)) event.getValue :: a else a)
      }
    })

    originalList.waitFor(_.size > 0)

    val meas = originalList.current.apply(0)

    measList.waitFor(_.find(m => m.getDoubleVal == meas.getDoubleVal && m.getTime == meas.getTime).isDefined)
  }

  class EndpointConnectionStateMap(result: SubscriptionResult[List[EndpointConnection], EndpointConnection]) {

    private def makeEntry(e: EndpointConnection) = {
      //println(e.getEndpointByUuid.getName + " s: " + e.getState + " e: " + e.getEnabled + " a:" + e.getFrontEnd.getUuid.getUuid + " at: " + e.getLastUpdate)
      e.getEndpoint.getUuid -> e
    }

    val endpointStateMap = result.getResult.map { makeEntry(_) }.toMap
    val syncVar = new SyncVar(endpointStateMap)

    result.getSubscription.start(new SubscriptionEventAcceptor[EndpointConnection] {
      def onEvent(event: SubscriptionEvent[EndpointConnection]) {
        syncVar.atomic(m => m + makeEntry(event.getValue))
      }
    })

    def checkAllState(enabled: Boolean, state: EndpointConnection.State) {
      syncVar.waitFor(x => x.values.forall(e => e.getEnabled == enabled && e.getState == state), 20000)
    }
    def checkState(uuid: ReefUUID, enabled: Boolean, state: EndpointConnection.State) {
      syncVar.waitFor(x => x.get(uuid).map(e => e.getEnabled == enabled && e.getState == state).getOrElse(false), 20000)
    }
  }
}