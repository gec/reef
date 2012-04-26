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
import org.totalgrid.reef.util.SyncVar
import org.totalgrid.reef.client.service.proto.Model.CommandType
import org.totalgrid.reef.client.{ SubscriptionEvent, SubscriptionEventAcceptor }
import org.totalgrid.reef.client.service.proto.FEP.EndpointConnection.State._
import org.totalgrid.reef.client.service.proto.Measurements.Measurement
import org.totalgrid.reef.client.service.proto.Measurements.Quality.Validity
import org.totalgrid.reef.client.sapi.rpc.impl.util.{ EndpointConnectionStateMap, ServiceClientSuite }
import org.totalgrid.reef.loader.commons.LoaderServices
import org.totalgrid.reef.loader.LoadManager

@RunWith(classOf[JUnitRunner])
class Dnp3ProtocolTest extends ServiceClientSuite {

  override val modelFile = "../../protocol-dnp3/src/test/resources/sample-model.xml"

  test("Cycle endpoints") {
    val (endpoints, map) = checkEndpointsOnline()

    (1 to 5).foreach { i =>

      val start = System.currentTimeMillis()
      endpoints.foreach { e => client.disableEndpointConnection(e.getUuid) }

      map.checkAllState(false, COMMS_DOWN)
      val disabled = System.currentTimeMillis()
      println("Disabled to COMMS_DOWN in: " + (disabled - start))

      endpoints.foreach { e => client.enableEndpointConnection(e.getUuid) }

      map.checkAllState(true, COMMS_UP)
      val enabled = System.currentTimeMillis()
      println("Enabled to COMMS_UP in: " + (enabled - disabled))

      waitForRepublishedMeasurement()
      println("COMMS_UP to first measurement roundtripped in: " + (System.currentTimeMillis() - enabled))
    }
  }

  test("Issue Commands") {
    val endpoint = client.getEndpointByName("DNPInput")
    val commands = client.getCommandsBelongingToEndpoint(endpoint.getUuid).toList

    val lock = client.createCommandExecutionLock(commands)
    try {
      commands.foreach { cmd =>
        cmd.getType match {
          case CommandType.CONTROL => client.executeCommandAsControl(cmd)
          case CommandType.SETPOINT_DOUBLE => client.executeCommandAsSetpoint(cmd, 55.55)
          case CommandType.SETPOINT_INT => client.executeCommandAsSetpoint(cmd, 100)
          case CommandType.SETPOINT_STRING => client.executeCommandAsSetpoint(cmd, "TestString")
        }
      }
    } finally {
      client.deleteCommandLock(lock)
    }
  }

  test("Reload config file on running system") {
    val loaderServices = session.getService(classOf[LoaderServices])

    loaderServices.setHeaders(loaderServices.getHeaders.setTimeout(50000))
    LoadManager.loadFile(loaderServices, modelFile, true, false, false, 25)

    checkEndpointsOnline()
  }

  private def checkEndpointsOnline() = {
    val endpoints = client.getEndpoints().toList

    endpoints.isEmpty should equal(false)

    val result = client.subscribeToEndpointConnections()

    val map = new EndpointConnectionStateMap(result)

    // make sure everything starts comms_up and enabled
    map.checkAllState(true, COMMS_UP)

    (endpoints, map)

  }

  private def setupMeasSubscribe(names: List[String]): Map[String, SyncVar[List[Measurement]]] = {
    def validMeas(meas: Measurement) = meas.getQuality.getValidity == Validity.GOOD

    val syncVars = names.map { _ -> new SyncVar(List.empty[Measurement]) }.toMap

    val subResult = client.subscribeToMeasurementsByNames(names)
    subResult.getSubscription.start(new SubscriptionEventAcceptor[Measurement] {
      def onEvent(event: SubscriptionEvent[Measurement]) {
        val m = event.getValue
        if (validMeas(m)) syncVars(m.getName).atomic(a => m :: a)
      }
    })

    syncVars
  }

  private def waitForRepublishedMeasurement() {

    val originalMeas = setupMeasSubscribe("OriginalSubstation.Line01.Current" :: "OriginalSubstation.Line01.ScaledCurrent" :: Nil)

    val roundtripMeas = setupMeasSubscribe("RoundtripSubstation.Line01.Current" :: "RoundtripSubstation.Line01.ScaledCurrent" :: Nil)

    def checkRoundtrip(originalName: String, roundtripName: String, scaling: Double = 1.0) {

      val originalEvents = originalMeas(originalName)
      val roundtrip = roundtripMeas(roundtripName)

      originalEvents.waitFor(_.size > 0)

      val meas = originalEvents.current.apply(0)

      def dEqual(d1: Double, d2: Double, ep: Double = 0.0001) = scala.math.abs(d1 - d2) <= ep

      roundtrip.waitFor(_.find(m => dEqual(m.getDoubleVal, meas.getDoubleVal * scaling) && m.getTime == meas.getTime).isDefined)
    }

    checkRoundtrip("OriginalSubstation.Line01.Current", "RoundtripSubstation.Line01.Current")

    checkRoundtrip("OriginalSubstation.Line01.ScaledCurrent", "RoundtripSubstation.Line01.ScaledCurrent", 1000.0)
  }

}