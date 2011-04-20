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

import org.totalgrid.reef.models._

import org.totalgrid.reef.services.framework._
import org.totalgrid.reef.services.{ ProtoRoutingKeys }
import org.totalgrid.reef.proto.OptionalProtos._
import org.totalgrid.reef.proto.Descriptors
import org.totalgrid.reef.messaging.serviceprovider.{ ServiceEventPublishers, ServiceSubscriptionHandler }

import org.totalgrid.reef.services.framework.SquerylModel._
import org.squeryl.PrimitiveTypeMode._
import scala.collection.JavaConversions._
import org.totalgrid.reef.api.BadRequestException
import org.totalgrid.reef.proto.Auth.{ Permission, PermissionSet => PermissionSetProto }

class PermissionSetService(protected val modelTrans: ServiceTransactable[PermissionSetServiceModel])
    extends BasicSyncModeledService[PermissionSetProto, PermissionSet, PermissionSetServiceModel]
    with DefaultSyncBehaviors {

  override val descriptor = Descriptors.permissionSet
}

class PermissionSetServiceModelFactory(pub: ServiceEventPublishers)
    extends BasicModelFactory[PermissionSetProto, PermissionSetServiceModel](pub, classOf[PermissionSetProto]) {

  def model = new PermissionSetServiceModel(subHandler)
}

class PermissionSetServiceModel(protected val subHandler: ServiceSubscriptionHandler)
    extends SquerylServiceModel[PermissionSetProto, PermissionSet]
    with EventedServiceModel[PermissionSetProto, PermissionSet]
    with PermissionSetConversions {

  override def createFromProto(req: PermissionSetProto): PermissionSet = {

    if (!req.hasName) throw new BadRequestException("Must include name and password when creating a PermissionSet.")
    if (req.getPermissionsCount == 0) throw new BadRequestException("Must specify atleast 1 Permission when creating a PermissionSet.")

    val expirationTime = if (req.hasDefaultExpirationTime) {
      if (req.getDefaultExpirationTime < 0) throw new BadRequestException("DefaultExpirationTime must be greater than 0 milliseconds: " + req.getDefaultExpirationTime)
      req.getDefaultExpirationTime
    } else {
      18144000000L // one month
    }
    val permissionSet = create(new PermissionSet(req.getName, expirationTime))

    createPermissions(req, permissionSet)

    permissionSet
  }

  def createPermissions(req: PermissionSetProto, existing: PermissionSet) = {
    val permissions = req.getPermissionsList.toList.map { p => ApplicationSchema.permissions.insert(PermissionConversions.createModelEntry(p)) }
    val joins = permissions.map { p => new PermissionSetJoin(existing.id, p.id) }
    ApplicationSchema.permissionSetJoins.insert(joins)
  }

  override def updateFromProto(req: PermissionSetProto, existing: PermissionSet) = {

    if (req.getPermissionsCount == 0) throw new BadRequestException("Must specify atleast 1 Permission when updating a PermissionSet.")

    // TODO: add a find all function to UniqueAndSearchQueryable
    val requestedPermissions = req.getPermissionsList.toList.map(PermissionConversions.findRecords(_)).flatten
    val currentPermissions = existing.permissions.value.toList

    val updated = if (requestedPermissions != currentPermissions) {
      createPermissions(req, existing)
      ApplicationSchema.permissionSetJoins.deleteWhere(_.permissionId in currentPermissions.map { _.id })
      ApplicationSchema.permissions.deleteWhere(_.id in currentPermissions.map { _.id })
      true
    } else {
      false
    }

    if (req.hasDefaultExpirationTime && req.getDefaultExpirationTime != existing.defaultExpirationTime) {
      update(existing.copy(defaultExpirationTime = req.getDefaultExpirationTime), existing)
    } else {
      (existing, updated)
    }
  }

  override def preDelete(existing: PermissionSet) {
    val currentPermissions = existing.permissions.value.toList
    ApplicationSchema.permissionSetJoins.deleteWhere(_.permissionSetId === existing.id)
    ApplicationSchema.permissions.deleteWhere(_.id in currentPermissions.map { _.id })
  }

}

trait PermissionSetConversions
    extends MessageModelConversion[PermissionSetProto, PermissionSet]
    with UniqueAndSearchQueryable[PermissionSetProto, PermissionSet] {

  val table = ApplicationSchema.permissionSets

  def uniqueQuery(proto: PermissionSetProto, sql: PermissionSet) = {
    List(
      proto.uid.asParam(sql.id === _.toInt),
      proto.name.asParam(sql.name === _))
  }

  def searchQuery(proto: PermissionSetProto, sql: PermissionSet) = Nil

  def getRoutingKey(req: PermissionSetProto) = ProtoRoutingKeys.generateRoutingKey {
    req.name :: Nil
  }

  def isModified(existing: PermissionSet, updated: PermissionSet): Boolean =
    existing.defaultExpirationTime != updated.defaultExpirationTime

  def convertToProto(entry: PermissionSet): PermissionSetProto = {
    val b = PermissionSetProto.newBuilder.setUid(entry.id.toString)
    b.setName(entry.name)
    b.setDefaultExpirationTime(entry.defaultExpirationTime)
    entry.permissions.value.foreach(p => b.addPermissions(PermissionConversions.convertToProto(p)))
    b.build
  }

  def createModelEntry(proto: PermissionSetProto): PermissionSet = throw new Exception
}
object PermissionSetConversions extends PermissionSetConversions

trait PermissionConversions
    extends UniqueAndSearchQueryable[Permission, AuthPermission] {

  val table = ApplicationSchema.permissions

  def convertToProto(entry: AuthPermission): Permission = {
    val b = Permission.newBuilder.setUid(entry.id.toString)
    b.setAllow(entry.allow)
    b.setResource(entry.resource)
    b.setVerb(entry.verb)
    b.build
  }

  def uniqueQuery(proto: Permission, sql: AuthPermission) = {
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
      case "*" | "get" | "put" | "post" | "delete" => new AuthPermission(proto.getAllow, proto.getResource, normalizedVerb)
      case _ => throw new BadRequestException(proto.getVerb + " is not one of the valid verbs: get,put,post,delete,*")
    }
  }
}
object PermissionConversions extends PermissionConversions