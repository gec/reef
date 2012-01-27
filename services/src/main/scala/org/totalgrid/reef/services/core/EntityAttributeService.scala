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
package org.totalgrid.reef.services.core

import org.totalgrid.reef.services.framework._
import org.totalgrid.reef.client.service.proto.Model.{ EntityAttribute => AttrProto }
import org.totalgrid.reef.client.service.proto.Utils.Attribute
import org.totalgrid.reef.client.service.proto.Descriptors
import org.totalgrid.reef.client.service.proto.OptionalProtos._
import org.squeryl.PrimitiveTypeMode._
import org.totalgrid.reef.models.{ Entity, ApplicationSchema, EntityAttribute => AttrModel }
import org.totalgrid.reef.client.exception.ReefServiceException
import org.totalgrid.reef.client.proto.Envelope

class EntityAttributeService(protected val model: EntityAttributeServiceModel)
    extends SyncModeledServiceBase[AttrProto, AttrModel, EntityAttributeServiceModel]
    with DefaultSyncBehaviors {

  override val descriptor = Descriptors.entityAttribute

  override protected def preCreate(context: RequestContext, attr: AttrProto): AttrProto = {

    if (!attr.hasEntity)
      throw new ReefServiceException("Must specify entity on creation", Envelope.Status.BAD_REQUEST)
    if (!attr.hasAttribute)
      throw new ReefServiceException("Must specify attribute on creation", Envelope.Status.BAD_REQUEST)

    def check(f: => Boolean, typ: String) {
      if (!f) throw new ReefServiceException("Must specify attribute value of type " + typ + " on creation", Envelope.Status.BAD_REQUEST)
    }

    attr.getAttribute.getVtype match {
      case Attribute.Type.STRING => check(attr.getAttribute.hasValueString, "string")
      case Attribute.Type.BOOL => check(attr.getAttribute.hasValueBool, "bool")
      case Attribute.Type.DOUBLE => check(attr.getAttribute.hasValueDouble, "double")
      case Attribute.Type.BYTES => check(attr.getAttribute.hasValueBytes, "bytes")
      case Attribute.Type.SINT64 => check(attr.getAttribute.hasValueSint64, "integer")
    }

    attr
  }
}

class EntityAttributeServiceModel
    extends SquerylServiceModel[Long, AttrProto, AttrModel]
    with EventedServiceModel[AttrProto, AttrModel]
    with EntityAttributeConversion {

  val entityModel = new EntityServiceModel
  //val edgeModel = new EntityEdgeServiceModel

  val table = ApplicationSchema.entityAttributes

  override def createFromProto(context: RequestContext, attr: AttrProto): AttrModel = {
    val entity = EntityQuery.findEntity(attr.getEntity).getOrElse {
      throw new ReefServiceException("Cannot find entity", Envelope.Status.BAD_REQUEST)
    }

    create(context, EntityAttributesService.convertProtoToEntry(entity.id, attr.getAttribute))
  }

  override def updateFromProto(context: RequestContext, attr: AttrProto, existing: AttrModel): (AttrModel, Boolean) = {
    val model = EntityAttributesService.convertProtoToEntry(existing.entityId, attr.getAttribute)
    update(context, model, existing)
  }
}

trait EntityAttributeConversion extends UniqueAndSearchQueryable[AttrProto, AttrModel] {

  def sortResults(list: List[AttrProto]) = list

  def getRoutingKey(proto: AttrProto) = ProtoRoutingKeys.generateRoutingKey {
    proto.entity.uuid.value :: proto.entity.name :: proto.attribute.name :: Nil
  }

  def searchQuery(proto: AttrProto, sql: AttrModel) = {
    proto.entity.map(ent => sql.entityId in EntitySearches.searchQueryForId(ent, { _.id })) ::
      proto.attribute.name.map(n => sql.attrName === n) ::
      Nil
  }

  def uniqueQuery(proto: AttrProto, sql: AttrModel) = {
    proto.entity.map(ent => sql.entityId in EntitySearches.uniqueQueryForId(ent, { _.id })) ::
      proto.attribute.name.map(n => sql.attrName === n) ::
      Nil
  }

  def isModified(entry: AttrModel, existing: AttrModel): Boolean = {
    entry.boolVal != existing.boolVal ||
      entry.doubleVal != existing.doubleVal ||
      entry.longVal != existing.longVal ||
      entry.stringVal != existing.stringVal ||
      entry.byteVal != existing.byteVal

  }

  /*def createModelEntry(proto: AttrProto, entity: Entity): AttrModel = {
    null
  }*/

  import org.totalgrid.reef.services.framework.ProtoSerializer.convertBytesToByteString
  def convertToProto(entry: AttrModel): AttrProto = {
    AttrProto.newBuilder
      .setEntity(EntityQuery.entityToProto(entry.entity.value))
      .setAttribute(EntityAttributesService.convertToProto(entry))
      .build
  }
}
