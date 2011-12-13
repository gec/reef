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
package org.totalgrid.reef.loader

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ConfigSamplesIntegrationTest extends FunSuite with ShouldMatchers {

  val samplesPath = "../" + "assemblies/assembly-common/filtered-resources/samples/"

  private def loadFile(fileName: String, numExpected: Int) = {
    val (loader, valid) = LoadManager.prepareModelCache(fileName, false, 25)
    valid should equal(true)
    loader.size should equal(numExpected)
  }

  test("samples/integration") {
    loadFile(samplesPath + "integration/config.xml", 80)
  }

  test("samples/demo") {
    loadFile(samplesPath + "demo/configuration.demo.xml", 294)
  }

  test("samples/two_substations") {
    loadFile(samplesPath + "two_substations/config.xml", 301)
  }

  test("samples/mainstreet") {
    loadFile(samplesPath + "mainstreet/config.xml", 637)
  }

  test("dnp3-sample") {
    loadFile("../" + "protocol-dnp3/src/test/resources/sample-model.xml", 75)
  }
}