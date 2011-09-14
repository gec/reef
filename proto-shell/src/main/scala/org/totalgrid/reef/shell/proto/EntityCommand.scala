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

@Command(scope = "entity", name = "tree", description = "Prints all entities or information on a specific entity.")
class EntityTreeCommand extends ReefCommandSupport {

  @Argument(index = 0, name = "Entity Name", description = "Entity name.", required = false, multiValued = false)
  var rootName: String = null

  @Argument(index = 1, name = "Relationship", description = "Name of relationship type", required = false, multiValued = false)
  var relationship: String = null

  @Argument(index = 2, name = "types", description = "List of types we want in returned list", required = false, multiValued = true)
  var types: java.util.List[String] = null

  def doCommand() = {
    val rootEntityName = Option(rootName) match {
      case Some(entId) => entId
      case None => getRootName.getOrElse(throw new Exception("No root entity configured, use entity:tree <name> to root tree!"))
    }
    val rel = Option(relationship).getOrElse(getRelationship.getOrElse("owns"))

    val rootEntity = services.getEntityByName(rootEntityName)

    // store the current options
    setRootName(rootEntityName)
    setRelationship(rel)

    val ents = Option(types) match {
      case Some(typs) => services.getEntityImmediateChildren(rootEntity.getUuid, rel, typs)
      case None => services.getEntityImmediateChildren(rootEntity.getUuid, rel)
    }
    EntityView.printTreeSingleDepth(rootEntity, ents.toList)
  }

  private def getRootName: Option[String] = get("entity:tree:root")
  private def setRootName(name: String) = set("entity:tree:root", name)

  private def getRelationship: Option[String] = get("entity:tree:rel")
  private def setRelationship(rel: String) = set("entity:tree:rel", rel)
}

