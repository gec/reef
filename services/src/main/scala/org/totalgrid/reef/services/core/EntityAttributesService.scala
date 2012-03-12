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

import org.totalgrid.reef.client.service.proto.Model.{ Entity => EntityProto, EntityAttributes => AttrProto }
import org.totalgrid.reef.client.service.proto.Utils.Attribute
import org.totalgrid.reef.client.service.proto.Descriptors

import org.totalgrid.reef.client.sapi.client.Response
import org.totalgrid.reef.client.exception.BadRequestException
import org.totalgrid.reef.client.proto.Envelope.Status

import org.totalgrid.reef.models.{ Entity, ApplicationSchema, EntityAttribute => AttrModel }
import org.totalgrid.reef.models.EntityQuery

import scala.collection.JavaConversions._

import java.util.UUID
import org.totalgrid.reef.services.framework._
import org.totalgrid.reef.services.framework.SquerylModel._

class EntityAttributesService extends ServiceEntryPoint[AttrProto] {
  import EntityAttributesService._

  override val descriptor = Descriptors.entityAttributes

  override def getAsync(source: RequestContextSource, req: AttrProto)(callback: (Response[AttrProto]) => Unit) {
    callback(source.transaction { context =>
      if (!req.hasEntity) throw new BadRequestException("Must specify Entity in request.")

      val entitiesWithAttributes = queryEntities(req.getEntity)

      context.auth.authorize(context, componentId, "read", entitiesWithAttributes.map { _._1 })

      Response(Status.OK, resultToProto(entitiesWithAttributes))
    })
  }

}

object EntityAttributesService {
  import org.squeryl.PrimitiveTypeMode._
  import org.totalgrid.reef.client.service.proto.OptionalProtos._
  import com.google.protobuf.ByteString

  def deleteAllFromEntity(entityId: UUID) = {
    ApplicationSchema.entityAttributes.deleteWhere(t => t.entityId === entityId)
  }

  def queryEntities(proto: EntityProto): List[(Entity, Option[AttrModel])] = {
    val join = if (proto.hasUuid && proto.getUuid.getValue == "*") {
      allJoin
    } else if (proto.hasUuid) {
      uidJoin(proto.getUuid.getValue)
    } else if (proto.hasName) {
      nameJoin(proto.getName)
    } else {
      throw new BadRequestException("Must search for entities by id or name.")
    }

    if (join.isEmpty)
      throw new BadRequestException("No entities match request.")

    join
  }

  def resultToProto(join: List[(Entity, Option[AttrModel])]): List[AttrProto] = {
    val pairs = join.groupBy { case (ent, attr) => ent }.toList

    pairs.map {
      case (ent, tupleList) =>
        val attrList = tupleList.map(_._2)
        protoFromEntity(ent, attrList.toList.flatten)
    }
  }

  def uidJoin(id: String): List[(Entity, Option[AttrModel])] = {
    join(ApplicationSchema.entities, ApplicationSchema.entityAttributes.leftOuter)((ent, attr) =>
      where(ent.id === UUID.fromString(id))
        select (ent, attr)
        on (Some(ent.id) === attr.map(_.entityId))).toList
  }

  def nameJoin(name: String): List[(Entity, Option[AttrModel])] = {
    join(ApplicationSchema.entities, ApplicationSchema.entityAttributes.leftOuter)((ent, attr) =>
      where(ent.name === name)
        select (ent, attr)
        on (Some(ent.id) === attr.map(_.entityId))).toList
  }

  def allJoin: List[(Entity, Option[AttrModel])] = {
    join(ApplicationSchema.entities, ApplicationSchema.entityAttributes.leftOuter)((ent, attr) =>
      select(ent, attr)
        on (Some(ent.id) === attr.map(_.entityId))).toList
  }

  def protoFromEntity(entry: Entity): AttrProto = {
    AttrProto.newBuilder
      .setEntity(EntityQuery.entityToProto(entry))
      .addAllAttributes(entry.attributes.value.map(convertToProto(_)))
      .build
  }

  def protoFromEntity(entry: Entity, attrList: List[AttrModel]): AttrProto = {
    AttrProto.newBuilder
      .setEntity(EntityQuery.entityToProto(entry))
      .addAllAttributes(attrList.map(convertToProto(_)))
      .build
  }

  def convertToProto(entry: AttrModel) = {
    val proto = Attribute.newBuilder
      .setName(entry.attrName)

    entry.boolVal.foreach { v =>
      proto.setVtype(Attribute.Type.BOOL)
      proto.setValueBool(v)
    }
    entry.stringVal.foreach { v =>
      proto.setVtype(Attribute.Type.STRING)
      proto.setValueString(v)
    }
    entry.longVal.foreach { v =>
      proto.setVtype(Attribute.Type.SINT64)
      proto.setValueSint64(v)
    }
    entry.doubleVal.foreach { v =>
      proto.setVtype(Attribute.Type.DOUBLE)
      proto.setValueDouble(v)
    }
    entry.byteVal.foreach { v =>
      proto.setVtype(Attribute.Type.BYTES)
      proto.setValueBytes(ByteString.copyFrom(v))
    }

    proto.build
  }

  def createEntryFromProto(entityId: UUID, attr: Attribute) = {
    ApplicationSchema.entityAttributes.insert(convertProtoToEntry(entityId, attr))
  }

  def convertProtoToEntry(entityId: UUID, attr: Attribute) = {
    val attrName = attr.getName
    val attrTyp = attr.getVtype

    var stringVal: Option[String] = None
    var boolVal: Option[Boolean] = None
    var longVal: Option[Long] = None
    var doubleVal: Option[Double] = None
    var byteVal: Option[Array[Byte]] = None

    def typExcept(typ: String) = new BadRequestException("Type " + typ + " specified but no " + typ + " value.")

    attrTyp match {
      case Attribute.Type.STRING =>
        val v = attr.valueString getOrElse { throw typExcept("string") }
        stringVal = Some(v)
      case Attribute.Type.BOOL =>
        val v = attr.valueBool getOrElse { throw typExcept("boolean") }
        boolVal = Some(v)
      case Attribute.Type.SINT64 =>
        val v = attr.valueSint64 getOrElse { throw typExcept("long") }
        longVal = Some(v)
      case Attribute.Type.DOUBLE =>
        val v = attr.valueDouble getOrElse { throw typExcept("double") }
        doubleVal = Some(v)
      case Attribute.Type.BYTES =>
        val v = attr.valueBytes getOrElse { throw typExcept("byte array") }
        byteVal = Some(v.toByteArray)
    }

    new AttrModel(entityId, attrName, stringVal, boolVal, longVal, doubleVal, byteVal)
  }
}

