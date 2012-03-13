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
import org.totalgrid.reef.client.service.proto.OptionalProtos._
import org.totalgrid.reef.client.service.proto.Descriptors

import org.totalgrid.reef.services.framework.SquerylModel._
import org.totalgrid.reef.models.UUIDConversions._
import org.squeryl.PrimitiveTypeMode._
import scala.collection.JavaConversions._
import org.totalgrid.reef.client.exception.BadRequestException

import org.totalgrid.reef.client.service.proto.Auth.{ Permission, PermissionSet => PermissionSetProto }
import org.totalgrid.reef.models._

class PermissionSetService(protected val model: PermissionSetServiceModel)
    extends SyncModeledServiceBase[PermissionSetProto, PermissionSet, PermissionSetServiceModel]
    with DefaultSyncBehaviors {

  override val descriptor = Descriptors.permissionSet
}

class PermissionSetServiceModel
    extends SquerylServiceModel[Long, PermissionSetProto, PermissionSet]
    with EventedServiceModel[PermissionSetProto, PermissionSet]
    with PermissionSetConversions {

  val entityModel = new EntityServiceModel

  private def createSet(context: RequestContext, proto: PermissionSetProto) = {
    val entity = entityModel.findOrCreate(context, proto.getName, "PermissionSet" :: Nil, None)
    val permissionSet = new PermissionSet(entity.id, proto.toByteArray)
    permissionSet.entity.value = entity
    permissionSet
  }

  override def createFromProto(context: RequestContext, req: PermissionSetProto): PermissionSet = {

    if (!req.hasName) throw new BadRequestException("Must include name and password when creating a PermissionSet.")
    if (req.getPermissionsCount == 0) throw new BadRequestException("Must specify atleast 1 Permission when creating a PermissionSet.")

    val proto = if (req.hasDefaultExpirationTime) {
      if (req.getDefaultExpirationTime < 0) throw new BadRequestException("DefaultExpirationTime must be greater than 0 milliseconds: " + req.getDefaultExpirationTime)
      req
    } else {
      req.toBuilder.setDefaultExpirationTime(18144000000L).build // one month
    }

    val permissionSet = create(context, createSet(context, proto))

    permissionSet
  }

  override def updateFromProto(context: RequestContext, req: PermissionSetProto, existing: PermissionSet) = {

    if (req.getPermissionsCount == 0) throw new BadRequestException("Must specify atleast 1 Permission when updating a PermissionSet.")

    val previousProto = existing.proto

    if (previousProto != req) {
      update(context, existing.copy(protoData = req.toByteArray), existing)
    } else {
      (existing, false)
    }
  }

  override def preDelete(context: RequestContext, existing: PermissionSet) {
    // TODO: delete roles links
  }

  override def postDelete(context: RequestContext, existing: PermissionSet) {
    entityModel.delete(context, existing.entity.value)
  }

}

trait PermissionSetConversions
    extends UniqueAndSearchQueryable[PermissionSetProto, PermissionSet] {

  val table = ApplicationSchema.permissionSets

  def sortResults(list: List[PermissionSetProto]) = list.sortBy(_.getName)

  def relatedEntities(entries: List[PermissionSet]) = {
    Nil
  }

  def uniqueQuery(proto: PermissionSetProto, sql: PermissionSet) = {
    val eSearch = EntitySearch(proto.uuid.value, proto.name, proto.name.map(x => List("PermissionSet")))
    List(
      eSearch.map(es => sql.entityId in EntityPartsSearches.searchQueryForId(es, { _.id })))
  }

  def searchQuery(proto: PermissionSetProto, sql: PermissionSet) = Nil

  def getRoutingKey(req: PermissionSetProto) = ProtoRoutingKeys.generateRoutingKey {
    req.name :: Nil
  }

  def isModified(existing: PermissionSet, updated: PermissionSet): Boolean =
    existing.protoData != updated.protoData

  def convertToProto(entry: PermissionSet): PermissionSetProto = {
    entry.proto.toBuilder.build
  }
}
object PermissionSetConversions extends PermissionSetConversions
