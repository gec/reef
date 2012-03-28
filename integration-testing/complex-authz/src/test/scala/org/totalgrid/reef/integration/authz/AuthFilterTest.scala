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

@RunWith(classOf[JUnitRunner])
class AuthFilterTest extends AuthTestBase {

  override val modelFile = "../../assemblies/assembly-common/filtered-resources/samples/authorization/config.xml"

  test("Regional command test") {
    as("regional_op") { ops =>
      val result = ops.authFilterLookup("create", "command_lock", List(Entity.newBuilder.addTypes("Command").build()))

      val results = result.await
      val (allowed, denied) = results.partition(_.getAllowed)
      val allowedNames = allowed.map(_.getEntity.getName)
      val deniedNames = denied.map(_.getEntity.getName)

      val allowCheck = List("C1", "C2", "C3", "C4", "C5", "C6", "C7", "C8")
      val denyCheck = List("C9", "C10", "C11", "C12")
      allowedNames.filterNot(allowCheck.contains) should equal(Nil)
      deniedNames.filterNot(denyCheck.contains) should equal(Nil)
    }
  }

}
