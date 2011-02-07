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
package org.totalgrid.reef.shell.proto.request

import org.totalgrid.reef.protoapi.client.SyncOperations
import org.totalgrid.reef.proto.Alarms.{ AlarmList, AlarmSelect }

import scala.collection.JavaConversions._
import RequestFailure._

object AlarmRequest {

  def getAlarms(users: List[String], types: List[String], client: SyncOperations) = {
    val alarms = interpretAs("Bad request.") {
      client.getOneOrThrow(AlarmList.newBuilder.setSelect(buildSelect(users, types)).build).getAlarmsList.toList
    }
    if (alarms.isEmpty) throw RequestFailure("No alarms found.")

    alarms
  }

  def buildSelect(users: List[String], types: List[String]) = {
    AlarmSelect.newBuilder.setEventSelect(EventRequest.buildSelect(users, types)).build
  }
}