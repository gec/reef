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
import java.util.UUID
import org.scalatest.TestFailedException

@RunWith(classOf[JUnitRunner])
class AuthzFilteringTest extends AuthzTestBase {

  val denyAll = Permission.denyAllPermission("deny *")

  def prepare() = {
    val mock = new ResourceSpecificFiltering {
      var usedResourceFilter = false

      def resourceSpecificFiltering[A](applicablePermissions: List[Permission], pairs: List[(A, List[UUID])]) = {
        usedResourceFilter = true
        pairs.map { x => Allowed[A](x._1, applicablePermissions.head) }
      }
    }
    (new AuthzFiltering(mock), mock)
  }

  def noUuids: List[List[UUID]] = fail("should not access uuids")

  def allDenied[A](results: List[FilteredResult[A]]) {
    results.map { _.isAllowed }.distinct should equal(List(false))
  }

  def allAllowed[A](results: List[FilteredResult[A]]) = {
    results.map { _.isAllowed }.distinct should equal(List(true))
    results
  }

  test("Deny returned if no rule matches") {

    val (filter, resourceFilter) = prepare()

    val noPermissions = Nil
    allDenied(filter.filter(noPermissions, "service", "action", List(1), noUuids))

    val unmatchingPermissions = List(new Permission(true, "service", "otheraction"), new Permission(true, "wrongservice", "action"))
    allDenied(filter.filter(unmatchingPermissions, "service", "action", List(1), noUuids))
  }

  test("Return first match") {

    val (filter, resourceFilter) = prepare()

    val acceptFirst = List(new Permission(true, "service", "action"), new Permission(false, "service", "action"))
    allAllowed(filter.filter(acceptFirst, "service", "action", List(1), noUuids))

    val denyFirst = acceptFirst.reverse
    allDenied(filter.filter(denyFirst, "service", "action", List(1), noUuids))
  }

  test("Wildcard matches") {

    val (filter, resourceFilter) = prepare()

    val wildcardService = List(new Permission(true, "*", "action"))
    allAllowed(filter.filter(wildcardService, "service", "action", List(1), noUuids))

    val wildcardAction = List(new Permission(true, "service", "*"))
    allAllowed(filter.filter(wildcardAction, "service", "action", List(1), noUuids))

    val bothWildCard = List(new Permission(true, "*", "*"))
    allAllowed(filter.filter(wildcardAction, "service", "action", List(1), noUuids))
  }

  test("Results are returned in same order as request") {

    val (filter, resourceFilter) = prepare()

    val accept = List(new Permission(true, "service", "action"))
    val input = List(1, 2, 3, 4, 5)

    val results = allAllowed(filter.filter(accept, "service", "action", input, noUuids))

    results.map { _.result.get } should equal(input)
  }

  test("Uuids are not requested unless we are using a resource specific rule") {

    val (filter, resourceFilter) = prepare()

    val accept = List(new Permission(true, "otherservice", "action"), new Permission(true, List("service"), List("action"), new EntityHasName(List("a"))))

    allAllowed(filter.filter(accept, "otherservice", "action", List(1), noUuids))

    intercept[TestFailedException] {
      allAllowed(filter.filter(accept, "service", "action", List(1), noUuids))
    }
  }

  test("Resource specific permissions are delegated to resourceFilter") {

    val (filter, resourceFilter) = prepare()

    val accept = List(new Permission(true, List("service"), List("action"), new EntityHasName(List("a"))))

    resourceFilter.usedResourceFilter should equal(false)
    allAllowed(filter.filter(accept, "service", "action", List(1), List(List(UUID.randomUUID()))))

    resourceFilter.usedResourceFilter should equal(true)
  }

}
