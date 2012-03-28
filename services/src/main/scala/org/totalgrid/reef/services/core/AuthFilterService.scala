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
import org.totalgrid.reef.models.{ Entity => EntityModel }

import org.totalgrid.reef.client.service.proto.{ OptionalProtos, Descriptors }
import OptionalProtos._
import scala.collection.JavaConversions._
import org.totalgrid.reef.client.service.proto.Model.{ ReefUUID, Entity }
import org.totalgrid.reef.services.authz.AuthzService
import org.totalgrid.reef.authz.{ AuthzFilteringService, Permission }
import org.totalgrid.reef.client.exception.{ InternalServiceException, BadRequestException }
import java.util.UUID

object AuthFilterService {

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

    val filter: AuthzFilteringService = context.get[AuthzFilteringService](AuthzService.filterService).getOrElse {
      throw new InternalServiceException("Filtering implementation not present")
    }

    if (!req.hasRequest) {
      throw new BadRequestException("Must include request description")
    }
    if (req.getResultsCount != 0) {
      throw new BadRequestException("Request must not include results")
    }

    val desc = req.getRequest
    val action = desc.action.getOrElse("read")
    val resource = desc.resource.getOrElse("entity")
    val permList = desc.permissions.map(Permission.fromProto(_, context.agent.entity.value.name))

    val entities = desc.getEntityList.toList.whenNonEmpty.getOrElse {
      List(Entity.newBuilder.setUuid(ReefUUID.newBuilder.setValue("*")).build)
    }

    val ents: List[EntityModel] = entities.flatMap(e => entityModel.findRecords(context, e))
    val uuids: List[List[UUID]] = ents.map(e => List(e.id))

    val results = permList match {
      case None => context.auth.filter(context, resource, action, ents, uuids)
      case Some(perms) => filter.filter(perms, resource, action, ents, uuids)
    }

    val resultProtos = results.map { result =>
      AuthFilterResult.newBuilder()
        .setAllowed(result.isAllowed)
        .setEntity(entityModel.convertToProto(result.result.asInstanceOf[EntityModel]))
        .setReason(result.permission.reason)
        .build
    }

    AuthFilter.newBuilder()
      .addAllResults(resultProtos)
      .build()
  }

}
