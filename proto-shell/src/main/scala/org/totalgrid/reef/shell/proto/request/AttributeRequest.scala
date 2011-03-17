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

import org.totalgrid.reef.api.scalaclient.SyncOperations
import org.totalgrid.reef.proto.Model.{ EntityAttributes, Entity }
import org.totalgrid.reef.proto.Utils.Attribute

import scala.collection.JavaConversions._

object AttributeRequest {

  def getByEntityUid(id: String, client: SyncOperations) = {
    client.getOneOrThrow(EntityAttributes.newBuilder.setEntity(Entity.newBuilder.setUid(id)).build)
  }

  def setEntityAttribute(id: String, name: String, value: String, client: SyncOperations): EntityAttributes = {
    val prev = getByEntityUid(id, client)

    val prevSet = prev.getAttributesList.toList.filterNot(_.getName == name)

    val setAttr = buildAttribute(name, value)

    val req = EntityAttributes.newBuilder.setEntity(Entity.newBuilder.setUid(id))
    prevSet.foreach(req.addAttributes(_))
    req.addAttributes(setAttr)

    client.putOneOrThrow(req.build)
  }

  def removeEntityAttribute(id: String, name: String, client: SyncOperations) = {
    val prev = getByEntityUid(id, client)
    val prevSet = prev.getAttributesList.toList.filterNot(_.getName == name)
    val req = EntityAttributes.newBuilder.setEntity(Entity.newBuilder.setUid(id))
    prevSet.foreach(req.addAttributes(_))
    client.putOneOrThrow(req.build)
  }

  def clearEntityAttributes(id: String, client: SyncOperations) = {
    client.deleteOneOrThrow(EntityAttributes.newBuilder.setEntity(Entity.newBuilder.setUid(id)).build)
  }

  def attributeMap(attr: EntityAttributes): Map[String, Attribute] = {
    attr.getAttributesList.toList.map(a => (a.getName, a)).toMap
  }

  def buildAttribute(name: String, value: String): Attribute = {

    val attr = Attribute.newBuilder
    attr.setName(name)

    try {
      val v = value.toLong
      attr.setVtype(Attribute.Type.SINT64)
      attr.setValueSint64(v)
      return attr.build
    } catch {
      case _ => false
    }

    try {
      val v = value.toDouble
      attr.setVtype(Attribute.Type.DOUBLE)
      attr.setValueDouble(v)
      return attr.build
    } catch {
      case _ => false
    }

    try {
      val v = value.toBoolean
      attr.setVtype(Attribute.Type.BOOL)
      attr.setValueBool(v)
      return attr.build
    } catch {
      case _ => false
    }

    attr.setVtype(Attribute.Type.STRING)
    attr.setValueString(value)
    attr.build
  }
}