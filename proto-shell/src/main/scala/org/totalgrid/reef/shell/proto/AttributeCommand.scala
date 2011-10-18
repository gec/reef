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

import org.totalgrid.reef.shell.proto.presentation.AttributeView

import scala.collection.JavaConversions._

import org.totalgrid.reef.util.Conversion
import org.totalgrid.reef.api.proto.Model.ReefUUID

@Command(scope = "attr", name = "attr", description = "Prints the attributes for an entity.")
class AttributeCommand extends ReefCommandSupport {

  @Argument(index = 0, name = "entity name", description = "Entity name", required = true, multiValued = false)
  private var entityName: String = null

  def doCommand() = {
    val entity = services.getEntityByName(entityName)
    val attributes = services.getEntityAttributes(entity.getUuid)
    AttributeView.printAttributes(attributes)
  }
}

@Command(scope = "attr", name = "set", description = "Prints events.")
class AttributeSetCommand extends ReefCommandSupport {

  @Argument(index = 0, name = "entity name", description = "Entity name", required = true, multiValued = false)
  private var entityName: String = null

  @Argument(index = 1, name = "name", description = "Attribute name", required = true, multiValued = false)
  private var name: String = null

  @Argument(index = 2, name = "value", description = "Attribute value", required = true, multiValued = false)
  private var value: String = null

  def doCommand() = {

    val entity = services.getEntityByName(entityName)
    val entityUUID = entity.getUuid
    val attributes = Conversion.convertStringToType(value) match {
      case x: Int => services.setEntityAttribute(entityUUID, name, x)
      case x: Long => services.setEntityAttribute(entityUUID, name, x)
      case x: Double => services.setEntityAttribute(entityUUID, name, x)
      case x: Boolean => services.setEntityAttribute(entityUUID, name, x)
      case x: String => services.setEntityAttribute(entityUUID, name, x)
      case x: Any => throw new Exception("Couldn't convert " + x + " into long, boolean, double or string: " + x.asInstanceOf[AnyRef].getClass)
    }

    AttributeView.printAttributes(attributes)
  }
}

@Command(scope = "attr", name = "remove", description = "Prints events.")
class AttributeRemoveCommand extends ReefCommandSupport {

  @Argument(index = 0, name = "entity name", description = "Entity name", required = true, multiValued = false)
  private var entityName: String = null

  @Argument(index = 1, name = "name", description = "Attribute name", required = true, multiValued = false)
  private var name: String = null

  def doCommand() = {
    val entity = services.getEntityByName(entityName)
    val attributes = services.removeEntityAttribute(entity.getUuid, name)
    AttributeView.printAttributes(attributes)
  }
}

@Command(scope = "attr", name = "clear", description = "Prints events.")
class AttributeClearCommand extends ReefCommandSupport {

  @Argument(index = 0, name = "entity name", description = "Entity name", required = true, multiValued = false)
  private var entityName: String = null

  def doCommand() = {
    val entity = services.getEntityByName(entityName)
    val attributes = services.clearEntityAttributes(entity.getUuid)
    AttributeView.printAttributes(attributes)
  }
}