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

import org.apache.felix.gogo.commands.{ Command, Argument }

import org.totalgrid.reef.shell.proto.presentation.{ MeasView }

import scala.collection.JavaConversions._
import org.totalgrid.reef.api.request.ReefUUID

@Command(scope = "meas", name = "meas", description = "Prints all measurements or a specified measurement.")
class MeasCommand extends ReefCommandSupport {

  @Argument(index = 0, name = "name", description = "Measurement name.", required = false, multiValued = false)
  var name: String = null

  def doCommand() = {
    Option(name) match {
      case Some(measName) => MeasView.printInspect(services.getMeasurementByName(name))
      case None =>
        val points = services.getAllPoints
        MeasView.printTable(services.getMeasurementsByPoints(points).toList)
    }
  }
}

@Command(scope = "meas", name = "from", description = "Prints measurements under an entity.")
class MeasFromCommand extends ReefCommandSupport {

  @Argument(index = 0, name = "parentId", description = "Parent entity name.", required = true, multiValued = false)
  var parentName: String = null

  def doCommand(): Unit = {

    val entity = services.getEntityByName(parentName)
    val pointEntites = services.getEntityRelatedChildrenOfType(new ReefUUID(entity.getUid), "owns", "Point")

    MeasView.printTable(services.getMeasurementsByNames(pointEntites.map { _.getName }).toList)
  }
}

@Command(scope = "meas", name = "hist", description = "Prints measurements under an entity.")
class MeasHistCommand extends ReefCommandSupport {

  @Argument(index = 0, name = "name", description = "Measurement name.", required = true, multiValued = false)
  var name: String = null

  @Argument(index = 1, name = "count", description = "Number of previous updates.", required = false, multiValued = false)
  var count: Int = 10

  def doCommand(): Unit = {

    val point = services.getPointByName(name)
    MeasView.printTable(services.getMeasurementHistory(point, count).toList)
  }
}