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
package org.totalgrid.reef.shell.proto

import org.totalgrid.reef.benchmarks.BenchmarksRunner
import org.totalgrid.reef.client.settings.util.PropertyReader
import org.apache.felix.gogo.commands.{ Argument, Command }

@Command(scope = "reef", name = "benchmark", description = "Runs the benchmark suite")
class BenchmarkCommand extends ReefCommandSupport {

  @Argument(index = 0, name = "configFile", description = "Configuration file path", required = false, multiValued = false)
  var configFile: String = "etc/org.totalgrid.reef.benchmarks.cfg"

  override def doCommand(): Unit = {

    val testOptions = PropertyReader.readFromFile(configFile)

    BenchmarksRunner.runAllTests(connection, reefClient, testOptions)
  }
}
