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
import org.totalgrid.reef.client.service.proto.Model.{ ReefUUID, Entity }
import org.totalgrid.reef.client.service.proto.Auth.AuthFilterResult

@RunWith(classOf[JUnitRunner])
class AuthFilterTest extends AuthTestBase {

  override val modelFile = "../../assemblies/assembly-common/filtered-resources/samples/authorization/config.xml"

  def checkLookup(results: List[AuthFilterResult], allowCheck: List[String], denyCheck: List[String]) {
    val (allowed, denied) = results.partition(_.getAllowed)
    val allowedNames = allowed.map(_.getEntity.getName)
    val deniedNames = denied.map(_.getEntity.getName)

    allowedNames.toSet should equal(allowCheck.toSet)
    deniedNames.toSet should equal(denyCheck.toSet)
  }

  def checkAllowed(results: List[AuthFilterResult], allowCheck: List[String]) {
    val (allowed, _) = results.partition(_.getAllowed)
    val allowedNames = allowed.map(_.getEntity.getName)
    allowedNames.toSet should equal(allowCheck.toSet)
  }

  test("Parent selector") {
    as("system") { ops =>

      val set = ops.getPermissionSet("regional")
      val allowCheck = List("C1", "C2", "C3", "C4", "C5", "C6", "C7", "C8")
      val denyCheck = List("C9", "C10", "C11", "C12")
      val result = ops.getAuthFilterResults("create", "command_lock", List(Entity.newBuilder.addTypes("Command").build()), set)
      checkLookup(result, allowCheck, denyCheck)
    }
  }

  test("Type selector") {
    as("system") { ops =>

      val set = ops.getPermissionSet("non_critical")
      val allowCheck = List("C2", "C3", "C6", "C7", "C10", "C11")
      val denyCheck = List("C1", "C4", "C5", "C8", "C9", "C12")
      val result = ops.getAuthFilterResults("create", "command_lock", List(Entity.newBuilder.addTypes("Command").build()), set)
      checkLookup(result, allowCheck, denyCheck)
    }
  }

  test("Self selector") {
    val set = as("system") { _.getPermissionSet("user_role") }
    as("non_critical_op") { ops =>

      val allowCheck = List("non_critical_op")
      val result = ops.getAuthFilterResults("update", "agent_password", List(Entity.newBuilder.addTypes("Agent").build()), set)
      checkAllowed(result, allowCheck)
    }
  }

  test("No read leak") {
    val set = as("system") { _.getPermissionSet("regional") }
    as("limited_regional_op") { ops =>

      val allowCheck = List("C1", "C2", "C3", "C4", "C5", "C6", "C7", "C8")
      val denyCheck = List()
      val result = ops.getAuthFilterResults("create", "command_lock", List(Entity.newBuilder.addTypes("Command").build()), set)
      checkLookup(result, allowCheck, denyCheck)
    }
  }

}
