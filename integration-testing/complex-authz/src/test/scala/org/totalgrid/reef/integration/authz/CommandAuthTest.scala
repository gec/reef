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
import org.totalgrid.reef.client.service.entity.EntityRelation

import scala.collection.JavaConversions._

@RunWith(classOf[JUnitRunner])
class CommandAuthTest extends AuthTestBase {

  private val DLRC_VISIBLE_COMMANDS = List("C1", "C2", "C3", "C5", "C7", "C10", "C11")

  test("Regional ops can view commands and command history") {
    as("regional_op") { regionalOp =>
      regionalOp.getCommands()

      regionalOp.getCommandHistory()
    }
  }

  test("Regional_op cant delete dlrc lock") {
    as("dlrc_app") { dlrc =>
      as("regional_op") { regionalOp =>
        val dlrcCommands = dlrc.getEntitiesWithType("DLRC").map { _.getName }
        dlrcCommands.toSet should equal(DLRC_VISIBLE_COMMANDS.toSet)
        val dlrcCommandName = dlrcCommands.head
        val cmd = dlrc.getCommandByName(dlrcCommandName)

        val lock = dlrc.createCommandDenialLock(List(cmd))
        try {
          unAuthed("regionalOp cant delete dlrc lock") {
            regionalOp.deleteCommandLock(lock)
          }
        } finally {
          dlrc.deleteCommandLock(lock)
        }
      }
    }
  }

  test("Test regional ops can only commands in West and East") {
    as("regional_op") { regionalOp =>

      val parents = List("West", "East")
      val relation = new EntityRelation("owns", List("Command"), true)
      val allowedCommands = regionalOp.getEntityRelationsForParentsByName(parents, List(relation))
        .map { _.getRelationsList.toList.map { _.getEntitiesList.toList }.flatten }.flatten.map { _.getName }

      val allCommands = regionalOp.getCommands().map { _.getName }
      val invalidCommands = allCommands.diff(allowedCommands)

      allowedCommands.sorted should equal(List("C1", "C2", "C3", "C4", "C5", "C6", "C7", "C8"))
      invalidCommands.sorted should equal(List("C10", "C11", "C12", "C9"))

      executeCommands(regionalOp, allowedCommands)

      cantExecuteCommands(regionalOp, invalidCommands)
    }
  }

  test("Test dlrc app can only execute dlrc commands") {
    as("dlrc_app") { dlrc =>

      val dlrcCommands = dlrc.getEntitiesWithType("DLRC").map { _.getName }
      dlrcCommands.toSet should equal(DLRC_VISIBLE_COMMANDS.toSet)
      val commands = dlrc.getCommands().map { _.getName }.diff(dlrcCommands)

      executeCommands(dlrc, dlrcCommands)

      cantExecuteCommands(dlrc, commands)

      dlrcCommands.foreach { cmdName =>
        val cmd = dlrc.getCommandByName(cmdName)
        dlrc.getCommandHistory(cmd) should not equal (List())
      }
    }
  }

}