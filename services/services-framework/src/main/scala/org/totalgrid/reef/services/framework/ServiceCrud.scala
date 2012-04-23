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

import org.totalgrid.reef.client.proto.Envelope
import org.totalgrid.reef.client.sapi.service.HasComponentId
import org.totalgrid.reef.authz.FilteredResult
import org.totalgrid.reef.client.exception.BadRequestException

trait HasCreate extends HasAllTypes with HasComponentId {

  /**
   * Called before create. Default implementation does nothing.
   * @param proto  Create request message
   * @return Verified/modified create request message
   */
  protected def preCreate(context: RequestContext, proto: ServiceType): ServiceType = proto

  protected def performCreate(context: RequestContext, model: ServiceModelType, request: ServiceType): ModelType = {
    val entry = model.createFromProto(context, request)
    context.auth.authorize(context, componentId, "create", model.relatedEntities(List(entry)))
    entry
  }

  /**
   * Called after successful create. Default implementation does nothing.
   * @param proto  Created response message
   */
  protected def postCreate(context: RequestContext, created: ModelType, request: ServiceType): Unit = {}

  final def create(context: RequestContext, model: ServiceModelType, request: ServiceType): Tuple2[ServiceType, Envelope.Status] = {
    val validated = preCreate(context, request)
    val sql = performCreate(context, model, validated)
    postCreate(context, sql, request)
    (model.convertToProto(sql), Envelope.Status.CREATED)
  }
}

trait HasRead extends HasAllTypes with HasComponentId {

  /**
   * Called before read. Default implementation does nothing.
   */
  protected def preRead(context: RequestContext, proto: ServiceType): ServiceType = proto

  /**
   * Called after read with results. Default implementation does nothing.
   */
  protected def postRead(context: RequestContext, results: List[ServiceType]): List[ServiceType] = results

  final protected def read(context: RequestContext, model: ServiceModelType, request: ServiceType): List[ServiceType] = {
    val validated = preRead(context, request)
    val results = performRead(context, model, validated)
    postRead(context, results)
  }

  protected def performRead(context: RequestContext, model: ServiceModelType, request: ServiceType): List[ServiceType] = {
    val records: List[ModelType] = model.findRecords(context, request)

    val relatedEntities = records.map(r => model.relatedEntities(List(r)))
    val results: List[FilteredResult[ModelType]] = context.auth.filter(context, componentId, "read", records, relatedEntities)

    //val removed = results.filter(!_.isAllowed)
    //if (!removed.isEmpty) throw new BadRequestException("Missed filtering: " + removed)
    val filtered: List[ModelType] = results.filter(_.isAllowed).map(_.result)
    if (filtered == Nil) {
      context.auth.authorize(context, componentId, "read", Nil)
    }
    model.sortResults(filtered.map(model.convertToProto(_)))
  }
}

trait HasUpdate extends HasAllTypes with HasComponentId {

  /**
   * Called before update. Default implementation does nothing.
   * @param proto      Update request message
   * @param existing   Existing model entry
   * @return Verified/modified update request message
   */
  protected def preUpdate(context: RequestContext, request: ServiceType, existing: ModelType): ServiceType = request

  /**
   * Called after successful update. Default implementation does nothing.
   * @param proto Updated response message
   */
  protected def postUpdate(context: RequestContext, updated: ModelType, request: ServiceType) = {}

  /**
   * Performs an update operation first calling preUpdate, then updating, then calling postUpdate.
   */
  final protected def update(context: RequestContext, model: ServiceModelType, request: ServiceType, existing: ModelType): Tuple2[ServiceType, Envelope.Status] = {
    val validated = preUpdate(context, request, existing)
    val (sql, updated) = performUpdate(context, model, validated, existing)
    postUpdate(context, sql, validated)
    val status = if (updated) Envelope.Status.UPDATED else Envelope.Status.NOT_MODIFIED
    (model.convertToProto(sql), status)
  }

  protected def performUpdate(context: RequestContext, model: ServiceModelType, request: ServiceType, existing: ModelType): Tuple2[ModelType, Boolean] = {
    context.auth.authorize(context, componentId, "update", model.relatedEntities(List(existing)))
    model.updateFromProto(context, request, existing)
  }
}

trait HasDelete extends HasAllTypes with HasComponentId {

  /**
   * Called before delete. Default implementation does nothing.
   *  @param proto    Delete request message
   */
  protected def preDelete(context: RequestContext, request: ServiceType): ServiceType = request

  /**
   * Called after successful delete. Default implementation does nothing.
   *  @param proto    Deleted objects
   */
  protected def postDelete(context: RequestContext, results: List[ServiceType]): List[ServiceType] = results

  final protected def doDelete(context: RequestContext, model: ServiceModelType, request: ServiceType): List[ServiceType] = {
    // TODO: consider stripping off everything but UID if UID set on delete
    val validated = preDelete(context, request)
    val existing = performDelete(context, model, validated)
    postDelete(context, model.sortResults(existing.map(model.convertToProto(_))))
  }

  protected def performDelete(context: RequestContext, model: ServiceModelType, request: ServiceType): List[ModelType] = {
    val existing = model.findRecords(context, request)
    context.auth.authorize(context, componentId, "delete", model.relatedEntities(existing))
    existing.foreach(model.delete(context, _))
    existing
  }

}

