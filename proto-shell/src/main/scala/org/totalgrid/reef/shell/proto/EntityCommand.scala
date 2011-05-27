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

import scala.collection.JavaConversions._
import org.totalgrid.reef.shell.proto.presentation.{ EntityView }
import org.totalgrid.reef.japi.request.builders.EntityRequestBuilders

@Command(scope = "entity", name = "entity", description = "Prints all entities or information on a specific entity.")
class EntityCommand extends ReefCommandSupport {

  @Argument(index = 0, name = "Entity Name", description = "Entity name.", required = false, multiValued = false)
  var entityName: String = null

  def doCommand() = {
    Option(entityName) match {
      case Some(entId) => EntityView.printInspect(services.getEntityByName(entityName))
      case None => EntityView.printList(services.getAllEntities().toList)
    }
  }
}

@Command(scope = "entity", name = "type", description = "Lists entities of a certain type.")
class EntityTypeCommand extends ReefCommandSupport {

  @Argument(index = 0, name = "typeName", description = "Entity type name.", required = true, multiValued = false)
  var typeName: String = null

  def doCommand() = {
    EntityView.printList(services.getAllEntitiesWithType(typeName).toList)
  }

}

@Command(scope = "entity", name = "children", description = "Lists children of a parent entity.")
class EntityChildrenCommand extends ReefCommandSupport {

  @Argument(index = 0, name = "Parent Entity Name", description = "Parent entity name.", required = true, multiValued = false)
  var parentName: String = null

  @Argument(index = 1, name = "relType", description = "Relationship type.", required = false, multiValued = false)
  var relType: String = null

  @Argument(index = 2, name = "subType", description = "Sub entity type.", required = false, multiValued = false)
  var subType: String = null

  @GogoOption(name = "-d", description = "Show children at any depth.")
  var depths: Boolean = false

  def doCommand() = {

    val selector = EntityRequestBuilders.optionalChildrenSelector(parentName, Option(relType), Option(subType).toList, depths)

    val ents = services.getEntityTree(selector)
    if (depths) {
      EntityView.printTreeMultiDepth(ents)
    } else {
      EntityView.printTreeSingleDepth(ents)
    }
  }

}

@Command(scope = "point", name = "list", description = "Lists points")
class PointListCommand extends ReefCommandSupport {

  def doCommand() = EntityView.printList(services.getAllEntitiesWithType("Point").toList)

}

@Command(scope = "point", name = "commands", description = "Lists points with associated commands.")
class PointCommandsCommand extends ReefCommandSupport {

  @Argument(index = 0, name = "pointName", description = "Point name.", required = false, multiValued = false)
  var pointName: String = null

  def doCommand() = {

    import org.totalgrid.reef.proto.Model.Entity

    val query = Option(pointName) match {
      case Some(entName) => Entity.newBuilder().setName(pointName).addRelations(EntityRequestBuilders.getAllFeedBackCommands).build
      case None => EntityRequestBuilders.getAllPointsAndRelatedFeedbackCommands
    }
    services.getEntities(query).foreach(EntityView.printTreeRecursively(_))
  }

}

