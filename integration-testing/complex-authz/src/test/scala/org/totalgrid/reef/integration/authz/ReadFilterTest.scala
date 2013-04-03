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
package org.totalgrid.reef.integration.authz

import scala.collection.JavaConversions._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.totalgrid.reef.client.service.proto.Calculations.Calculation
import org.totalgrid.reef.client.sapi.sync.ClientOperations
import org.totalgrid.reef.client.service.proto.Model.Point
import org.totalgrid.reef.client.service.proto.Processing.TriggerSet
import org.totalgrid.reef.client.exception._
import org.totalgrid.reef.client.service.entity.EntityRelation

@RunWith(classOf[JUnitRunner])
class ReadFilterTest extends AuthTestBase {

  private val visibleCommands = List("C1", "C2", "C3", "C4", "C5", "C6", "C7", "C8")
  private val visiblePoints = List("P1", "P2", "P3", "P4", "P5", "P6", "P7", "P8")
  private val visibleEquipment = List("Sub1", "Sub2", "Sub3", "Sub4")
  private val visibleEquipmentGroups = List("East", "West")
  private val visibleEndpoints = List("EastEndpoint", "WestEndpoint", "NukeEndpoint")
  private val visibleAgents = List("limited_regional_op")
  private val allVisibile = visibleCommands ::: visiblePoints ::: visibleEquipment ::: visibleEquipmentGroups ::: visibleEndpoints ::: visibleAgents

  private val invisibleCommands = List("C9", "C10", "C11", "C12")
  private val invisiblePoints = List("P9", "P10", "P11", "P12")

  test("See only entities we're allowed to") {
    as("limited_regional_op") { ops =>

      val entNames = ops.getEntities().map(_.getName)
      entNames.toSet should equal(allVisibile.toSet)
    }
  }
  test("Entity search only find visibile entries") {
    as("limited_regional_op") { ops =>

      val entitesByType = ops.getEntitiesWithType("Command").map(_.getName)
      entitesByType.toSet should equal(visibleCommands.toSet)

      val sub1 = ops.getEntityByName("Sub1")
      val sub1Commands = ops.getEntityRelations(sub1.getUuid, List(new EntityRelation("owns", "Command", true))).map(_.getName)

      sub1Commands.toSet should equal(List("C1", "C2").toSet)
    }
  }

  test("Multi step entity search only find visibile entries") {
    as("limited_regional_op") { ops =>

      val allCommandsAsEntityRel = ops.getEntityRelationsFromTypeRoots("Equipment", List(new EntityRelation("owns", "Command", true)))
      allCommandsAsEntityRel.map { _.getName }.toSet should equal(visibleEquipment.toSet)

      val allCommandNames = allCommandsAsEntityRel.map { _.getRelationsList.map { _.getEntitiesList.map { _.getName } }.flatten }.flatten

      allCommandNames.toSet should equal(visibleCommands.toSet)

      val nukeEndpoint = ops.getEntityByName("NukeEndpoint")
      val nukeWithCommandsTree = ops.getEntityChildren(nukeEndpoint.getUuid, "source", -1, List("Command"))

      val nukeCommands = nukeWithCommandsTree.getRelationsList.map { _.getEntitiesList.map { _.getName } }.flatten

      nukeCommands should equal(Nil)
    }
  }

  test("Entity search from hidden roots are blank") {
    as("limited_regional_op") { ops =>

      val rootEquipment = ops.getEntitiesWithType("Root")
      rootEquipment should equal(Nil)
      val queryFromInvisibleRoot = ops.getEntityRelationsFromTypeRoots("Root", List(new EntityRelation("owns", "Command", true))).map(_.getName)
      queryFromInvisibleRoot should equal(Nil)
    }
  }

  test("See only allowed commands") {
    as("limited_regional_op") { ops =>

      val commands = ops.getCommands().map(_.getName)

      commands.toSet should equal(visibleCommands.toSet)
    }
  }

  test("See only allowed points") {
    as("limited_regional_op") { ops =>

      val pointNames = ops.getPoints().map(_.getName)

      pointNames.toSet should equal(visiblePoints.toSet)
    }
  }

  test("Limited point overides") {

    val points = client.getPoints()
    try {
      points.foreach { client.setPointOutOfService(_) }

      as("limited_regional_op") { ops =>

        val pointNames = ops.getMeasurementOverrides().map { _.getPoint.getName }

        pointNames.toSet should equal(visiblePoints.toSet)
      }
    } finally {
      points.foreach { client.clearMeasurementOverridesOnPoint(_) }
    }
  }

