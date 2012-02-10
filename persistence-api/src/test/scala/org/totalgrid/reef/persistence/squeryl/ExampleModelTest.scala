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

import org.scalatest.{ FunSuite, BeforeAndAfterAll, BeforeAndAfterEach }
import org.scalatest.matchers.ShouldMatchers

import org.squeryl.{ Schema, Table, KeyedEntity }
import org.squeryl.PrimitiveTypeMode._

class Foo(val value: String, val data: Long = 0) extends KeyedEntity[Long] {
  val id: Long = 0
}

object FooSchema extends Schema {
  val foos = table[Foo]

  def reset() = {
    drop // its protected for some reason
    create
  }
}

abstract class ExampleModelTest extends FunSuite with ShouldMatchers with BeforeAndAfterAll with BeforeAndAfterEach {

  def getDbInfo: DbInfo

  lazy val db: DbConnection = DbConnector.connect(getDbInfo)

  override def beforeEach() {
    db.transaction { FooSchema.reset }
  }

  test("FooSchema1") {
    db.transaction {
      FooSchema.foos.insert(new Foo("test"))

      val foos = FooSchema.foos.where(f => f.value === "test")
      foos.size should equal(1)
    }
  }

  test("FooSchema2") {
    db.transaction {
      val foo1 = FooSchema.foos.insert(new Foo("test"))
      val foo2 = FooSchema.foos.insert(new Foo("testa"))

      val foos = FooSchema.foos.where(f => f.value === "test")
      foos.size should equal(1)
    }
  }

  // the following 3 tests prove the nested transaction bug with squeryl
  // https://github.com/max-l/Squeryl/issues#issue/79
  test("Excepting out of nested transaction") {
    db.transaction {
      FooSchema.reset
      val foo1 = FooSchema.foos.insert(new Foo("test"))
      FooSchema.foos.where(f => f.value === "test").size should equal(1)

      intercept[Exception] {
        doSomething(true)
      }

      FooSchema.foos.where(f => f.value === "test").size should equal(1)
    }
  }

  test("Returning out of nested transaction") {
    db.transaction {
      FooSchema.reset
      val foo1 = FooSchema.foos.insert(new Foo("test"))
      FooSchema.foos.where(f => f.value === "test").size should equal(1)

      doSomething(false)

      FooSchema.foos.where(f => f.value === "test").size should equal(1)
    }
  }

  test("Returning out of sibling transaction") {
    db.transaction {
      FooSchema.reset
      val foo1 = FooSchema.foos.insert(new Foo("test"))
      FooSchema.foos.where(f => f.value === "test").size should equal(1)

      doSomething(false)
    }
    db.transaction {
      FooSchema.foos.where(f => f.value === "test").size should equal(1)
    }
  }

  test("Empty list inhibition with iterable") {
    db.transaction {
      FooSchema.reset
      val foo1 = FooSchema.foos.insert(new Foo("test1"))

      val map1 = Map("test1" -> "test1")
      val result1 = FooSchema.foos.where(f => f.value in map1.keys).toList
      result1 should equal(List(foo1))

      //Select
      //  Foo1.data as Foo1_data,
      //  Foo1.id as Foo1_id,
      //  Foo1.value as Foo1_value
      //From
      //  Foo Foo1
      //Where
      //  (Foo1.value in ())   <-- EMPTY!
      intercept[Exception] {
        val map2 = Map.empty[String, String]
        val result2 = FooSchema.foos.where(f => f.value in map2.keys).toList
        result2 should equal(Nil)
      }
    }
  }

  test("Empty list inhibition inside join") {
    db.transaction {
      FooSchema.reset
      val foo1 = FooSchema.foos.insert(new Foo("test1", 100))
      val foo2 = FooSchema.foos.insert(new Foo("test1", 150))
      val foo3 = FooSchema.foos.insert(new Foo("test2", 99))
      val foo4 = FooSchema.foos.insert(new Foo("test2", 40))

      def maxData(list: Iterable[String]) =
        from(FooSchema.foos)(m =>
          where((m.value in list))
            groupBy (m.value)
            compute (max(m.data)))
      def byId(list: Iterable[String]) = join(FooSchema.foos, maxData(list))((m, mmt) =>
        select(m)
          on ((m.value === mmt.key) and (m.data === mmt.measures)))

      val result1 = byId(List("test1", "test2")).toList
      result1 should equal(List(foo2, foo3))

      val result2 = byId(List("test2")).toList
      result2 should equal(List(foo3))

      val result3 = byId(Nil).toList
      result3 should equal(Nil)

      intercept[Exception] {
        // throws an exception because of empty query
        val map = Map.empty[String, String]
        val result4 = byId(map.keys).toList
        result4 should equal(Nil)
      }
    }
  }

  def doSomething(except: Boolean): Int = {
    db.transaction {
      if (except) throw new Exception()
      return 1
    }
  }

}