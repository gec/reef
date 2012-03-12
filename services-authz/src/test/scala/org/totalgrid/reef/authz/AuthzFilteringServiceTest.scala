/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the GNU Affero General Public License
 * Version 3.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.authz

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

@RunWith(classOf[JUnitRunner])
class AuthzFilteringServiceTest extends FunSuite with ShouldMatchers {

  case class TestEntity(name: String, types: List[String]) extends AuthEntity

  val denyAll = Permission(false, "*", "*", List(ResourceSet(List(new AllMatcher))))

  def permissions(p: Permission) = p :: List(denyAll)

  test("Filter Entities") {

    val matcher = new EntityTypeIncludesMatcher(List("something"))
    val rs = ResourceSet(List(matcher))
    val allowPermission = Permission(true, "test", "get", List(rs))

    val permissions = List(allowPermission, denyAll)

    val entities = List(
      (TestEntity("object", List("something")), true),
      (TestEntity("object", List("nothing")), false))

    val filtered = AuthzFilteringService.filterByEntity(permissions, entities)

    filtered.map { _.result } should equal(List(Some(true), None))
    filtered.map { _.permission } should equal(List(allowPermission, denyAll))

  }
}
