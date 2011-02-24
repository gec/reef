/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Green Energy Corp licenses this file
 * to you under the GNU Affero General Public License Version 3.0
 * (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.shell.admin

import org.apache.felix.gogo.commands.{ Command, Argument }
import org.apache.karaf.shell.console.OsgiCommandSupport

import org.totalgrid.reef.metrics.MetricsSink

@Command(scope = "metrics", name = "dump", description = "Output the metrics to a csv file (metrics.csv by default)")
class DumpMetricsToCSV extends OsgiCommandSupport {

  @Argument(index = 0, name = "fileName", description = "Output file path", required = false, multiValued = false)
  private var fileName: String = "metrics.csv"

  override def doExecute(): Object = {
    MetricsSink.dumpToFile(fileName)
    null
  }
}

@Command(scope = "metrics", name = "reset", description = "Reset metrics counters")
class ResetMetrics extends OsgiCommandSupport {

  override def doExecute(): Object = {
    MetricsSink.resetAll()
    null
  }
}