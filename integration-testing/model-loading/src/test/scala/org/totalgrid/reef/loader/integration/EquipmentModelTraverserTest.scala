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

import org.totalgrid.reef.client.sapi.rpc.impl.util.{ ModelPreparer, ServiceClientSuite }
import org.totalgrid.reef.loader.commons._
import scala.collection.JavaConversions._
import org.totalgrid.reef.client.service.proto.Alarms.EventConfig
import org.totalgrid.reef.client.service.proto.FEP.{ Endpoint, CommChannel }
import org.totalgrid.reef.client.service.proto.Model._

@RunWith(classOf[JUnitRunner])
class EquipmentModelTraverserTest extends ServiceClientSuite {

  def prepareLoaderServices(): LoaderServices = {
    val loader = session.getService(classOf[LoaderServices])
    loader.setHeaders(loader.getHeaders.setTimeout(50000))
    loader
  }

  lazy val loaderServices = prepareLoaderServices()
  val fileName = "../../assemblies/assembly-common/filtered-resources/samples/integration/config.xml"

  test("Load integration model") {
    LoadManager.loadFile(loaderServices, fileName, true, false, false, 25)
    ModelPreparer.waitForEndpointsOnline(async)
  }

  private def collect(forDelete: Boolean, entity: Entity, expectedNames: List[String]) {
    collect(forDelete, List(entity), expectedNames)
  }
  private def collect(forDelete: Boolean, entities: List[Entity], expectedNames: List[String]) {
    val notifier = new TraversalProgressNotifier {
      var names = List.empty[String]
      def display(entity: Entity, depth: Int) {
        names ::= entity.getName
      }
    }
    val collector = new NameCapturingCollector
    val traverser = new EquipmentModelTraverser(loaderServices, collector, forDelete, Some(notifier))
    entities.foreach { traverser.collect(_) }
    traverser.finish()
    val extras = notifier.names.diff(expectedNames)
    val missedNames = expectedNames.diff(notifier.names)

    (missedNames, extras) should equal((Nil, Nil))

    // double check that notifier and collector see same objects
    collector.names.sorted should equal(notifier.names.sorted)
  }

  test("Traverse forDelete starting at endpoint") {

    val endpoint = client.getEndpointByName("SimulatedEndpoint")
    val commands = client.getCommandsBelongingToEndpoint(endpoint.getUuid)
    val points = client.getPointsBelongingToEndpoint(endpoint.getUuid)
    val configFiles = client.getConfigFilesUsedByEntity(endpoint.getUuid)

    val expectedNames = endpoint.getName :: commands.map { _.getName } ::: points.map { _.getName } ::: configFiles.map { _.getName }

    val foundNames = collect(true, client.getEntityByName(endpoint.getName), expectedNames)
  }

  test("Traverse forDelete starting at Equipment, partial endpoint") {

    val parentEquipment = client.getEntityByName("SimulatedSubstation.Breaker01")

    val children = client.getEntityChildren(parentEquipment.getUuid, "owns", 5)

    val expectedNames: List[String] = "Notes.txt" :: childNames(children)

    collect(true, parentEquipment, expectedNames)
  }

  test("Traverse forDelete starting at EquipmentGroup, full endpoint") {

    val parentEquipment = client.getEntityByName("SimulatedSubstation")

    val children = client.getEntityChildren(parentEquipment.getUuid, "owns", 5)
    val endpoint = client.getEndpointByName("SimulatedEndpoint")
    val configFiles = client.getConfigFilesUsedByEntity(endpoint.getUuid)

    val expectedNames = endpoint.getName :: "Notes.txt" :: childNames(children) ::: configFiles.map { _.getName }

    collect(true, parentEquipment, expectedNames)

  }

  test("Traverse forAddition starting at Equipment, parital endpoint") {

    val parentEquipment = client.getEntityByName("SimulatedSubstation.Breaker01")

    val children = client.getEntityChildren(parentEquipment.getUuid, "owns", 5)
    val endpoint = client.getEndpointByName("SimulatedEndpoint")
    val configFiles = client.getConfigFilesUsedByEntity(endpoint.getUuid)

    val expectedNames = endpoint.getName :: "Notes.txt" :: childNames(children) ::: configFiles.map { _.getName }

    collect(false, parentEquipment, expectedNames)
  }

  test("Traverse from both parents equals deleteEverything ") {

    val configFile = client.getEntityByName(fileName)
    val parents = configFile :: client.getEntitiesWithType("EquipmentGroup")

    val collector = new NameCapturingCollector
    ModelDeleter.collectEverything(loaderServices, collector)
    val expectedNames = collector.names

    collect(false, parents, expectedNames)
    collect(true, parents, expectedNames)
  }

  def childNames(entity: Entity): List[String] = {
    entity.getName :: entity.getRelationsList.toList.map { _.getEntitiesList.toList.map { childNames(_) }.flatten }.flatten
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

  class NameCapturingCollector extends ModelCollector {
    var names = List.empty[String]

    def addPoint(obj: Point, entity: Entity) = names ::= obj.getName
    def addCommand(obj: Command, entity: Entity) = names ::= obj.getName
    def addEndpoint(obj: Endpoint, entity: Entity) = names ::= obj.getName
    def addChannel(obj: CommChannel, entity: Entity) = names ::= obj.getName
    def addEquipment(entity: Entity) = names ::= entity.getName
    def addConfigFile(obj: ConfigFile, entity: Entity) = names ::= obj.getName

    def addEventConfig(eventConfig: EventConfig) {}
    def addEdge(edge: EntityEdge) {}
  }
}