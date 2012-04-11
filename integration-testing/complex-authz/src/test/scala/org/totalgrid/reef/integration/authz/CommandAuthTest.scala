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

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.totalgrid.reef.client.sapi.rpc.AllScadaService
import org.totalgrid.reef.client.service.entity.EntityRelation

import scala.collection.JavaConversions._

@RunWith(classOf[JUnitRunner])
class CommandAuthTest extends AuthTestBase {

  override val modelFile = "../../assemblies/assembly-common/filtered-resources/samples/authorization/config.xml"

  test("Regional ops can view commands and command history") {
    as("regional_op") { regionalOp =>
      regionalOp.getCommands().await

      regionalOp.getCommandHistory().await
    }
  }

  test("Regional_op cant delete dlrc lock") {
    as("dlrc_app") { dlrc =>
      as("regional_op") { regionalOp =>
        val dlrcCommandName = dlrc.getEntitiesWithType("DLRC").await.map { _.getName }.head
        val cmd = dlrc.getCommandByName(dlrcCommandName).await

        val lock = dlrc.createCommandDenialLock(List(cmd)).await
        try {
          unAuthed("regionalOp cant delete dlrc lock") {
            regionalOp.deleteCommandLock(lock).await
          }
        } finally {
          dlrc.deleteCommandLock(lock).await
        }
      }
    }
  }

  test("Test regional ops can only commands in West and East") {
    as("regional_op") { regionalOp =>

      val parents = List("West", "East")
      val relation = new EntityRelation("owns", List("Command"), true)
      val allowedCommands = regionalOp.getEntityRelationsForParentsByName(parents, List(relation)).await
        .map { _.getRelationsList.toList.map { _.getEntitiesList.toList }.flatten }.flatten.map { _.getName }

      val allCommands = regionalOp.getCommands().await.map { _.getName }
      val invalidCommands = allCommands.diff(allowedCommands)

      allowedCommands.sorted should equal(List("C1", "C2", "C3", "C4", "C5", "C6", "C7", "C8"))
      invalidCommands.sorted should equal(List("C10", "C11", "C12", "C9"))

      executeCommands(regionalOp, allowedCommands)

      cantExecuteCommands(regionalOp, invalidCommands)
    }
  }

  test("Test dlrc app can only execute dlrc commands") {
    as("dlrc_app") { dlrc =>

      val dlrcCommands = dlrc.getEntitiesWithType("DLRC").await.map { _.getName }
      val commands = dlrc.getCommands().await.map { _.getName }.diff(dlrcCommands)

      executeCommands(dlrc, dlrcCommands)

      cantExecuteCommands(dlrc, commands)
    }
  }

  private def executeCommands(service: AllScadaService, cmds: List[String]) {
    cmds.foreach { cmdName => executeCommand(service, cmdName) }
  }

  private def cantExecuteCommands(service: AllScadaService, cmds: List[String]) {
    cmds.foreach { cmdName =>
      unAuthed("Expected executing command: " + cmdName + " to be unauthorized") {
        executeCommand(service, cmdName)
      }
    }
  }

  private def executeCommand(service: AllScadaService, cmdName: String) {
    val cmd = service.getCommandByName(cmdName).await
    val lock = service.createCommandExecutionLock(cmd).await
    try {
      service.executeCommandAsControl(cmd).await
    } finally {
      service.deleteCommandLock(lock).await
    }
  }
}