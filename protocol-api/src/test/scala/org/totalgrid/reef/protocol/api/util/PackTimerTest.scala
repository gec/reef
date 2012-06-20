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
package org.totalgrid.reef.protocol.api.util

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import net.agileautomata.executor4s.testing.MockExecutor
import net.agileautomata.executor4s._
import scala.util.Random
import net.agileautomata.commons.testing.SynchronizedList

@RunWith(classOf[JUnitRunner])
class PackTimerTest extends FunSuite with ShouldMatchers {

  case class TestObject(i: Int)

  test("Publishing on delay") {

    var pubbed = List.empty[List[TestObject]]
    val pubFunc = (l: List[TestObject]) => pubbed ::= l
    val exe = new MockExecutor

    val packTimer = new PackTimer(10, 10, pubFunc, Strand(exe))
    exe.isIdle should equal(true)

    packTimer.addEntry(TestObject(0))
    exe.numQueuedTimers should equal(1)

    packTimer.addEntry(TestObject(1))
    exe.numQueuedTimers should equal(1)
    exe.tick(100.milliseconds)
    exe.numQueuedTimers should equal(0)

    pubbed.head should equal(List(TestObject(0), TestObject(1)))
  }

  test("Publishing when full") {

    var pubbed = List.empty[List[TestObject]]
    val pubFunc = (l: List[TestObject]) => pubbed ::= l
    val exe = new MockExecutor

    val packTimer = new PackTimer(10, 10, pubFunc, Strand(exe))
    exe.numQueuedTimers should equal(0)

    (0 to 8).foreach { i =>
      packTimer.addEntry(TestObject(i))
      exe.numQueuedTimers should equal(1)
    }
    packTimer.addEntry(TestObject(9))
    exe.numQueuedTimers should equal(1)

    exe.tick(0.milliseconds)
    exe.numQueuedTimers should equal(0)
    pubbed.head should equal((0 to 9).map { TestObject(_) }.toList)
    exe.numQueuedTimers should equal(0)

  }

  test("Instant publish with large entry") {

    var pubbed = List.empty[List[TestObject]]
    val pubFunc = (l: List[TestObject]) => pubbed ::= l
    val exe = new MockExecutor

    val packTimer = new PackTimer(10, 10, pubFunc, Strand(exe))
    exe.numQueuedTimers should equal(0)

    packTimer.addEntries((0 to 20).map { TestObject(_) })
    exe.numQueuedTimers should equal(1)
    exe.tick(0.milliseconds)
    exe.numQueuedTimers should equal(0)
  }

  test("Threading stress test (single producer)") {

    val exe = Executors.newResizingThreadPool(7.seconds)

    val (packTimer, result, batchSizes) = fixture(exe)

    val original = (0 to 1000).toList

    // publish all of the entries using the same strand (maintains order)
    val testExe = Strand(exe)
    original.foreach { o => testExe.execute(packTimer.addEntry(o)) }

    result shouldBecome (original) within 10000
    batchSizes.get.sum should equal(original.size)
    batchSizes.get should not contain (0)
  }

  test("Threading stress test (multiple producers)") {

    val exe = Executors.newResizingThreadPool(7.seconds)

    val (packTimer, result, batchSizes) = fixture(exe)

    val original = (0 to 1000).toList

    // publish entries using thread pool (breaks ordering)
    original.foreach { o => exe.execute(packTimer.addEntry(o)) }

    result.value.awaitUntil(10000)(l => l.size == original.size)
    batchSizes.get.sum should equal(original.size)
    batchSizes.get should not contain (0)
  }

  private def fixture(exe: ExecutorService) = {

    val random = new Random
    val result = new SynchronizedList[Int]
    val batchSizes = new SynchronizedList[Int]
    val pub = (list: List[Int]) => {
      batchSizes.append(list.size)
      list.foreach { result.append(_) }
    }
    (new PackTimer[Int](10, 10, pub, Strand(exe)), result, batchSizes)

  }
}