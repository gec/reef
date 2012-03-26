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

import org.totalgrid.reef.services.framework.SimpleServiceBehaviors.SimplePost
import org.totalgrid.reef.services.framework.{ RequestContext, ServiceEntryPoint }
import org.totalgrid.reef.client.types.TypeDescriptor
import org.totalgrid.reef.client.service.proto.Auth.{ AuthFilterResult, AuthFilter, Permission => PermissionProto }
import org.totalgrid.reef.client.exception.BadRequestException
import org.totalgrid.reef.models.{ Entity => EntityModel }

import org.totalgrid.reef.client.service.proto.{ OptionalProtos, Descriptors }
import OptionalProtos._
import scala.collection.JavaConversions._
import org.totalgrid.reef.client.service.proto.Model.{ ReefUUID, Entity }
import java.util.UUID
import org.totalgrid.reef.authz.{ Permission, Denied, Allowed }

object AuthFilterService {

  /*def permToProto(permission: Permission): PermissionProto = {
    PermissionProto.newBuilder()
      .addAllResource(permission.services)
  }*/

  class NonEmptyList[A](l: List[A]) {
    def whenNonEmpty: Option[List[A]] = l match {
      case Nil => None
      case x => Some(x)
    }
  }
  implicit def _list2nonEmpty[A](l: List[A]): NonEmptyList[A] = new NonEmptyList(l)
}

class AuthFilterService extends ServiceEntryPoint[AuthFilter] with SimplePost {
  import AuthFilterService._

  val descriptor: TypeDescriptor[AuthFilter] = Descriptors.authFilter

  val entityModel = new EntityServiceModel

  def doPost(context: RequestContext, req: AuthFilter): AuthFilter = {

    if (!req.hasRequest) {
      throw new BadRequestException("Must include request description")
    }
    if (req.getResultsCount != 0) {
      throw new BadRequestException("Request must not include results")
    }

    val desc = req.getRequest

    val action = desc.action.getOrElse("read")
    val resource = desc.resource.getOrElse("entity")

    val entities = desc.getEntityList.toList.whenNonEmpty.getOrElse {
      List(Entity.newBuilder.setUuid(ReefUUID.newBuilder.setValue("*")).build)
    }

    val entModels = entities.map(e => entityModel.findRecords(context, e))
    val uuids = entModels.map(_.map(_.id))

    val results = context.auth.filter(context, resource, action, entModels, uuids)

    val resultProtos = results.map {
      case Allowed(pay, perm) =>
        AuthFilterResult.newBuilder().setAllowed(true).setEntity(entityModel.convertToProto(pay.asInstanceOf[EntityModel])).build
      case Denied(perm) =>
        AuthFilterResult.newBuilder().setAllowed(false).build
    }

    AuthFilter.newBuilder()
      .addAllResults(resultProtos)
      .build()
  }

}
