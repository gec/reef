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

import presentation.AttributeView
import request.AttributeRequest
import scala.collection.JavaConversions._
import org.totalgrid.reef.proto.Model.{ Entity, EntityAttributes }

@Command(scope = "attr", name = "attr", description = "Prints the attributes for an entity.")
class AttributeCommand extends ReefCommandSupport {

  @Argument(index = 0, name = "id", description = "Entity id", required = true, multiValued = false)
  private var id: String = null

  def doCommand() = {
    AttributeView.printAttributes(AttributeRequest.getByEntityUid(id, reefSession))
  }
}

@Command(scope = "attr", name = "set", description = "Prints events.")
class AttributeSetCommand extends ReefCommandSupport {

  @Argument(index = 0, name = "id", description = "Entity id", required = true, multiValued = false)
  private var id: String = null

  @Argument(index = 1, name = "name", description = "Attribute name", required = true, multiValued = false)
  private var name: String = null

  @Argument(index = 2, name = "value", description = "Attribute value", required = true, multiValued = false)
  private var value: String = null

  def doCommand() = {
    AttributeView.printAttributes(AttributeRequest.setEntityAttribute(id, name, value, reefSession))
  }
}

@Command(scope = "attr", name = "remove", description = "Prints events.")
class AttributeRemoveCommand extends ReefCommandSupport {

  @Argument(index = 0, name = "id", description = "Entity id", required = true, multiValued = false)
  private var id: String = null

  @Argument(index = 1, name = "name", description = "Attribute name", required = true, multiValued = false)
  private var name: String = null

  def doCommand() = {
    AttributeView.printAttributes(AttributeRequest.removeEntityAttribute(id, name, reefSession))
  }
}

@Command(scope = "attr", name = "clear", description = "Prints events.")
class AttributeClearCommand extends ReefCommandSupport {

  @Argument(index = 0, name = "id", description = "Entity id", required = true, multiValued = false)
  private var id: String = null

  def doCommand() = {
    AttributeView.printAttributes(AttributeRequest.clearEntityAttributes(id, reefSession))
  }
}