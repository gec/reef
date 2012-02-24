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

import org.apache.felix.gogo.commands.{ Argument, Command }

import scala.collection.JavaConversions._
import org.totalgrid.reef.shell.proto.presentation.CalculationView

@Command(scope = "calculation", name = "list", description = "List all calculations")
class CalculationListCommand extends ReefCommandSupport {

  def doCommand() = {
    CalculationView.printTable(services.getCalculations().toList)
  }
}

@Command(scope = "calculation", name = "view", description = "View a calculations details")
class CalculationViewCommand extends ReefCommandSupport {

  @Argument(index = 0, name = "name", description = "Name of the output point associated with the calculation", required = true, multiValued = false)
  var outputPointName: String = null

  def doCommand() = {
    CalculationView.printInspect(services.getCalculationForPointByName(outputPointName))
  }
}
