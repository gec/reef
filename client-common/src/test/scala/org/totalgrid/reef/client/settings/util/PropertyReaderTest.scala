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
package org.totalgrid.reef.client.settings.util

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

import scala.collection.JavaConversions._

@RunWith(classOf[JUnitRunner])
class PropertyReaderTest extends FunSuite with ShouldMatchers {

  def fname(s: String) = "src/test/resources/" + s + ".cfg"

  test("Single File loading (testCfgA)") {
    val propsA = PropertyReader.readFromFile(fname("testCfgA"))

    propsA.keys().toList.map { _.toString }.sorted should equal(List("prop1", "prop2", "prop3", "prop5"))

    propsA.get("prop1") should equal("A1")
    propsA.get("prop2") should equal("A2")
    propsA.get("prop3") should equal("")
    propsA.get("prop5") should equal("A5")

    propsA.get("random") should equal(null)
  }
  test("Single File loading (testCfgB)") {

    val propsB = PropertyReader.readFromFile(fname("testCfgB"))

    propsB.keys().toList.map { _.toString }.sorted should equal(List("prop1", "prop2", "prop3", "prop4"))

    propsB.get("prop1") should equal("B1")
    propsB.get("prop2") should equal("B2")
    propsB.get("prop3") should equal("B3")
    propsB.get("prop4") should equal("B4")

    propsB.get("random") should equal(null)
  }

  test("Property merging") {
    val aThenB = PropertyReader.readFromFiles(List(fname("testCfgA"), fname("testCfgB")))

    aThenB.keys().toList.map { _.toString }.sorted should equal(List("prop1", "prop2", "prop3", "prop4", "prop5"))

    aThenB.get("prop1") should equal("B1")
    aThenB.get("prop2") should equal("B2")
    aThenB.get("prop3") should equal("B3")
    aThenB.get("prop4") should equal("B4")
    aThenB.get("prop5") should equal("A5")

    val propsA = PropertyReader.readFromFile(fname("testCfgA"))
    val propsB = PropertyReader.readFromFile(fname("testCfgB"))

    PropertyLoading.mergeDictionaries(propsA, propsB).toMap should equal(aThenB.toMap)
  }

}
