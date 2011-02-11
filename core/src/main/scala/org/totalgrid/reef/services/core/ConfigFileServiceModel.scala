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
package org.totalgrid.reef.services.core

import org.totalgrid.reef.models.{ ConfigFile, ApplicationSchema, Entity }

import org.totalgrid.reef.proto.Model.{ ConfigFile => ConfigProto }
import org.totalgrid.reef.services.framework._
import org.totalgrid.reef.services.ProtoRoutingKeys
import org.totalgrid.reef.messaging.serviceprovider.{ ServiceEventPublishers, ServiceSubscriptionHandler }
import org.totalgrid.reef.messaging.Descriptors

import org.squeryl.PrimitiveTypeMode._
import org.totalgrid.reef.messaging.OptionalProtos._
import org.totalgrid.reef.protoapi.ProtoServiceException

import SquerylModel._

// implict asParam

class ConfigFileService(protected val modelTrans: ServiceTransactable[ConfigFileServiceModel])
    extends BasicProtoService[ConfigProto, ConfigFile, ConfigFileServiceModel] /*(modelTrans)*/ {

  override val descriptor = Descriptors.configFile
}

class ConfigFileServiceModelFactory(pub: ServiceEventPublishers)
    extends BasicModelFactory[ConfigProto, ConfigFileServiceModel](pub, classOf[ConfigProto]) {

  def model = new ConfigFileServiceModel(subHandler)
}

class ConfigFileServiceModel(protected val subHandler: ServiceSubscriptionHandler)
    extends SquerylServiceModel[ConfigProto, ConfigFile]
    with EventedServiceModel[ConfigProto, ConfigFile]
    with ConfigFileConversion {

  val table = ApplicationSchema.configFiles

  def setOwningEntity(protos: List[ConfigProto], entity: Entity): Unit = {
    protos.foreach(proto => {
      val exisiting = findRecord(proto) match {
        case Some(found) => found
        case None => createFromProto(proto)
      }
      if (exisiting.entityId != Some(entity.id)) {
        val config = createModelEntry(convertToProto(exisiting))
        config.entityId = Some(entity.id)
        update(config, exisiting)
      }
    })
  }

  override def createFromProto(req: ConfigProto): ConfigFile = {
    if (!req.hasMimeType || !req.hasFile || !req.hasName) {
      throw new ProtoServiceException("Cannot add config file without mimeType, file text and name set")
    }
    create(createModelEntry(req))
  }
}

trait ConfigFileConversion extends MessageModelConversion[ConfigProto, ConfigFile] with UniqueAndSearchQueryable[ConfigProto, ConfigFile] {

  def getRoutingKey(req: ConfigProto) = ProtoRoutingKeys.generateRoutingKey {
    req.uid :: req.name :: req.mimeType :: Nil
  }

  def searchQuery(proto: ConfigProto, sql: ConfigFile) = {
    List(
      proto.mimeType.asParam(sql.mimeType === _))
  }

  def uniqueQuery(proto: ConfigProto, sql: ConfigFile) = {
    List(
      proto.uid.asParam(sql.id === _.toInt),
      proto.name.asParam(sql.name === _),
      proto.entity.map(entity => sql.entityId in EntitySearches.searchQueryForId(entity, { _.id })))
  }

  def isModified(entry: ConfigFile, existing: ConfigFile): Boolean = {
    entry.mimeType.compareTo(existing.mimeType) != 0 || !entry.file.sameElements(existing.file) || entry.entityId != existing.entityId
  }

  def createModelEntry(proto: ConfigProto): ConfigFile = {
    new ConfigFile(
      proto.getName(),
      proto.getMimeType(),
      proto.getFile.toByteArray,
      proto.entity.uid.map { _.toLong })
  }
  override def updateModelEntry(proto: ConfigProto, existing: ConfigFile): ConfigFile = {
    new ConfigFile(
      proto.getName(),
      proto.getMimeType(),
      proto.getFile.toByteArray,
      existing.entityId)
  }

  import org.totalgrid.reef.messaging.ProtoSerializer.convertBytesToByteString
  def convertToProto(entry: ConfigFile): ConfigProto = {
    val b = ConfigProto.newBuilder
      .setUid(entry.id.toString)
      .setName(entry.name)
      .setMimeType(entry.mimeType)
      .setFile(entry.file)

    b.build
  }
}
