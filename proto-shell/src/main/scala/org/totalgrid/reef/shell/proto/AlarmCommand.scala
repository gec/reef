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

import org.apache.felix.gogo.commands.{ Command, Argument, Option => GogoOption }

import org.totalgrid.reef.shell.proto.presentation.{ AlarmView }
import scala.collection.JavaConversions._
import org.totalgrid.reef.proto.Model.ReefUUID

@Command(scope = "alarm", name = "alarm", description = "Prints alarms.")
class AlarmCommand extends ReefCommandSupport {

  @GogoOption(name = "-t", description = "Show alarms of type.", required = false, multiValued = true)
  var types: java.util.List[String] = null

  @GogoOption(name = "-l", description = "Limit number of displayed events", required = false, multiValued = false)
  var limit: Int = 10

  def doCommand() = {
    val typList = Option(types).map(_.toList) getOrElse Nil

    val alarms = services.getActiveAlarms(typList, limit).toList.reverse

    AlarmView.printTable(alarms)
  }
}

@Command(scope = "alarm", name = "silence", description = "Silences an Alarm")
class AlarmSilenceCommand extends ReefCommandSupport {

  @Argument(index = 0, name = "id", description = "Alarm id", required = true, multiValued = false)
  private var id: String = null

  def doCommand() = {

    val alarm = services.getAlarm(id)

    val edittedAlarm = services.silenceAlarm(alarm)

    AlarmView.printTable(edittedAlarm :: Nil)
  }
}

@Command(scope = "alarm", name = "ack", description = "Acknowledges an Alarm")
class AlarmAcknowledgeCommand extends ReefCommandSupport {

  @Argument(index = 0, name = "id", description = "Alarm id", required = true, multiValued = false)
  private var id: String = null

  def doCommand() = {

    val alarm = services.getAlarm(id)

    val edittedAlarm = services.acknowledgeAlarm(alarm)

    AlarmView.printTable(edittedAlarm :: Nil)
  }
}

@Command(scope = "alarm", name = "remove", description = "Removes an Alarm")
class AlarmRemoveCommand extends ReefCommandSupport {

  @Argument(index = 0, name = "id", description = "Alarm id", required = true, multiValued = false)
  private var id: String = null

  def doCommand() = {

    val alarm = services.getAlarm(id)

    val edittedAlarm = services.removeAlarm(alarm)

    AlarmView.printTable(edittedAlarm :: Nil)
  }
}