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

import org.totalgrid.reef.messaging.serviceprovider.ServiceSubscriptionHandler

import org.totalgrid.reef.proto.Model.{ EntityAttributes => AttrProto }
import org.totalgrid.reef.services.framework._
import org.totalgrid.reef.services.ProtoRoutingKeys
import org.totalgrid.reef.models.{ ApplicationSchema, EntityAttributes => AttrModel }
import org.totalgrid.reef.api.service.SyncServiceBase
import org.totalgrid.reef.api.RequestEnv
import org.totalgrid.reef.api.ServiceTypes.Response
import org.totalgrid.reef.proto.Descriptors

class EntityAttributesService extends SyncServiceBase[AttrProto] {
  import org.squeryl.PrimitiveTypeMode._

  override val descriptor = Descriptors.entityAttributes

  override def put(req: AttrProto, env: RequestEnv): Response[AttrProto] = {
    //transaction {
    // }
    null
  }
  override def delete(req: AttrProto, env: RequestEnv): Response[AttrProto] = noVerb("delete")
  override def post(req: AttrProto, env: RequestEnv): Response[AttrProto] = noVerb("post")

  override def get(req: AttrProto, env: RequestEnv): Response[AttrProto] = {
    //transaction {
    //}
    null
  }

}

/*
class EntityAttributesServiceModel(protected val subHandler: ServiceSubscriptionHandler)
    extends SquerylServiceModel[AttrProto, AttrModel]
    with EventedServiceModel[AttrProto, AttrModel]
    with EntityAttributesConversion {

  val table = ApplicationSchema.entityAttributes

}


trait EntityAttributesConversion
    extends MessageModelConversion[AttrProto, AttrModel]
    with UniqueAndSearchQueryable[AttrProto, AttrModel] {

  import org.squeryl.PrimitiveTypeMode._
  import org.totalgrid.reef.proto.OptionalProtos._
  import SquerylModel._ // Implicit squeryl list -> query conversion

  def getRoutingKey(req: AttrProto) = ProtoRoutingKeys.generateRoutingKey {
    req.entity.uid :: Nil
  }

  def uniqueQuery(proto: AttrProto, sql: AttrModel) = {
    null
  }

  def searchQuery(proto: AttrProto, sql: AttrModel) = {
    null

  }

  private def findAccessesByCommandNames(names: List[String]) = {
    null

  }

  def createModelEntry(proto: AttrProto): AttrModel = {
    null

  }

  def isModified(entry: AttrModel, existing: AttrModel): Boolean = {
    false
  }

  def convertToProto(entry: AttrModel): AttrProto = {
    null
  }
}*/ 