  test("Limited point calculations") {

    val points = client.getPoints()
    try {
      points.foreach { point =>
        client.addCalculation(Calculation.newBuilder().setOutputPoint(point).setFormula("").build)
      }

      as("limited_regional_op") { ops =>
        val pointNames = ops.getCalculations().map { _.getOutputPoint.getName }
        pointNames.toSet should equal(visiblePoints.toSet)
      }
    } finally {
      val calcs = client.getCalculations()
      calcs.foreach { client.deleteCalculation(_) }
    }
  }

  test("Limited point triggersets") {

    val loader = session.getService(classOf[ClientOperations])

    val points = client.getPoints()
    val triggerSets = points.map { TriggerSet.newBuilder.setPoint(_).build }

    triggerSets.foreach { loader.putOne(_) }
    try {
      asOps("limited_regional_op") { ops =>

        val triggers = ops.getMany(TriggerSet.newBuilder.setPoint(Point.newBuilder.setName("*")).build)
        val pointNames = triggers.map { _.getPoint.getName }
        pointNames.toSet should equal(visiblePoints.toSet)
      }
    } finally {
      triggerSets.foreach { loader.deleteOne(_) }
    }

  }

  test("Limited command lock and history view") {

    val commands = client.getCommands()
    executeCommands(client, commands.map { _.getName })

    as("limited_regional_op") { ops =>

      val commandRequests = ops.getCommandHistory()
      val requestNames = commandRequests.map { _.getCommandRequest.getCommand.getName }
      requestNames.toSet should equal(visibleCommands.toSet)

      val commandLocks = ops.getCommandLocksIncludingDeleted()
      val lockNames = commandLocks.map { _.getCommandsList.toList.map { _.getName } }.flatten
      lockNames.toSet should equal(visibleCommands.toSet)
    }
  }

  test("Partially visibile command lock") {
    val commands = client.getCommands()
    val lock = client.createCommandExecutionLock(commands)
    try {
      as("limited_regional_op") { ops =>
        val commandLocks = ops.getCommandLocks()
        commandLocks.map { _.getId } should equal(List(lock.getId))
        val commandLock = commandLocks.head

        val commandNames = commandLock.getCommandsList.map { _.getName }
        commandNames.toSet should equal(visibleCommands.toSet)

        intercept[ExpectationException] {
          // TODO: shouldn't have been returned name of agent we can't see and same with error message
          val agent = ops.getAgentByName(commandLock.getUser)
        }
        val errorMessage = intercept[BadRequestException] {
          ops.createCommandExecutionLock(ops.getCommandByName(visibleCommands.head))
        }.getMessage

        visibleCommands.foreach { cmdName => errorMessage should include(cmdName) }
        invisibleCommands.foreach { cmdName => errorMessage should not include (cmdName) }

        errorMessage should include("system")
      }
    } finally {
      client.deleteCommandLock(lock)
    }
  }

  test("Completely invisibile command lock") {
    // since we can't see C9 or C10 we can't see a lock on those commands
    val commands = client.getCommandsByNames(invisibleCommands)
    val lock = client.createCommandExecutionLock(commands)
    try {
      as("limited_regional_op") { ops =>
        val commandLocks = ops.getCommandLocks()
        commandLocks should equal(Nil)
      }
    } finally {
      client.deleteCommandLock(lock)
    }
  }

  test("View Endpoints") {

    val allEndpoints = client.getEndpoints()

    as("limited_regional_op") { ops =>
      val endpoints = ops.getEndpoints()

      endpoints.map { _.getUuid }.toSet should equal(allEndpoints.map { _.getUuid }.toSet)

      val pointNames = endpoints.map { _.getOwnerships.getPointsList.toList }.flatten.distinct
      pointNames.toSet should equal(visiblePoints.toSet)

      val commandNames = endpoints.map { _.getOwnerships.getCommandsList.toList }.flatten.distinct
      commandNames.toSet should equal(visibleCommands.toSet)

      val endpoint = ops.getEndpointByName("NukeEndpoint")
      endpoint.getOwnerships.getPointsCount should equal(0)
      endpoint.getOwnerships.getCommandsCount should equal(0)
    }

  }

  test("View Endpoint Connections for Visibile Endpoints") {

    as("limited_regional_op") { ops =>
      val endpoints = ops.getEndpoints()

      val connections = ops.getEndpointConnections()

      connections.map { _.getEndpoint.getName }.toSet should equal(endpoints.map { _.getName }.toSet)
    }
  }

}
