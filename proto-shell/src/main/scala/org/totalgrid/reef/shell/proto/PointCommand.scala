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

import org.apache.felix.gogo.commands.{ Argument, Command, Option => GogoOption }

import scala.collection.JavaConversions._

import org.totalgrid.reef.shell.proto.presentation.PointView

@Command(scope = "point", name = "list", description = "Prints point information")
class PointListCommand extends ReefCommandSupport {

  def doCommand() = {
    PointView.printPointTable(services.getAllPoints.toList)
  }
}

@Command(scope = "point", name = "commands", description = "Lists points with associated commands.")
class PointCommandsCommand extends ReefCommandSupport {

  @Argument(index = 0, name = "pointName", description = "Point name.", required = false, multiValued = false)
  var pointName: String = null

  @GogoOption(name = "-a", description = "Show points that don't have commands", required = false, multiValued = false)
  var showPointsWithoutCommands = false

  def doCommand() = {

    import org.totalgrid.reef.api.japi.client.rpc.impl.builders.EntityRequestBuilders
    import org.totalgrid.reef.api.proto.Model.Entity

    val query = Option(pointName) match {
      case Some(entName) => Entity.newBuilder().setName(pointName).addRelations(EntityRequestBuilders.getAllFeedBackCommands).build
      case None => EntityRequestBuilders.getAllPointsAndRelatedFeedbackCommands
    }
    var entities = services.getEntities(query).toList

    if (!showPointsWithoutCommands) entities = entities.filter { _.getRelationsCount > 0 }

    PointView.printPointsWithCommands(entities)
  }

}
