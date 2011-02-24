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

import org.totalgrid.reef.proto.Model.{ ConfigFile => ConfigProto, Entity => EntityProto }
import org.totalgrid.reef.services.framework._
import org.totalgrid.reef.services.ProtoRoutingKeys
import org.totalgrid.reef.messaging.serviceprovider.{ ServiceEventPublishers, ServiceSubscriptionHandler }
import org.totalgrid.reef.proto.Descriptors

import org.squeryl.PrimitiveTypeMode._
import org.totalgrid.reef.proto.OptionalProtos._
import org.totalgrid.reef.api.BadRequestException

import SquerylModel._
import scala.collection.JavaConversions._

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

  def addOwningEntity(protos: List[ConfigProto], entity: Entity): Unit = {
    protos.foreach(proto => {

      // add the entity to the config file protos so if we need to create it the ownership is set
      val p = proto.toBuilder.clearEntities.addEntities(EQ.entityToProto(entity)).build

      findRecord(p) match {
        case Some(found) => updateUsingEntities(p, found, found.owners.value)
        case None => createFromProto(p)
      }
    })
  }

  override def createFromProto(req: ConfigProto): ConfigFile = {
    if (!req.hasMimeType || !req.hasFile || !req.hasName) {
      throw new BadRequestException("Cannot add config file without mimeType, file text and name set")
    }

    // make the entity entry for the config file
    val ent = EQ.findOrCreateEntity(req.getName, "ConfigurationFile")

    val sql = create(createModelEntry(req, ent))
    updateUsingEntities(req, sql, Nil) // add entity edges
    sql
  }

  override def updateFromProto(req: ConfigProto, existing: ConfigFile): Tuple2[ConfigFile, Boolean] = {
    val sql = createModelEntry(req, existing.entity.value)
    updateUsingEntities(req, sql, existing.owners.value) // add entity edges
    update(sql, existing)
  }

  private def updateUsingEntities(req: ConfigProto, sql: ConfigFile, existingEntities: List[Entity]) {

    val updatedEntities = req.getEntitiesList.toList.map { e => EQ.findEntity(e).get }
    val newEntitites = updatedEntities.diff(existingEntities)

    // we don't delete edges this way, currently no way to delete configFile edges

    newEntitites.foreach(addUserEntity(sql.entity.value, _))
    if (!newEntitites.isEmpty) sql.changedOwners = true
  }

  private def addUserEntity(configFile: Entity, user: Entity): Unit = {
    EQ.addEdge(user, configFile, "uses")
  }
}

trait ConfigFileConversion extends MessageModelConversion[ConfigProto, ConfigFile] with UniqueAndSearchQueryable[ConfigProto, ConfigFile] {

  def getRoutingKey(req: ConfigProto) = ProtoRoutingKeys.generateRoutingKey {
    req.uid :: req.name :: req.mimeType :: Nil
  }

  def searchQuery(proto: ConfigProto, sql: ConfigFile) = {

    // when searching we go through all the entities in the proto constucting the intersection of the used config files
    val entities = EQ.findEntities { proto.getEntitiesList.toList }
    val configEntityIds = entities.map { e => EQ.getChildrenOfType(e.id, "uses", "ConfigurationFile").map { _.id } }.flatten
    // if we have specified entities only return matching config files they own (which will be zero if configEntityIds.size == 0)
    val query = if (entities.isEmpty) Nil else Some(sql.entityId in configEntityIds) :: Nil

    List(proto.mimeType.asParam(sql.mimeType === _)) ::: query
  }

  def uniqueQuery(proto: ConfigProto, sql: ConfigFile) = {
    List(
      proto.uid.asParam(sql.id === _.toInt),
      proto.name.asParam(name => sql.entityId in EntitySearches.searchQueryForId(EntityProto.newBuilder.setName(name).build, { _.id })))
  }

  def isModified(entry: ConfigFile, existing: ConfigFile): Boolean = {
    entry.mimeType.compareTo(existing.mimeType) != 0 || !entry.file.sameElements(existing.file) || entry.changedOwners
  }

  def createModelEntry(proto: ConfigProto): ConfigFile = throw new Exception("Not implemented")

  def createModelEntry(proto: ConfigProto, entity: Entity): ConfigFile = {
    new ConfigFile(
      entity,
      proto.getMimeType(),
      proto.getFile.toByteArray)
  }

  import org.totalgrid.reef.messaging.ProtoSerializer.convertBytesToByteString
  def convertToProto(entry: ConfigFile): ConfigProto = {
    val b = ConfigProto.newBuilder
      .setUid(entry.id.toString)
      .setName(entry.entity.value.name)
      .setMimeType(entry.mimeType)
      .setFile(entry.file)

    entry.owners.value.foreach(e => b.addEntities(EQ.entityToProto(e)))

    b.build
  }
}
