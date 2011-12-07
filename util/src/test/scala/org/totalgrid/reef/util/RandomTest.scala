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
package org.totalgrid.reef.util

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import collection.immutable.Map
import collection.immutable.Range.Inclusive
import org.totalgrid.reef.test.MockitoFunSuite
import com.weiglewilczek.slf4s.Logging

@RunWith(classOf[JUnitRunner])
class RandomTest extends MockitoFunSuite with Logging {
  test("generate with range 1-2") {
    val numberOfRandomIntsToGenerate: Int = 2
    val expectedRange: Inclusive = 1 to numberOfRandomIntsToGenerate
    var valueToCountMap: Map[Int, Int] = Map.empty
    expectedRange.foreach(idx => valueToCountMap += idx -> 0)
    valueToCountMap.size should be(numberOfRandomIntsToGenerate)

    val iterations: Int = 10000
    for (i <- 0 until iterations) {
      val value: Int = Random.generate(expectedRange)
      valueToCountMap = incrementCount(valueToCountMap, value)
    }

    printCounts(valueToCountMap, expectedRange, iterations)
    assertCounts(iterations, numberOfRandomIntsToGenerate, valueToCountMap)
  }

  test("generate with range 4-7") {
    val expectedRange = 4 to 7
    val numberOfRandomIntsToGenerate: Int = expectedRange.toSeq.size
    var valueToCountMap: Map[Int, Int] = Map.empty
    expectedRange.foreach(idx => valueToCountMap += idx -> 0)
    valueToCountMap.size should be(numberOfRandomIntsToGenerate)

    val iterations: Int = 10000
    for (i <- 0 until iterations) {
      val value: Int = Random.generate(expectedRange)
      valueToCountMap = incrementCount(valueToCountMap, value)
    }

    printCounts(valueToCountMap, expectedRange, iterations)
    assertCounts(iterations, numberOfRandomIntsToGenerate, valueToCountMap)
  }

  test("generate 2") {
    val numberOfRandomIntsToGenerate: Int = 2
    val expectedRange: Inclusive = 1 to numberOfRandomIntsToGenerate
    var valueToCountMap: Map[Int, Int] = Map.empty
    expectedRange.foreach(idx => valueToCountMap += idx -> 0)
    valueToCountMap.size should be(numberOfRandomIntsToGenerate)

    val iterations: Int = 10000
    for (i <- 0 until iterations) {
      val value: Int = Random.generate(numberOfRandomIntsToGenerate)
      valueToCountMap = incrementCount(valueToCountMap, value)
    }

    printCounts(valueToCountMap, expectedRange, iterations)
    assertCounts(iterations, numberOfRandomIntsToGenerate, valueToCountMap)
  }

  test("generate 2 ZeroBased") {
    val numberOfRandomIntsToGenerate: Int = 2
    val expectedRange = 0 until numberOfRandomIntsToGenerate
    var valueToCountMap: Map[Int, Int] = Map.empty
    expectedRange.foreach(idx => valueToCountMap += idx -> 0)
    valueToCountMap.size should be(numberOfRandomIntsToGenerate)

    val iterations: Int = 10000
    for (i <- 0 until iterations) {
      val value: Int = Random.generateZeroBased(numberOfRandomIntsToGenerate)
      valueToCountMap = incrementCount(valueToCountMap, value)
    }

    printCounts(valueToCountMap, expectedRange, iterations)
    assertCounts(iterations, numberOfRandomIntsToGenerate, valueToCountMap)
  }

  test("generate 4") {
    val numberOfRandomIntsToGenerate: Int = 4
    val expectedRange: Inclusive = 1 to numberOfRandomIntsToGenerate
    var valueToCountMap: Map[Int, Int] = Map.empty
    expectedRange.foreach(idx => valueToCountMap += idx -> 0)
    valueToCountMap.size should be(numberOfRandomIntsToGenerate)

    val iterations: Int = 10000
    for (i <- 0 until iterations) {
      val value: Int = Random.generate(numberOfRandomIntsToGenerate)
      valueToCountMap = incrementCount(valueToCountMap, value)
    }

    printCounts(valueToCountMap, expectedRange, iterations)
    assertCounts(iterations, numberOfRandomIntsToGenerate, valueToCountMap)
  }

  test("generate 7") {
    val numberOfRandomIntsToGenerate: Int = 7
    val expectedRange: Inclusive = 1 to numberOfRandomIntsToGenerate
    var valueToCountMap: Map[Int, Int] = Map.empty
    expectedRange.foreach(idx => valueToCountMap += idx -> 0)
    valueToCountMap.size should be(numberOfRandomIntsToGenerate)

    val iterations: Int = 10000
    for (i <- 0 until iterations) {
      val value: Int = Random.generate(numberOfRandomIntsToGenerate)
      valueToCountMap = incrementCount(valueToCountMap, value)
    }

    printCounts(valueToCountMap, expectedRange, iterations)
    assertCounts(iterations, numberOfRandomIntsToGenerate, valueToCountMap)
  }

  def incrementCount(valueToCountMap: Map[Int, Int], value: Int): Map[Int, Int] =
    {
      val count: Option[Int] = valueToCountMap.get(value)
      count.isDefined should be(true)
      valueToCountMap + (value -> (count.get + 1))
    }

  def printCounts(countOfValuesMap: Map[Int, Int], range: Range, iterations: Int) {
    logger.debug("")
    range.foreach(value =>
      {
        val count: Int = countOfValuesMap.get(value).getOrElse(throw new RuntimeException)
        logger.debug("count(" + value + "): " + count + ", " + (count / iterations.asInstanceOf[Double]) + "%")
      })
  }

  def assertCounts(iterations: Int, numberOfRandomIntsToGenerate: Int, countOfValuesMap: Map[Int, Int]) {
    val expectedCount = iterations.asInstanceOf[Double] / numberOfRandomIntsToGenerate
    val allowedVariance = expectedCount * .1d
    countOfValuesMap.foreach({
      case (value, count) => count.asInstanceOf[Double] should be(expectedCount plusOrMinus allowedVariance)
    })
  }

}