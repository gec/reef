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
package org.totalgrid.reef.services.framework

import org.totalgrid.reef.sapi.RequestEnv
import org.totalgrid.reef.japi.Envelope
import org.totalgrid.reef.sapi.service.HasServiceType

trait CanAuthorizeCreate extends HasServiceType {

  /**
   * @throws UnauthorizedException if authorization is rejected
   */
  protected def authorizeCreate(context: RequestContext[_], request: ServiceType, headers: RequestEnv): ServiceType
}

trait HasCreate extends CanAuthorizeCreate with HasAllTypes {

  /**
   * Called before create. Default implementation does nothing.
   * @param proto  Create request message
   * @return Verified/modified create request message
   */
  protected def preCreate(context: RequestContext[_], proto: ServiceType, headers: RequestEnv): ServiceType = proto

  /**
   * Called after preCreate validation step. Default does no authorization
   *
   * @throws UnauthorizedException if authorization is rejected
   */
  override protected def authorizeCreate(context: RequestContext[_], request: ServiceType, headers: RequestEnv): ServiceType = request

  protected def performCreate(context: RequestContext[_], model: ServiceModelType, request: ServiceType): ModelType = {
    model.createFromProto(context, request)
  }

  /**
   * Called after successful create. Default implementation does nothing.
   * @param proto  Created response message
   */
  protected def postCreate(context: RequestContext[_], created: ModelType, request: ServiceType): Unit = {}

  final def create(context: RequestContext[_], model: ServiceModelType, request: ServiceType, headers: RequestEnv): Tuple2[ServiceType, Envelope.Status] = {
    val validated = preCreate(context, request, headers)
    val authorized = authorizeCreate(context, validated, headers)
    val sql = performCreate(context, model, authorized)
    postCreate(context, sql, request)
    (model.convertToProto(sql), Envelope.Status.CREATED)
  }
}

trait CanAuthorizeRead extends HasServiceType {

  /**
   *
   * @throws UnauthorizedException if authorization is rejected
   */
  protected def authorizeRead(context: RequestContext[_], request: ServiceType, headers: RequestEnv): ServiceType
}

trait HasRead extends CanAuthorizeRead with HasAllTypes {

  /**
   * Called before read. Default implementation does nothing.
   */
  protected def preRead(context: RequestContext[_], proto: ServiceType): ServiceType = proto

  /**
   * Called after preRead validation step. Default does no authorization
   *
   * @throws UnauthorizedException if authorization is rejected
   */
  override protected def authorizeRead(context: RequestContext[_], request: ServiceType, headers: RequestEnv): ServiceType = request

  /**
   * Called after read with results. Default implementation does nothing.
   */
  protected def postRead(context: RequestContext[_], results: List[ServiceType]): List[ServiceType] = results

  final protected def read(context: RequestContext[_], model: ServiceModelType, request: ServiceType, headers: RequestEnv): List[ServiceType] = {
    val validated = preRead(context, request)
    val authorized = authorizeRead(context, validated, headers)
    val results = performRead(context, model, authorized)
    postRead(context, results)
  }

  protected def performRead(context: RequestContext[_], model: ServiceModelType, request: ServiceType): List[ServiceType] = {
    model.findRecords(context, request).map(model.convertToProto(_))
  }
}

trait CanAuthorizeUpdate extends HasServiceType {

  /**
   *
   * @throws UnauthorizedException if authorization is rejected
   */
  protected def authorizeUpdate(context: RequestContext[_], request: ServiceType, headers: RequestEnv): ServiceType
}

trait HasUpdate extends CanAuthorizeUpdate with HasAllTypes {

  /**
   * Called after preUpdate validation step. Default does no authorization
   *
   * @throws UnauthorizedException if authorization is rejected
   */
  override protected def authorizeUpdate(context: RequestContext[_], request: ServiceType, headers: RequestEnv): ServiceType = request

  /**
   * Called before update. Default implementation does nothing.
   * @param proto      Update request message
   * @param existing   Existing model entry
   * @return Verified/modified update request message
   */
  protected def preUpdate(context: RequestContext[_], request: ServiceType, existing: ModelType, headers: RequestEnv): ServiceType = request

  /**
   * Called after successful update. Default implementation does nothing.
   * @param proto Updated response message
   */
  protected def postUpdate(context: RequestContext[_], updated: ModelType, request: ServiceType) = {}

  /**
   * Performs an update operation first calling preUpdate, then updating, then calling postUpdate.
   */
  final protected def update(context: RequestContext[_], model: ServiceModelType, request: ServiceType, existing: ModelType, headers: RequestEnv): Tuple2[ServiceType, Envelope.Status] = {
    val validated = preUpdate(context, request, existing, headers)
    val authorized = authorizeUpdate(context, validated, headers)
    val (sql, updated) = performUpdate(context, model, authorized, existing)
    postUpdate(context, sql, validated)
    val status = if (updated) Envelope.Status.UPDATED else Envelope.Status.NOT_MODIFIED
    (model.convertToProto(sql), status)
  }

  protected def performUpdate(context: RequestContext[_], model: ServiceModelType, request: ServiceType, existing: ModelType): Tuple2[ModelType, Boolean] = {
    model.updateFromProto(context, request, existing)
  }
}

trait CanAuthorizeDelete extends HasServiceType {

  /**
   *
   * @throws UnauthorizedException if authorization is rejected
   */
  protected def authorizeDelete(context: RequestContext[_], request: ServiceType, headers: RequestEnv): ServiceType
}

trait HasDelete extends CanAuthorizeDelete with HasAllTypes {

  /**
   * Called before delete. Default implementation does nothing.
   *  @param proto    Delete request message
   */
  protected def preDelete(context: RequestContext[_], request: ServiceType, headers: RequestEnv): ServiceType = request

  /**
   * Called after preDelete validation step. Default does no authorization
   *
   * @throws UnauthorizedException if authorization is rejected
   */
  override protected def authorizeDelete(context: RequestContext[_], request: ServiceType, headers: RequestEnv): ServiceType = request

  /**
   * Called after successful delete. Default implementation does nothing.
   *  @param proto    Deleted objects
   */
  protected def postDelete(context: RequestContext[_], results: List[ServiceType]): List[ServiceType] = results

  final protected def doDelete(context: RequestContext[_], model: ServiceModelType, request: ServiceType, headers: RequestEnv): List[ServiceType] = {
    // TODO: consider stripping off everything but UID if UID set on delete
    val validated = preDelete(context, request, headers)
    val authorized = authorizeDelete(context, validated, headers)
    val existing = performDelete(context, model, authorized)
    postDelete(context, existing.map(model.convertToProto(_)))
  }

  protected def performDelete(context: RequestContext[_], model: ServiceModelType, request: ServiceType): List[ModelType] = {
    val existing = model.findRecords(context, request)
    existing.foreach(model.delete(context, _))
    existing
  }

}

