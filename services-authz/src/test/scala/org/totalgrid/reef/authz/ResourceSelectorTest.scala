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

@RunWith(classOf[JUnitRunner])
class ResourceSelectorTest extends AuthzTestBase {

  private def prepareEntities(): List[UUID] = {
    val entities = List(
      TestEntity("object1", List("something")),
      TestEntity("object2", List("else")),
      TestEntity("object3", List("nothing")),
      TestEntity("object4", List("something")))

    defineEntities(entities)
  }

  test("EntityTypeIncludes") {

    val uuids = prepareEntities()

    val matcher1 = new EntityTypeIncludes(List("something"))
    val matcher2 = new EntityTypeIncludes(List("else"))
    val matcher3 = new EntityTypeIncludes(List("nomatch"))

    matcher1.includes(uuids) should equal(List(Some(true), None, None, Some(true)))
    matcher2.includes(uuids) should equal(List(None, Some(true), None, None))
    matcher3.includes(uuids) should equal(List(None, None, None, None))

    matcher1.includes(uuids.slice(0, 2)) should equal(List(Some(true), None))
    matcher1.includes(uuids.slice(2, 4)) should equal(List(None, Some(true)))
  }

  test("EntityHasName") {

    val uuids = prepareEntities()

    val matcher1 = new EntityHasName(List("object1"))
    val matcher2 = new EntityHasName(List("object2", "object4"))
    val matcher3 = new EntityHasName(List("nomatch"))

    matcher1.includes(uuids) should equal(List(Some(true), None, None, None))
    matcher2.includes(uuids) should equal(List(None, Some(true), None, Some(true)))
    matcher3.includes(uuids) should equal(List(None, None, None, None))

    matcher1.includes(uuids.slice(0, 2)) should equal(List(Some(true), None))
    matcher1.includes(uuids.slice(2, 4)) should equal(List(None, None))
  }

  test("WildcardMatcher") {

    val uuids = prepareEntities()

    val matcher1 = new WildcardMatcher

    matcher1.includes(uuids) should equal(List(Some(true), Some(true), Some(true), Some(true)))
  }

}
