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
import org.totalgrid.reef.client.sapi.rpc.impl.builders.EntityRequestBuilders
import org.totalgrid.reef.client.service.entity.EntityRelation

@Command(scope = "entity", name = "entity", description = "Prints all entities or information on a specific entity.")
class EntityCommand extends ReefCommandSupport {

  @Argument(index = 0, name = "Entity Name", description = "Entity name.", required = false, multiValued = false)
  var entityName: String = null

  def doCommand() = {
    Option(entityName) match {
      case Some(entId) => EntityView.printInspect(services.getEntityByName(entityName))
      case None => EntityView.printList(services.getEntities().toList)
    }
  }
}

@Command(scope = "entity", name = "type", description = "Lists entities of a certain type.")
class EntityTypeCommand extends ReefCommandSupport {

  @Argument(index = 0, name = "typeName", description = "Entity type name.", required = true, multiValued = false)
  var typeName: String = null

  def doCommand() = {
    EntityView.printList(services.getEntitiesWithType(typeName).toList)
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

@Command(scope = "entity", name = "tree", description = "Prints trees based on root type start")
class EntityTreeCommand extends ReefCommandSupport {

  @Argument(index = 0, name = "Entity Type", description = "Entity name.", required = false, multiValued = false)
  var rootType: String = null

  @Argument(index = 1, name = "Relationship", description = "Name of relationship type", required = true, multiValued = false)
  var relationship: String = null

  @Argument(index = 2, name = "Depth", description = "Show entities that are up to depth level children from parent.", required = true, multiValued = false)
  var depth: Int = 1

  @Argument(index = 3, name = "types", description = "List of types we want in returned list", required = true, multiValued = true)
  var types: java.util.List[String] = null

  @GogoOption(name = "-name", description = "First argument is a specific entity name")
  var rootTypeIsName: Boolean = false

  def doCommand() = {
    val entities = rootTypeIsName match {
      case false =>
        services.getEntityChildrenFromTypeRoots(rootType, relationship, depth, types).toList
      case true =>
        val root = services.getEntityByName(rootType)
        services.getEntityChildren(root.getUuid, relationship, depth, types) :: Nil

    }
    entities.foreach { EntityView.printTreeRecursively(_) }
  }
}

@Command(scope = "entity", name = "relations", description = "Prints the results of complex tree queries on the entity system. Each relationship descriptor " +
  "is made of 4 parts seperated by colons. Examples:\n" +
  "Get all child points regardless of depth:\n\tentity:relations Root owns:*:true:Point\n" +
  "Get all points and commands organized by equipment:\n\tentity:relations Root owns:*:true:Equipment owns:*:true:Point,Command\n" +
  "Get all Equipment organized by EquipmentGroup:\n\tentity:relations Root owns:1:true:EquipmentGroup owns:1:true:Equipment\n" +
  "Get all of an Endpoints commands and their owning equipment:\n\tentity:relations -name NullEndpoint source:*:true:Command owns:1:false:Equipment\n" +
  "Stars can be used as a wildcard for depth and types.")
class EntityRelationsCommand extends ReefCommandSupport {

  @Argument(index = 0, name = "Entity Type", description = "Entity name.", required = false, multiValued = false)
  var rootType: String = null

  @Argument(index = 1, name = "Relationship descriptor", description = "[relationship]:[depth]:[child]:[type1,type2,..]", required = true, multiValued = true)
  var relUris: java.util.List[String] = null

  @GogoOption(name = "-name", description = "First argument is a specific entity name")
  var rootTypeIsName: Boolean = false

  def doCommand() = {

    val relations = relUris.toList.map { s =>
      val parts = s.split(":")
      if (parts.size != 4) throw new Exception(s + " shoudl be 4 parts seperated by colons.")

      var types = parts(3).split(",").toList
      if (types == List("*")) types = Nil

      val depth = parts(1) match {
        case "*" => -1
        case s: String => Integer.parseInt(s)
      }

      new EntityRelation(parts(0), types, parts(2).toBoolean, depth)
    }

    val entities = rootTypeIsName match {
      case false =>
        services.getEntityRelationsFromTypeRoots(rootType, relations).toList
      case true =>
        val root = services.getEntityByName(rootType)
        services.getEntityRelations(root.getUuid, relations).toList
    }

    entities.foreach { EntityView.printTreeRecursively(_) }
  }
}
