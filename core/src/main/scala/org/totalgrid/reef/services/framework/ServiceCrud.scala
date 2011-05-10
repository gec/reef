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
package org.totalgrid.reef.services.framework

import org.totalgrid.reef.api.{ RequestEnv, Envelope }
import org.totalgrid.reef.api.service.HasServiceType

trait CanAuthorizeCreate extends HasServiceType {

  /**
   * @throws UnauthorizedException if authorization is rejected
   */
  protected def authorizeCreate(request: ServiceType, headers: RequestEnv): ServiceType
}

trait HasCreate extends CanAuthorizeCreate with HasAllTypes {

  /**
   * Called before create. Default implementation does nothing.
   * @param proto  Create request message
   * @return Verified/modified create request message
   */
  protected def preCreate(proto: ServiceType, headers: RequestEnv): ServiceType = proto

  /**
   * Called after preCreate validation step. Default does no authorization
   *
   * @throws UnauthorizedException if authorization is rejected
   */
  override protected def authorizeCreate(request: ServiceType, headers: RequestEnv): ServiceType = request

  protected def performCreate(model: ServiceModelType, request: ServiceType): ModelType = {
    model.createFromProto(request)
  }

  /**
   * Called after successful create. Default implementation does nothing.
   * @param proto  Created response message
   */
  protected def postCreate(created: ModelType, request: ServiceType): Unit = {}

  final def create(model: ServiceModelType, request: ServiceType, headers: RequestEnv): Tuple2[ServiceType, Envelope.Status] = {
    val validated = preCreate(request, headers)
    val authorized = authorizeCreate(validated, headers)
    val sql = performCreate(model, authorized)
    postCreate(sql, request)
    (model.convertToProto(sql), Envelope.Status.CREATED)
  }
}

trait CanAuthorizeRead extends HasServiceType {

  /**
   *
   * @throws UnauthorizedException if authorization is rejected
   */
  protected def authorizeRead(request: ServiceType, headers: RequestEnv): ServiceType
}

trait HasRead extends CanAuthorizeRead with HasAllTypes {

  /**
   * Called before read. Default implementation does nothing.
   */
  protected def preRead(proto: ServiceType): ServiceType = proto

  /**
   * Called after preRead validation step. Default does no authorization
   *
   * @throws UnauthorizedException if authorization is rejected
   */
  override protected def authorizeRead(request: ServiceType, headers: RequestEnv): ServiceType = request

  /**
   * Called after read with results. Default implementation does nothing.
   */
  protected def postRead(results: List[ServiceType]): List[ServiceType] = results

  final protected def read(model: ServiceModelType, request: ServiceType, headers: RequestEnv): List[ServiceType] = {
    val validated = preRead(request)
    val authorized = authorizeRead(validated, headers)
    val results = performRead(model, authorized)
    postRead(results)
  }

  protected def performRead(model: ServiceModelType, request: ServiceType): List[ServiceType] = {
    model.findRecords(request).map(model.convertToProto(_))
  }
}

trait CanAuthorizeUpdate extends HasServiceType {

  /**
   *
   * @throws UnauthorizedException if authorization is rejected
   */
  protected def authorizeUpdate(request: ServiceType, headers: RequestEnv): ServiceType
}

trait HasUpdate extends CanAuthorizeUpdate with HasAllTypes {

  /**
   * Called after preUpdate validation step. Default does no authorization
   *
   * @throws UnauthorizedException if authorization is rejected
   */
  override protected def authorizeUpdate(request: ServiceType, headers: RequestEnv): ServiceType = request

  /**
   * Called before update. Default implementation does nothing.
   * @param proto      Update request message
   * @param existing   Existing model entry
   * @return Verified/modified update request message
   */
  protected def preUpdate(request: ServiceType, existing: ModelType): ServiceType = request

  /**
   * Called after successful update. Default implementation does nothing.
   * @param proto Updated response message
   */
  protected def postUpdate(updated: ModelType, request: ServiceType) = {}

  /**
   * Performs an update operation first calling preUpdate, then updating, then calling postUpdate.
   */
  final protected def update(model: ServiceModelType, request: ServiceType, existing: ModelType, headers: RequestEnv): Tuple2[ServiceType, Envelope.Status] = {
    val validated = preUpdate(request, existing)
    val authorized = authorizeUpdate(validated, headers)
    val (sql, updated) = performUpdate(model, authorized, existing)
    postUpdate(sql, validated)
    val status = if (updated) Envelope.Status.UPDATED else Envelope.Status.NOT_MODIFIED
    (model.convertToProto(sql), status)
  }

  protected def performUpdate(model: ServiceModelType, request: ServiceType, existing: ModelType): Tuple2[ModelType, Boolean] = {
    model.updateFromProto(request, existing)
  }
}

trait CanAuthorizeDelete extends HasServiceType {

  /**
   *
   * @throws UnauthorizedException if authorization is rejected
   */
  protected def authorizeDelete(request: ServiceType, headers: RequestEnv): ServiceType
}

trait HasDelete extends CanAuthorizeDelete with HasAllTypes {

  /**
   * Called before delete. Default implementation does nothing.
   *  @param proto    Delete request message
   */
  protected def preDelete(request: ServiceType): ServiceType = request

  /**
   * Called after preDelete validation step. Default does no authorization
   *
   * @throws UnauthorizedException if authorization is rejected
   */
  override protected def authorizeDelete(request: ServiceType, headers: RequestEnv): ServiceType = request

  /**
   * Called after successful delete. Default implementation does nothing.
   *  @param proto    Deleted objects
   */
  protected def postDelete(results: List[ServiceType]): List[ServiceType] = results

  final protected def doDelete(model: ServiceModelType, request: ServiceType, headers: RequestEnv): List[ServiceType] = {
    // TODO: consider stripping off everything but UID if UID set on delete
    val validated = preDelete(request)
    val authorized = authorizeDelete(validated, headers)
    val existing = performDelete(model, authorized)
    postDelete(existing.map(model.convertToProto(_)))
  }

  protected def performDelete(model: ServiceModelType, request: ServiceType): List[ModelType] = {
    val existing = model.findRecords(request)
    existing.foreach(model.delete(_))
    existing
  }

}

