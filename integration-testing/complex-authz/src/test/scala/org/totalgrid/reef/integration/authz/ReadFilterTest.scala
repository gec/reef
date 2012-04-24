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
import org.totalgrid.reef.client.exception.BadRequestException

@RunWith(classOf[JUnitRunner])
class ReadFilterTest extends AuthTestBase {

  private val visibleCommands = List("C1", "C2", "C3", "C4", "C5", "C6", "C7", "C8")
  private val visiblePoints = List("P1", "P2", "P3", "P4", "P5", "P6", "P7", "P8")
  private val visibleEquipment = List("Sub1", "Sub2", "Sub3", "Sub4", "East", "West")
  private val visibleEndpoints = List("EastEndpoint", "WestEndpoint", "NukeEndpoint")
  private val visibleAgents = List("limited_regional_op")

  test("See only entities we're allowed to") {
    as("limited_regional_op") { ops =>

      val entNames = ops.getEntities().map(_.getName)

      val expected = visibleCommands ::: visiblePoints ::: visibleEquipment ::: visibleEndpoints ::: visibleAgents
      entNames.toSet should equal(expected.toSet)
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
        // TODO: commandLocks should be returned but individually filtered to only show commands we can see

        val commandLocks = ops.getCommandLocks()
        commandLocks should equal(Nil)

        val limitedCommands = ops.getCommands()
        val message = intercept[BadRequestException] {
          ops.createCommandExecutionLock(limitedCommands)
        }.getMessage

        message should include("C9")
        message should include("C12")
        // TODO: should we be able to see command lock creators for agents we can't see?
        message should include("system")
      }
    } finally {
      client.deleteCommandLock(lock)
    }
  }

  test("View Endpoints") {

    val allEndpoints = client.getEndpoints()

    as("limited_regional_op") { ops =>
      val endpoints = ops.getEndpoints()

      endpoints should equal(allEndpoints)

      // TODO: endpoint ownership should be filtered to show only visible elements for each user

      val pointNames = endpoints.map { _.getOwnerships.getPointsList.toList }.flatten.distinct
      pointNames.toSet should not equal (visiblePoints.toSet)

      val commandNames = endpoints.map { _.getOwnerships.getCommandsList.toList }.flatten.distinct
      commandNames.toSet should not equal (visibleCommands.toSet)
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
