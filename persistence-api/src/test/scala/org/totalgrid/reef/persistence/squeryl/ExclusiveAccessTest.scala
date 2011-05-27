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
package org.totalgrid.reef.persistence.squeryl

import org.squeryl.{ Schema, Table, KeyedEntity }
import org.squeryl.PrimitiveTypeMode._

import scala.actors.Actor._

import org.totalgrid.reef.util.{ Timing, SyncVar }
import ExclusiveAccess._

class AccessTable(var value: Int) extends KeyedEntity[Long] {
  val id: Long = 0
}

object AccessSchema extends Schema {

  val acs = table[AccessTable]

  def reset() = {
    drop // its protected for some reason
    create
  }
}

import org.scalatest.{ FunSuite, BeforeAndAfterAll, BeforeAndAfterEach }
import org.scalatest.matchers.ShouldMatchers

abstract class ExclusiveAccessTest extends FunSuite with ShouldMatchers with BeforeAndAfterAll with BeforeAndAfterEach {

  def getDbInfo: DbInfo

  override def beforeAll() {
    import org.totalgrid.reef.persistence.squeryl.{ DbConnector, DbInfo }
    DbConnector.connect(getDbInfo)
  }

  override def beforeEach() {
    transaction { AccessSchema.reset }
  }

  def testFun(id: Long, fun: AccessTable => Boolean) = (f: AccessTable => AccessTable) => {
    exclusiveAccess(AccessSchema.acs, id, (a: AccessTable) => a, fun)(a => {
      f(a)
    })
  }
  def testFunMulti(ids: List[Long], fun: AccessTable => Boolean) = (f: List[AccessTable] => List[AccessTable]) => {
    exclusiveAccess(AccessSchema.acs, ids, (a: AccessTable) => a, fun)(a => {
      f(a)
    })
  }

  test("Precondition not met") {
    val id = transaction {
      AccessSchema.acs.insert(new AccessTable(0)).id
    }
    val accessor = testFun(id, _.value == 1)

    intercept[AcquireConditionNotMetException] {
      accessor(a => throw new Exception)
    }
  }
  test("Precondition not met (multi)") {
    transaction {
      AccessSchema.acs.insert(new AccessTable(0)).id
      AccessSchema.acs.insert(new AccessTable(0)).id
      AccessSchema.acs.insert(new AccessTable(0)).id
    }
    val accessor = testFunMulti(List(0, 1, 2), _.value == 1)

    intercept[AcquireConditionNotMetException] {
      accessor(a => throw new Exception)
    }
  }

  test("PostCondition not met") {
    val id = transaction {
      AccessSchema.acs.insert(new AccessTable(1)).id
    }
    val accessor = testFun(id, _.value == 1)

    intercept[AcquireConditionStillValidException] {
      accessor(a => a)
    }

    // correct usage
    accessor(a => {
      a.value = 0
      AccessSchema.acs.update(a)
      a
    })

  }

  test("PostCondition not met (multi)") {

    val ids: List[Long] = transaction {
      AccessSchema.acs.insert(new AccessTable(1)).id ::
        AccessSchema.acs.insert(new AccessTable(1)).id ::
        AccessSchema.acs.insert(new AccessTable(1)).id :: Nil
    }
    val accessor = testFunMulti(ids, _.value == 1)

    intercept[AcquireConditionStillValidException] {
      accessor(a => a)
    }

    // correct usage
    accessor(list => list.map { a =>
      a.value = 0
      AccessSchema.acs.update(a)
      a
    })

  }

  test("Object doesnt exist") {

    val accessor = testFun(999, _.value == 1)

    intercept[ObjectMissingException] {
      accessor(a => a)
    }
  }
  test("Object doesnt exist (multi)") {

    val accessor = testFunMulti(List(998, 999), _.value == 1)

    intercept[ObjectMissingException] {
      accessor(a => a)
    }
  }

  test("Row is locked") {
    val id = transaction {
      AccessSchema.acs.insert(new AccessTable(0)).id
    }

    val accessor = testFun(id, _.value == 0)

    val blockedAccessingTime = new SyncVar(-1: Long)

    var blockedTime = -1: Long

    accessor(a => {
      actor {
        Timing.time(blockedAccessingTime.update _) {
          intercept[AcquireConditionNotMetException] {
            // this function should block while the main transaction is "procssing"
            // then fail because the precondition of value == 0 is now false
            accessor(a => throw new Exception)
          }
        }
      }.start
      Timing.time({ t: Long => { blockedTime = t } }) { Thread.sleep(100) } // fake a long running process
      a.value = 1
      AccessSchema.acs.update(a)
      a
    })

    blockedAccessingTime.waitWhile(-1) should equal(true)

    //println("blocked for: " + blockedAccessingTime.current)
    //blockedAccessingTime.current should (be >= blockedTime)
  }

  test("Other rows unlocked") {
    val id1 = transaction {
      AccessSchema.acs.insert(new AccessTable(0)).id
    }
    val id2 = transaction {
      AccessSchema.acs.insert(new AccessTable(0)).id
    }

    val unBlock = new SyncVar(false)

    val firstEntry = -1

    testFun(id1, _.value == 0)(a => {
      actor {
        // sub actor gets a lock on a different row and then
        // unblocks the original transaction
        testFun(id2, _.value == 0)(a => {
          unBlock.update(true)
          a.value = 1
          a
        })
      }.start
      unBlock.waitUntil(true)
      a.value = 1
      AccessSchema.acs.update(a)
      a
    })
  }

}