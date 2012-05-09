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
package org.totalgrid.reef.loader.integration

import org.totalgrid.reef.loader.LoadManager

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import org.totalgrid.reef.loader.commons.{ LoaderServices, ModelDeleter }
import org.totalgrid.reef.client.sapi.rpc.impl.util.{ ModelPreparer, ServiceClientSuite }

@RunWith(classOf[JUnitRunner])
class DeleteChildrenTest extends ServiceClientSuite {

  def prepareLoaderServices(): LoaderServices = {
    val loader = session.getService(classOf[LoaderServices])
    loader.setHeaders(loader.getHeaders.setTimeout(50000))
    loader
  }

  lazy val loaderServices = prepareLoaderServices()
  val fileName = "../../assemblies/assembly-common/filtered-resources/samples/integration/config.xml"

  val dryRun = false
  val forceOffline = false
  val stream = Some(Console.out)
  val batchSize = 1

  test("Load integration model") {
    LoadManager.loadFile(loaderServices, fileName, true, false, false, 25)
    ModelPreparer.waitForEndpointsOnline(async)
  }

  test("Delete static endpoint") {

    val endpoint = client.getEndpointByName("StaticEndpoint")
    val commands = client.getCommandsBelongingToEndpoint(endpoint.getUuid)
    val points = client.getPointsBelongingToEndpoint(endpoint.getUuid)

    val previousEquipment = client.getEntitiesWithTypes(List("Equipment", "EquipmentGroup"))

    ModelDeleter.deleteChildren(loaderServices, List("StaticEndpoint"), dryRun, forceOffline, stream, batchSize) { (_, _) => }

    val leftPoints = client.getPoints()
    points.foreach { leftPoints should not contain (_) }

    val leftCommands = client.getCommands()
    commands.foreach { leftCommands should not contain (_) }

    client.getEndpoints() should not contain (endpoint)

    client.getEntitiesWithTypes(List("Equipment", "EquipmentGroup")) should equal(previousEquipment)
  }

  test("Delete Simulated Equipment") {

    val substationEquipment = client.getEntityByName("SimulatedSubstation")
    val parentEquipment = client.getEntityByName("SimulatedSubstation.Breaker01")

    val childrenOfSubstation = client.getEntityChildren(substationEquipment.getUuid, "owns", 5)
    val children = client.getEntityChildren(parentEquipment.getUuid, "owns", 5)
    val endpoint = client.getEndpointByName("SimulatedEndpoint")

    ModelDeleter.deleteChildren(loaderServices, List("SimulatedSubstation.Breaker01"), dryRun, forceOffline, stream, batchSize) { (_, _) => }

    val endpointAfter = client.getEndpointByName("SimulatedEndpoint")
    endpointAfter.getOwnerships.getPointsList should not equal (endpoint.getOwnerships.getPointsList)
    endpointAfter.getOwnerships.getCommandsList should not equal (endpoint.getOwnerships.getCommandsList)

    val childrenOfSubstationAfter = client.getEntityChildren(substationEquipment.getUuid, "owns", 5)
    childrenOfSubstationAfter should not equal (childrenOfSubstation)
  }

  /*
  karaf@root> entity:relations EquipmentGroup owns:*:true:Equipment owns:*:true:Point,Command

  +- SimulatedSubstation (EquipmentGroup, Substation)
    +- SimulatedSubstation.Line01 (Equipment, Line)
      |- SimulatedSubstation.Line01.Current (Analog, Point)
      |- SimulatedSubstation.Line01.VoltageSetpoint (Command, Setpoint)
    +- SimulatedSubstation.Breaker01 (Breaker, Equipment)
      |- SimulatedSubstation.Breaker01.Tripped (Point, Status)
      |- SimulatedSubstation.Breaker01.Bkr (Point, Status, bkrStatus)
      |- SimulatedSubstation.Breaker01.TripSetting (Command, Setpoint)
      |- SimulatedSubstation.Breaker01.Close (Command, Control, bkrClose)
      |- SimulatedSubstation.Breaker01.Trip (Command, Control, bkrTrip)
  +- StaticSubstation (EquipmentGroup, Substation)
    +- StaticSubstation.Line02 (Equipment, Line)
      |- StaticSubstation.Line02.Current (Analog, Point)
      |- StaticSubstation.Line02.VoltageSetpoint (Command, Setpoint)
    +- StaticSubstation.Breaker02 (Breaker, Equipment)
      |- StaticSubstation.Breaker02.Tripped (Point, Status)
      |- StaticSubstation.Breaker02.Bkr (Point, Status, bkrStatus)
      |- StaticSubstation.Breaker02.TripSetting (Command, Setpoint)
      |- StaticSubstation.Breaker02.Close (Command, Control, bkrClose)
      |- StaticSubstation.Breaker02.Trip (Command, Control, bkrTrip)
   */
}