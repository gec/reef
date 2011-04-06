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
package org.totalgrid.reef.shell.proto

import org.apache.felix.gogo.commands.{ Command, Argument, Option => GogoOption }

import org.totalgrid.reef.shell.proto.presentation.{ AlarmView }
import request.{ AlarmRequest }
import scala.collection.JavaConversions._

@Command(scope = "alarm", name = "alarm", description = "Prints alarms.")
class AlarmCommand extends ReefCommandSupport {

  @GogoOption(name = "-t", description = "Show alarms of type.", required = false, multiValued = true)
  var types: java.util.List[String] = null

  @GogoOption(name = "-u", description = "Show alarms of user.", required = false, multiValued = true)
  var users: java.util.List[String] = null

  def doCommand() = {
    val typList = Option(types).map(_.toList) getOrElse Nil
    val userList = Option(users).map(_.toList) getOrElse Nil

    AlarmView.printTable(AlarmRequest.getAlarms(userList, typList, reefSession).reverse)
  }
}