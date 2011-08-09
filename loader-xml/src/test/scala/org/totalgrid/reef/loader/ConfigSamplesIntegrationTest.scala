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
import org.totalgrid.reef.util.BuildEnv
import org.totalgrid.reef.sapi.client.MockSyncOperations
import org.totalgrid.reef.sapi.client.Success
import org.totalgrid.reef.japi.Envelope

@RunWith(classOf[JUnitRunner])
class ConfigSamplesIntegrationTest extends FunSuite with ShouldMatchers {

  val samplesPath = BuildEnv.configPath + "assemblies/assembly-common/filtered-resources/samples/"
  def client = new MockSyncOperations((AnyRef) => Success(Envelope.Status.OK, List[AnyRef]()))

  test("samples/integration") {
    val c = client
    LoadManager.loadFile(c, samplesPath + "integration/config.xml", false, false, false, true) should equal(true)
    c.getPutQueue.size should equal(72)
  }
  test("samples/demo") {
    val c = client
    LoadManager.loadFile(c, samplesPath + "demo/configuration.demo.xml", false, false, false, true) should equal(true)
    c.getPutQueue.size should equal(295)
  }
  test("samples/two_substations") {
    val c = client
    LoadManager.loadFile(c, samplesPath + "two_substations/config.xml", false, false, false, true) should equal(true)
    c.getPutQueue.size should equal(310)
  }
}