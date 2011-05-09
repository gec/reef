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

import org.totalgrid.reef.api.Envelope

trait HasCreate extends HasAllTypes {

  protected def create(model: ServiceModelType, req: ServiceType): Tuple2[ServiceType, Envelope.Status]

  /**
   * Called before create. Default implementation does nothing.
   * @param proto  Create request message
   * @return Verified/modified create request message
   */
  protected def preCreate(proto: ServiceType): ServiceType = proto

  /**
   * Called after successful create. Default implementation does nothing.
   * @param proto  Created response message
   */
  protected def postCreate(created: ModelType, request: ServiceType): Unit = {}

}

trait DefinesCreate extends HasCreate {

  override protected def create(model: ServiceModelType, req: ServiceType): Tuple2[ServiceType, Envelope.Status] = {
    val proto = preCreate(req)
    val sql = model.createFromProto(proto)
    postCreate(sql, req)
    (model.convertToProto(sql), Envelope.Status.CREATED)
  }
}

trait HasRead extends HasAllTypes {

  protected def read(model: ServiceModelType, req: ServiceType): List[ServiceType]

  /**
   * Called before read. Default implementation does nothing.
   */
  protected def preRead(proto: ServiceType): ServiceType = proto

  /**
   * Called after read with results. Default implementation does nothing.
   */
  protected def postRead(results: List[ServiceType]): List[ServiceType] = results
}

trait DefinesRead extends HasRead {

  override protected def read(model: ServiceModelType, request: ServiceType): List[ServiceType] = {
    val validated = preRead(request)
    val results = model.findRecords(validated).map(model.convertToProto(_))
    postRead(results)
  }
}

trait HasUpdate extends HasAllTypes {

  /**
   * Called before update. Default implementation does nothing.
   * @param proto      Update request message
   * @param existing   Existing model entry
   * @return Verified/modified update request message
   */
  protected def preUpdate(proto: ServiceType, existing: ModelType): ServiceType = proto

  /**
   * Called after successful update. Default implementation does nothing.
   * @param proto Updated response message
   */
  protected def postUpdate(updated: ModelType, request: ServiceType): Unit = {}

  /**
   * Performs an update operation first calling preUpdate, then updating, then calling postUpdate.
   */
  protected def update(model: ServiceModelType, req: ServiceType, existing: ModelType): Tuple2[ServiceType, Envelope.Status]
}

trait DefinesUpdate extends HasUpdate {

  override protected def update(model: ServiceModelType, req: ServiceType, existing: ModelType): Tuple2[ServiceType, Envelope.Status] = {
    val proto = preUpdate(req, existing)
    val (sql, updated) = model.updateFromProto(proto, existing)
    postUpdate(sql, req)
    val status = if (updated) Envelope.Status.UPDATED else Envelope.Status.NOT_MODIFIED
    (model.convertToProto(sql), status)
  }
}

trait HasDelete extends HasAllTypes {

  protected def doDelete(model: ServiceModelType, req: ServiceType): List[ServiceType]

  /**
   * Called before delete. Default implementation does nothing.
   *  @param proto    Delete request message
   */
  protected def preDelete(proto: ServiceType): Unit = {}

  /**
   * Called after successful delete. Default implementation does nothing.
   *  @param proto    Deleted objects
   */
  protected def postDelete(proto: List[ServiceType]): Unit = {}
}

trait DefinesDelete extends HasDelete {

  protected def doDelete(model: ServiceModelType, req: ServiceType): List[ServiceType] = {
    // TODO: consider stripping off everything but UID if UID set on delete
    val existing = model.findRecords(req)
    existing.foreach(model.delete(_))
    existing.map(model.convertToProto(_))
  }

}