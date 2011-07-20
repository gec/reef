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
package org.totalgrid.reef.models

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import org.squeryl._
import org.squeryl.PrimitiveTypeMode._

import java.util.UUID

object InstrumentedUUIDGenerator {
  private var _counts = 0

  def counts = {
    val ret = _counts
    _counts = 0
    ret
  }
  def increment = _counts += 1
  def reset = _counts = 0
}

/**
 * collects counts to newUUID
 */
trait InstrumentedUUIDShim extends UUIDGenerator {
  override def newUUID(): UUID = {
    InstrumentedUUIDGenerator.increment
    super.newUUID
  }
}
case class TestUUIDModel(name: String)
    extends ModelWithUUID
    with InstrumentedUUIDShim {

}

object TestSchema extends Schema {

  val uuids = table[TestUUIDModel]

  def reset() = {
    drop // its protected for some reason
    create
  }
}

@RunWith(classOf[JUnitRunner])
class ModelWithUUIDTest extends DatabaseUsingTestBase with RunTestsInsideTransaction {

  /**
   * @param actual how many calls to newUUID are made
   * @param expected how many I think should be made
   */
  def verifyUUIDCounts(actual: Int, expected: Int = -1) {
    InstrumentedUUIDGenerator.counts should equal(actual)
  }

  test("Insert and get generated UUID") {

    verifyUUIDCounts(0)
    TestSchema.reset
    verifyUUIDCounts(3, 0)

    val model1 = new TestUUIDModel("test0")
    verifyUUIDCounts(1)

    val entry0 = TestSchema.uuids.insert(model1)
    entry0.name should equal("test0")
    entry0.id should not equal ("")

    verifyUUIDCounts(0)

    val entry1 = TestSchema.uuids.insert(new TestUUIDModel("test1"))
    entry1.name should equal("test1")
    entry1.id should not equal ("")
    verifyUUIDCounts(1)

    entry0.id should not equal (entry1.id)

    val entry2 = TestSchema.uuids.lookup(entry0.id)
    entry2.get.name should equal("test0")
    entry2.get.id should equal(entry0.id)

    verifyUUIDCounts(3, 0)

    val entries = TestSchema.uuids.where(t => 1 === 1).toList
    verifyUUIDCounts(4, 0)

    entries.find(_.id == entry0.id).get should equal(entry0)
    entries.find(_.id == entry1.id).get should equal(entry1)

    verifyUUIDCounts(0)

    // add another entry and get them all again
    TestSchema.uuids.insert(new TestUUIDModel("test2"))
    verifyUUIDCounts(1)
    TestSchema.uuids.where(t => 1 === 1).toList
    verifyUUIDCounts(5, 0)

    // add another entry and get them all again
    TestSchema.uuids.insert(new TestUUIDModel("test3"))
    verifyUUIDCounts(1)
    TestSchema.uuids.where(t => 1 === 1).toList
    verifyUUIDCounts(6, 0)

    from(TestSchema.uuids)(t => select(t)).toList
    verifyUUIDCounts(6, 0)
  }

}