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

import org.totalgrid.reef.models._

import org.totalgrid.reef.services.framework._
import org.totalgrid.reef.client.sapi.OptionalProtos._
import org.totalgrid.reef.client.sapi.Descriptors

import org.totalgrid.reef.services.framework.SquerylModel._
import org.squeryl.PrimitiveTypeMode._
import scala.collection.JavaConversions._
import org.totalgrid.reef.api.japi.BadRequestException
import org.totalgrid.reef.proto.Auth.{ Permission, PermissionSet => PermissionSetProto }
import org.totalgrid.reef.services.{ ServiceDependencies, ProtoRoutingKeys }

class PermissionSetService(protected val model: PermissionSetServiceModel)
    extends SyncModeledServiceBase[PermissionSetProto, PermissionSet, PermissionSetServiceModel]
    with DefaultSyncBehaviors {

  override val descriptor = Descriptors.permissionSet
}

class PermissionSetServiceModel
    extends SquerylServiceModel[PermissionSetProto, PermissionSet]
    with EventedServiceModel[PermissionSetProto, PermissionSet]
    with PermissionSetConversions {

  override def createFromProto(context: RequestContext, req: PermissionSetProto): PermissionSet = {

    if (!req.hasName) throw new BadRequestException("Must include name and password when creating a PermissionSet.")
    if (req.getPermissionsCount == 0) throw new BadRequestException("Must specify atleast 1 Permission when creating a PermissionSet.")

    val expirationTime = if (req.hasDefaultExpirationTime) {
      if (req.getDefaultExpirationTime < 0) throw new BadRequestException("DefaultExpirationTime must be greater than 0 milliseconds: " + req.getDefaultExpirationTime)
      req.getDefaultExpirationTime
    } else {
      18144000000L // one month
    }
    val permissionSet = create(context, PermissionSet.newInstance(req.getName, expirationTime))

    createPermissions(context, req, permissionSet)

    permissionSet
  }

  def createPermissions(context: RequestContext, req: PermissionSetProto, existing: PermissionSet) = {
    val permissions = req.getPermissionsList.toList.map { p => ApplicationSchema.permissions.insert(PermissionConversions.createModelEntry(p)) }
    val joins = permissions.map { p => new PermissionSetJoin(existing.id, p.id) }
    ApplicationSchema.permissionSetJoins.insert(joins)
  }

  override def updateFromProto(context: RequestContext, req: PermissionSetProto, existing: PermissionSet) = {

    if (req.getPermissionsCount == 0) throw new BadRequestException("Must specify atleast 1 Permission when updating a PermissionSet.")

    // TODO: add a find all function to UniqueAndSearchQueryable
    val requestedPermissions = req.getPermissionsList.toList.map(PermissionConversions.findRecords(context, _)).flatten
    val currentPermissions = existing.permissions.value.toList

    val updated = if (requestedPermissions != currentPermissions) {
      createPermissions(context, req, existing)
      ApplicationSchema.permissionSetJoins.deleteWhere(_.permissionId in currentPermissions.map { _.id })
      ApplicationSchema.permissions.deleteWhere(_.id in currentPermissions.map { _.id })
      true
    } else {
      false
    }

    if (req.hasDefaultExpirationTime && req.getDefaultExpirationTime != existing.defaultExpirationTime) {
      update(context, existing.copy(defaultExpirationTime = req.getDefaultExpirationTime), existing)
    } else {
      (existing, updated)
    }
  }

  override def preDelete(context: RequestContext, existing: PermissionSet) {
    val currentPermissions = existing.permissions.value.toList
    ApplicationSchema.permissionSetJoins.deleteWhere(_.permissionSetId === existing.id)
    ApplicationSchema.permissions.deleteWhere(_.id in currentPermissions.map { _.id })
  }

}

trait PermissionSetConversions
    extends UniqueAndSearchQueryable[PermissionSetProto, PermissionSet] {

  val table = ApplicationSchema.permissionSets

  def uniqueQuery(proto: PermissionSetProto, sql: PermissionSet) = {
    val eSearch = EntitySearch(proto.uuid.uuid, proto.name, proto.name.map(x => List("PermissionSet")))
    List(
      eSearch.map(es => sql.entityId in EntityPartsSearches.searchQueryForId(es, { _.id })))
  }

  def searchQuery(proto: PermissionSetProto, sql: PermissionSet) = Nil

  def getRoutingKey(req: PermissionSetProto) = ProtoRoutingKeys.generateRoutingKey {
    req.name :: Nil
  }

  def isModified(existing: PermissionSet, updated: PermissionSet): Boolean =
    existing.defaultExpirationTime != updated.defaultExpirationTime

  def convertToProto(entry: PermissionSet): PermissionSetProto = {
    val b = PermissionSetProto.newBuilder.setUuid(makeUuid(entry))
    b.setName(entry.entityName)
    b.setDefaultExpirationTime(entry.defaultExpirationTime)
    entry.permissions.value.foreach(p => b.addPermissions(PermissionConversions.convertToProto(p)))
    b.build
  }
}
object PermissionSetConversions extends PermissionSetConversions

trait PermissionConversions
    extends UniqueAndSearchQueryable[Permission, AuthPermission] {

  val table = ApplicationSchema.permissions

  def convertToProto(entry: AuthPermission): Permission = {
    val b = Permission.newBuilder.setUid(makeUid(entry))
    b.setAllow(entry.allow)
    b.setResource(entry.resource)
    b.setVerb(entry.verb)
    b.build
  }

  def uniqueQuery(proto: Permission, sql: AuthPermission) = {
    // should be uid
    List(
      proto.uid.asParam(sql.id === _.toInt))
  }

  def searchQuery(proto: Permission, sql: AuthPermission) = {
    List(
      proto.allow.asParam(sql.allow === _),
      proto.resource.asParam(sql.resource === _),
      proto.verb.asParam(sql.verb === _))
  }

  def createModelEntry(proto: Permission): AuthPermission = {
    if (!proto.hasAllow || !proto.hasResource || !proto.hasVerb) throw new BadRequestException("Permissions must have allow, resource and verb specified.")
    val normalizedVerb = proto.getVerb.toString.toLowerCase
    normalizedVerb match {
      case "*" | "get" | "put" | "post" | "delete" | "read" | "create" | "update" => new AuthPermission(proto.getAllow, proto.getResource, normalizedVerb)
      case _ => throw new BadRequestException(proto.getVerb + " is not one of the valid operations: read,create,update,delete,*")
    }
  }
}
object PermissionConversions extends PermissionConversions